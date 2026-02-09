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
})
