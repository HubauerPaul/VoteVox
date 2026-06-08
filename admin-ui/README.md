# VoteVox Admin UI

Administration panel for the VoteVox school voting system at HTL Wels.

## Stack

- React 18 + TypeScript
- Vite 5
- React Router v6
- Axios
- Recharts
- Plain CSS

## Development

```bash
pnpm install
pnpm dev
```

The dev server runs on port **5174** and proxies `/api/*` requests to the backend on `http://localhost:8080`.

## Build

```bash
pnpm build
```

## Default Credentials

- Email: `admin@votevox.at`
- Password: `Admin1234!`

Change the password after the first login.
