import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// envDir '..' makes Vite read the shared floodwatch/.env (only VITE_* variables reach the browser)
export default defineConfig({
  plugins: [react()],
  envDir: '..',
  server: { port: 5173 },
})
