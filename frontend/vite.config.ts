import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    strictPort: true,
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('@mantine')) return 'mantine'
          if (id.includes('@telegram-apps')) return 'telegram'
          if (id.includes('@tanstack')) return 'query'
          return 'vendor'
        },
      },
    },
  },
})
