import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'
import path from 'node:path'
import net from 'node:net'

function resolveEnvBackendOrigin() {
  if (!process.env.VITE_JAVA_API_BASE_URL) {
    return ''
  }

  return new URL(process.env.VITE_JAVA_API_BASE_URL, 'http://localhost').origin
}

function resolveBackendTarget() {
  const portFile = path.resolve(process.cwd(), 'server-java/target/runtime-port.txt')
  try {
    const runtimePort = fs.readFileSync(portFile, 'utf-8').trim()
    if (runtimePort) {
      return `http://localhost:${runtimePort}`
    }
  } catch (error) {
    // 回退到默认开发端口，等后端真正启动后再自动更新
  }

  return 'http://localhost:8080'
}

function resolveProxyTarget() {
  return resolveEnvBackendOrigin() || resolveBackendTarget()
}

function isLoopbackHost(hostname) {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1'
}

/**
 * 探测代理目标端口当前是否可连接。
 * 仅对本机回环地址做快速探测，避免 runtime-port.txt 残留旧端口时把请求转发到失效进程。
 */
function probeLocalTarget(urlString, timeoutMs = 250) {
  try {
    const targetUrl = new URL(urlString)
    if (!isLoopbackHost(targetUrl.hostname)) {
      return Promise.resolve(true)
    }
    const port = Number(targetUrl.port || (targetUrl.protocol === 'https:' ? 443 : 80))
    return new Promise((resolve) => {
      const socket = net.connect({ host: targetUrl.hostname, port })
      let settled = false

      const finish = (result) => {
        if (settled) return
        settled = true
        socket.destroy()
        resolve(result)
      }

      socket.setTimeout(timeoutMs)
      socket.once('connect', () => finish(true))
      socket.once('timeout', () => finish(false))
      socket.once('error', () => finish(false))
    })
  } catch {
    return Promise.resolve(false)
  }
}

/**
 * 为 Vite 代理选择当前可用的后端地址。
 * 优先使用显式环境变量，其次尝试 runtime-port.txt 记录的动态端口，最后回退到本地默认 8080。
 */
async function resolveAvailableProxyTarget() {
  const envTarget = resolveEnvBackendOrigin()
  if (envTarget) {
    return envTarget
  }

  const runtimeTarget = resolveBackendTarget()
  if (await probeLocalTarget(runtimeTarget)) {
    return runtimeTarget
  }

  return 'http://localhost:8080'
}

function createBackendProxy(rewrite) {
  return {
    target: resolveProxyTarget(),
    changeOrigin: true,
    async configure(proxy) {
      proxy.on('proxyReq', async (proxyReq, req) => {
        const nextTarget = await resolveAvailableProxyTarget()
        if (proxyReq.protocol && proxyReq.host) {
          req.__viteResolvedProxyTarget = nextTarget
        }
      })
    },
    async bypass(req, res, options) {
      options.target = await resolveAvailableProxyTarget()
    },
    ...(rewrite ? { rewrite } : {})
  }
}

export default defineConfig({
  plugins: [vue()],
  envPrefix: ['VITE_', 'SERVER_'],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          // 业务目的：把后台编辑器、图表、表格与基础框架拆成稳定独立包，控制首屏和功能页加载体积。
          // 业务逻辑：按第三方依赖类型手动分块，避免一个功能模块把所有重依赖聚到同一个大 chunk 中。
          if (!id.includes('node_modules')) {
            return
          }

          if (id.includes('aieditor')) {
            return 'vendor-editor'
          }
          if (id.includes('xlsx')) {
            return 'vendor-xlsx'
          }
          if (id.includes('echarts') || id.includes('@antv/g2')) {
            return 'vendor-chart'
          }
          if (id.includes('element-plus')) {
            return 'vendor-ui'
          }
          if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
            return 'vendor-vue'
          }
          return 'vendor-misc'
        }
      }
    }
  },
  server: {
    proxy: {
      '/auth-api': createBackendProxy((requestPath) => requestPath.replace(/^\/auth-api/, '')),
      '/api': createBackendProxy(),
      '/uploads': createBackendProxy()
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
