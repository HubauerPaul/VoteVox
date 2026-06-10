# syntax=docker/dockerfile:1

# --- Stage 1: build the static Vite bundle -----------------------------------
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# --- Stage 2: serve via nginx over HTTPS -------------------------------------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
# Runs before nginx starts: bakes the QR_BASE_URL env into a static config file
# the admin SPA reads at runtime (so the QR codes carry the right LAN IP).
COPY docker-entrypoint.d/40-votevox-config.sh /docker-entrypoint.d/40-votevox-config.sh
RUN chmod +x /docker-entrypoint.d/40-votevox-config.sh
EXPOSE 443
