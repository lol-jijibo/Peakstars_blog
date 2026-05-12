import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'
import path from 'node:path'

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
      '/auth-api': {
        target: process.env.VITE_JAVA_API_BASE_URL ? new URL(process.env.VITE_JAVA_API_BASE_URL, 'http://localhost').origin : resolveBackendTarget(),
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/auth-api/, '')
      },
      '/uploads': {
        target: process.env.VITE_JAVA_API_BASE_URL ? new URL(process.env.VITE_JAVA_API_BASE_URL, 'http://localhost').origin : resolveBackendTarget(),
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
