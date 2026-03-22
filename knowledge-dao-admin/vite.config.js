import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // Admin API on port 8081
      '/api/admin': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // RAG API (RagController) on port 8080
      '/api/rag': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // Dashboard API on port 8080
      '/api/dashboard': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // Knowledge API on port 8080
      '/api/knowledge': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // Monitor API on port 8080
      '/api/monitor': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // Config API on port 8080
      '/api/config': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
