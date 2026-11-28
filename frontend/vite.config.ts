import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

// envDir '..' makes Vite read the shared floodwatch/.env (only VITE_* variables reach the browser)
export default defineConfig({
  plugins: [react(), tailwindcss()],
  envDir: '..',
  server: { port: 5173 },
  // Pre-bundle the map library up front so the dev server never serves a stale copy of it when the Shelters page loads lazily.
  optimizeDeps: { include: ['maplibre-gl'] },
})
