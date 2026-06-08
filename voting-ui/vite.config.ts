import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import mkcert from 'vite-plugin-mkcert';

// https://vitejs.dev/config/
export default defineConfig({
  // mkcert generates a locally-trusted HTTPS certificate (covering localhost and
  // the LAN IP) so the camera/getUserMedia works on phones and there is no
  // "connection not secure" warning once the CA is trusted on the device.
  plugins: [react(), mkcert({ hosts: ['localhost', '127.0.0.1', '192.168.178.44'] })],
  server: {
    // host:true binds to 0.0.0.0 so the voting UI is reachable from phones /
    // other devices on the LAN via https://<this-pc-ip>:5173
    host: true,
    // Allow the app to be served through a Cloudflare quick tunnel
    // (https://<random>.trycloudflare.com) so voters can be on any network.
    allowedHosts: ['.trycloudflare.com'],
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
