import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  base: '/rag/',
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    allowedHosts: ['blog.chqiuu.com'],
    port: 56673,
    proxy: {
      // Admin API on port 8081
      '/rag/api/admin': {
        target: 'http://localhost:58088',
        changeOrigin: true,
        rewrite: (path) => path.replace('/rag', '')
      },
      // RAG API (RagController) on port 8080
      '/rag/api/rag': {
        target: 'http://localhost:58088',
        changeOrigin: true,
        rewrite: (path) => path.replace('/rag', '')
      },
      // Dashboard API on port 8080
      '/rag/api/dashboard': {
        target: 'http://localhost:58088',
        changeOrigin: true,
        rewrite: (path) => path.replace('/rag', '')
      },
      // Knowledge API on port 8080
      '/rag/api/knowledge': {
        target: 'http://localhost:58088',
        changeOrigin: true,
        rewrite: (path) => path.replace('/rag', '')
      },
      // Monitor API on port 8080
      '/rag/api/monitor': {
        target: 'http://localhost:58088',
        changeOrigin: true,
        rewrite: (path) => path.replace('/rag', '')
      },
      // Config API on port 8080
      '/rag/api/config': {
        target: 'http://localhost:58088',
        changeOrigin: true,
        rewrite: (path) => path.replace('/rag', '')
      }
    }
  }
})
