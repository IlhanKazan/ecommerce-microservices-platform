import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
    resolve: {
        alias: {
            "keycloakify/lib": "keycloakify/dist/lib",
            "keycloakify": "keycloakify/dist/cjs"
        },
    },
  optimizeDeps: {
    include: [
        "keycloakify"
    ],
    exclude: ['@chakra-ui/icons'],
  },
    build: {
        rollupOptions: {
            output: {
                manualChunks: {
                    'react-vendor': ['react', 'react-dom', 'react-router-dom'],
                    'mui-vendor': ['@mui/material', '@mui/icons-material', '@emotion/react', '@emotion/styled'],
                    'tanstack-vendor': ['@tanstack/react-query'],
                    'auth-vendor': ['keycloakify', 'react-oidc-context', 'oidc-client-ts']
                }
            }
        }
    }
})
