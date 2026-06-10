import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import mkcert from 'vite-plugin-mkcert';

// Minimal declaration so tsc does not require @types/node just for this config.
declare const process: { env: Record<string, string | undefined> };

// Extra hosts the dev HTTPS cert should cover. The launcher exports the current
// LAN IP as VOTEVOX_LAN_IP so the cert (and thus the camera on phones) keeps
// working even when the machine's IP changes.
const lanIp = process.env.VOTEVOX_LAN_IP;
const certHosts = ['localhost', '127.0.0.1', ...(lanIp ? [lanIp] : ['192.168.178.44'])];

// https://vitejs.dev/config/
export default defineConfig(({ command }) => ({
  // mkcert only runs the dev server (serve). The production build is served by
  // nginx with its own certificate, so the plugin must NOT run during `vite
  // build` (it would need network access to fetch the mkcert binary).
  plugins: [react(), ...(command === 'serve' ? [mkcert({ hosts: certHosts })] : [])],
  server: {
    // host:true binds to 0.0.0.0 so the voting UI is reachable from phones /
    // other devices on the LAN via https://<this-pc-ip>:5173
    host: true,
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
}));
