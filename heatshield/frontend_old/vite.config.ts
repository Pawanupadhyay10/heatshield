import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          mapbox:       ['mapbox-gl'],
          react:        ['react', 'react-dom'],
          query:        ['@tanstack/react-query'],
          zustand:      ['zustand'],
        }
      }
    }
  },
  server: {
    port: 3000,
    host: true,   // needed for Codespaces port forwarding
  }
});
