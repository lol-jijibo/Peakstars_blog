const fs = require('node:fs')
const path = require('node:path')
const net = require('node:net')
const { spawn } = require('node:child_process')

const projectRoot = path.resolve(__dirname, '..')
const backendRoot = path.join(projectRoot, 'server-java')
const backendTargetDir = path.join(backendRoot, 'target')
const runtimePortFile = path.join(backendTargetDir, 'runtime-port.txt')
const backendJar = path.join(backendTargetDir, 'interview-auth-service-1.0.0.jar')
const bundledMavenHome = path.join(backendTargetDir, 'mvn_home', 'apache-maven-3.9.15')
const bundledMaven = path.join(bundledMavenHome, 'bin', 'mvn.cmd')
const bundledMavenBootJar = path.join(bundledMavenHome, 'boot', 'plexus-classworlds-2.9.0.jar')
const bundledMavenLibDir = path.join(bundledMavenHome, 'lib')
const defaultLocalUploadDir = path.join(backendRoot, 'uploads')
const backendWatchRoots = [
  path.join(backendRoot, 'pom.xml'),
  path.join(backendRoot, 'src')
]

function log(message) {
  process.stdout.write(`[dev-with-backend] ${message}\n`)
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function readRuntimePort() {
  try {
    const raw = fs.readFileSync(runtimePortFile, 'utf8').trim()
    const port = Number(raw)
    return Number.isInteger(port) && port > 0 ? port : null
  } catch {
    return null
  }
}

function canConnect(port, host = '127.0.0.1', timeoutMs = 400) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ port, host })
    let settled = false

    const finish = (result) => {
      if (settled) {
        return
      }
      settled = true
      socket.destroy()
      resolve(result)
    }

    socket.setTimeout(timeoutMs)
    socket.once('connect', () => finish(true))
    socket.once('timeout', () => finish(false))
    socket.once('error', () => finish(false))
  })
}

async function removeStaleRuntimePortFile() {
  const runtimePort = readRuntimePort()
  if (!runtimePort) {
    return
  }

  const reachable = await canConnect(runtimePort)
  if (reachable) {
    log(`detected running backend on port ${runtimePort}`)
    return
  }

  try {
    fs.unlinkSync(runtimePortFile)
    log(`removed stale runtime port file for port ${runtimePort}`)
  } catch (error) {
    log(`failed to remove stale runtime port file: ${error.message}`)
  }
}

function spawnCommand(command, args, options = {}) {
  const useShell = process.platform === 'win32' && /\.(cmd|bat)$/i.test(command)
  return spawn(command, args, {
    stdio: 'inherit',
    shell: useShell,
    ...options
  })
}

function hasBundledMavenCommand() {
  return fs.existsSync(bundledMaven)
}

function hasBundledMavenLauncher() {
  return fs.existsSync(bundledMavenBootJar) && fs.existsSync(bundledMavenLibDir)
}

function spawnBundledMaven(args, options = {}) {
  if (hasBundledMavenCommand()) {
    return spawnCommand(bundledMaven, args, options)
  }

  if (!hasBundledMavenLauncher()) {
    throw new Error('backend jar is missing and bundled Maven is unavailable')
  }

  const classPath = [bundledMavenBootJar, path.join(bundledMavenLibDir, '*')].join(path.delimiter)
  return spawnCommand('java', [
    `-Dmaven.multiModuleProjectDirectory=${backendRoot}`,
    '-cp',
    classPath,
    'org.apache.maven.cli.MavenCli',
    ...args
  ], options)
}

function resolveMinioEndpoint() {
  return process.env.MINIO_ENDPOINT || 'http://127.0.0.1:9000'
}

async function detectMinioAvailability() {
  try {
    const endpoint = new URL(resolveMinioEndpoint())
    const port = Number(endpoint.port || (endpoint.protocol === 'https:' ? 443 : 80))
    return await canConnect(port, endpoint.hostname, 400)
  } catch {
    return false
  }
}

