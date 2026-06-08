import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import mkcert from 'vite-plugin-mkcert';

export default defineConfig({
  // Locally-trusted HTTPS cert (covers localhost + LAN IP) via mkcert.
  plugins: [react(), mkcert({ hosts: ['localhost', '127.0.0.1', '192.168.178.44'] })],
  server: {
    // host:true binds to 0.0.0.0 so the dev server is reachable from other
    // devices on the LAN via http://<this-pc-ip>:5174
    host: true,
    port: 5174,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
