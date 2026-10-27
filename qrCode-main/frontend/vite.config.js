//import { defineConfig } from 'vite'
//import react from '@vitejs/plugin-react'
//import tailwindcss from '@tailwindcss/vite'
//
//export default defineConfig({
//  plugins: [react(), tailwindcss()],
//  server: { port: 5173 },
//})



import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

/*
 * DEVELOPMENT ONLY: the Vite dev server forwards /api, /ws and /uploads
 * to the backend. The browser talks to ONE address, so no CORS problems.
 */
const BACKEND = 'http://127.0.0.1:8080'

const dropOrigin = (proxy) => {
  proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
  proxy.on('proxyReqWs', (proxyReq) => proxyReq.removeHeader('origin'))
}

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    allowedHosts: true,
    proxy: {
      '/api': { target: BACKEND, changeOrigin: true, configure: dropOrigin },
      '/uploads': { target: BACKEND, changeOrigin: true },
      '/ws': { target: BACKEND, ws: true, changeOrigin: true, configure: dropOrigin },
    },
  },
})