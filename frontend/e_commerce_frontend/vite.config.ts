import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { visualizer } from 'rollup-plugin-visualizer'
import { keycloakify } from 'keycloakify/vite-plugin'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    keycloakify({
      themeName: 'ilhan-e-ticaret',
      // Account teması Multi-Page (v1 fork) — self-contained, extra extension gerektirmez.
      accountThemeImplementation: 'Multi-Page',
      // Sadece KC 26.2+ jar'ını üret (deploy ettiğimiz tek sürüm) → Docker build hızlanır, isim sabit.
      keycloakVersionTargets: {
        hasAccountTheme: true,
        '21-and-below': false,
        '23': false,
        '24': false,
        '25': false,
        '26.0-to-26.1': false,
        '26.2-and-above': 'ilhan-e-ticaret.jar',
      },
    }),
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
