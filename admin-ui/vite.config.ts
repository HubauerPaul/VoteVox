import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import mkcert from 'vite-plugin-mkcert';

// Minimal declaration so tsc does not require @types/node just for this config.
declare const process: { env: Record<string, string | undefined> };

// Extra hosts the dev HTTPS cert should cover (see voting-ui/vite.config.ts).
const lanIp = process.env.VOTEVOX_LAN_IP;
const certHosts = ['localhost', '127.0.0.1', ...(lanIp ? [lanIp] : ['192.168.178.44'])];

export default defineConfig(({ command }) => ({
  // mkcert only for the dev server; the production build is served by nginx.
  plugins: [react(), ...(command === 'serve' ? [mkcert({ hosts: certHosts })] : [])],
  server: {
    // host:true binds to 0.0.0.0 so the dev server is reachable from other
    // devices on the LAN via http://<this-pc-ip>:5174
    host: true,
    port: 5174,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
}));
