# VoteVox — Voting UI

Student-facing frontend for VoteVox, the secure QR-code based voting system used at HTL Wels. To run locally, install dependencies with `pnpm install`, then start the dev server with `pnpm dev`. The app runs at `http://localhost:5173` and proxies `/api/*` calls to the backend at `http://localhost:8080`, so make sure the backend is running before voting. Build for production with `pnpm build` and preview the built bundle with `pnpm preview`.
