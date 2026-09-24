import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El proxy evita CORS en desarrollo y, mas importante, hace que el codigo
// sea identico en desarrollo y en produccion: el front siempre pide rutas
// relativas que empiezan con /api.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/facturacion': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
