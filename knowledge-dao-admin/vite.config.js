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
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // RAG API on port 8080
      '/api/rag': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
