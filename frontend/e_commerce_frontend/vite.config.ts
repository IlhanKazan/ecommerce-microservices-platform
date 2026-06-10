import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { visualizer } from 'rollup-plugin-visualizer'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    // Bundle haritası — build sonrası dist/stats.html üretir (chunk boyutları, gzip/brotli)
    visualizer({
      filename: 'dist/stats.html',
      gzipSize: true,
      brotliSize: true,
    }),
  ],
  optimizeDeps: {
    exclude: ['@chakra-ui/icons'],
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          // MUI core ve ikonlar ayrı chunk'larda — initial mui-vendor küçülür, ikonlar ayrı yüklenir
          'mui-vendor': ['@mui/material', '@emotion/react', '@emotion/styled'],
          'mui-icons-vendor': ['@mui/icons-material'],
          'tanstack-vendor': ['@tanstack/react-query'],
          'auth-vendor': ['react-oidc-context', 'oidc-client-ts']
        }
      }
    }
  }
})