function getLatestModifiedTime(targetPath) {
  if (!fs.existsSync(targetPath)) {
    return 0
  }

  const stat = fs.statSync(targetPath)
  if (stat.isFile()) {
    return stat.mtimeMs
  }

  let latestTime = stat.mtimeMs
  for (const entry of fs.readdirSync(targetPath, { withFileTypes: true })) {
    latestTime = Math.max(
      latestTime,
      getLatestModifiedTime(path.join(targetPath, entry.name))
    )
  }
  return latestTime
}

function waitForExit(child, name) {
  return new Promise((resolve, reject) => {
    child.once('error', (error) => {
      reject(new Error(`${name} failed to start: ${error.message}`))
    })

    child.once('exit', (code) => {
      if (code === 0) {
        resolve()
        return
      }
      reject(new Error(`${name} exited with code ${code}`))
    })
  })
}

async function ensureBackendJar() {
  const jarModifiedTime = fs.existsSync(backendJar) ? fs.statSync(backendJar).mtimeMs : 0
  const sourceModifiedTime = Math.max(...backendWatchRoots.map((targetPath) => getLatestModifiedTime(targetPath)))
  if (jarModifiedTime >= sourceModifiedTime) {
    return
  }

  log('backend sources changed, packaging latest backend jar')
  const packageProcess = spawnBundledMaven(['-q', '-DskipTests', 'package'], {
    cwd: backendRoot
  })
  await waitForExit(packageProcess, 'backend package')
}

async function startBackendIfNeeded() {
  const existingPort = readRuntimePort()
  if (existingPort && await canConnect(existingPort)) {
    return { port: existingPort, started: false }
  }

  await ensureBackendJar()
  const minioAvailable = await detectMinioAvailability()
  const backendEnv = {
    ...process.env,
    LOCAL_UPLOAD_DIR: process.env.LOCAL_UPLOAD_DIR || defaultLocalUploadDir
  }
  if (minioAvailable) {
    backendEnv.MINIO_ENABLED = 'true'
    backendEnv.OSS_ENABLED = 'false'
    log(`detected MinIO at ${resolveMinioEndpoint()}, enabling MinIO storage for backend`)
  }
  log('starting Java backend')
  const backendProcess = spawnCommand('java', [
    '-jar',
    backendJar,
    '--spring.profiles.active=dev'
  ], {
    cwd: backendRoot,
    env: backendEnv
  })

  const shutdownBackend = () => {
    if (!backendProcess.killed) {
      backendProcess.kill()
    }
  }

  process.once('SIGINT', shutdownBackend)
  process.once('SIGTERM', shutdownBackend)
  process.once('exit', shutdownBackend)

  const startTime = Date.now()
  while (Date.now() - startTime < 60000) {
    const runtimePort = readRuntimePort()
    if (runtimePort && await canConnect(runtimePort)) {
      log(`backend is ready on port ${runtimePort}`)
      return { port: runtimePort, started: true }
    }

    if (backendProcess.exitCode !== null) {
      throw new Error(`backend exited with code ${backendProcess.exitCode}`)
    }

    await wait(800)
  }

  throw new Error('backend did not become reachable within 60 seconds')
}

async function startVite() {
  log('starting Vite dev server')
  const viteCommand = process.platform === 'win32'
    ? path.join(projectRoot, 'node_modules', '.bin', 'vite.cmd')
    : path.join(projectRoot, 'node_modules', '.bin', 'vite')

  const viteProcess = spawnCommand(viteCommand, [], {
    cwd: projectRoot
  })

  const exitCode = await new Promise((resolve, reject) => {
    viteProcess.once('error', (error) => reject(new Error(`vite failed to start: ${error.message}`)))
    viteProcess.once('exit', (code) => resolve(code ?? 0))
  })

  process.exit(exitCode)
}

async function main() {
  await removeStaleRuntimePortFile()
  await startBackendIfNeeded()
  await startVite()
}

main().catch((error) => {
  console.error(`[dev-with-backend] ${error.message}`)
  process.exit(1)
})
