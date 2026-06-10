#!/bin/sh
# Write the runtime config the admin SPA loads via <script src="/votevox-config.js">.
# QR_BASE_URL is injected by docker-compose from the launcher's detected LAN IP,
# e.g. https://192.168.178.44:5173/vote. If unset, the SPA falls back to deriving
# the URL from the browser's current hostname.
set -e
CONFIG_FILE=/usr/share/nginx/html/votevox-config.js
echo "window.__VOTEVOX_QR_BASE_URL__ = \"${QR_BASE_URL:-}\";" > "$CONFIG_FILE"
echo "[votevox] wrote $CONFIG_FILE (QR_BASE_URL=${QR_BASE_URL:-<empty>})"
