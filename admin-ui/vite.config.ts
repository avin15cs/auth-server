import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:9000',
      '/oauth2': 'http://localhost:9000',
      '/login': 'http://localhost:9000',
      '/logout': 'http://localhost:9000',
      '/actuator': 'http://localhost:9000',
      '/.well-known': 'http://localhost:9000',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
  },
})
