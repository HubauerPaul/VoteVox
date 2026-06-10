# VoteVox

Secure, QR‑code based digital voting system for school and department representative elections.

> Version 1.x — HTL Wels, Project Team AB00x
> See `/docs` for the full project handbook, requirements specification, and UML diagrams.

---

## Team

| Role                 | Name                   | Contact                           |
| -------------------- | ---------------------- | --------------------------------- |
| Project Owner & PM   | Hubauer Paul Matthias  | paul.hubauer@htl-wels.at          |
| Project Team Member  | Aigner Sandro          | sandro.aigner@htl-wels.at         |
| Project Team Member  | Anzengruber Laurenz    | laurenz.anzengruber@htl-wels.at   |
| Customer             | Loidl Susanne          | susanne.loidl@htl-wels.at         |

---

## Tech Stack

| Layer      | Technology                                               |
| ---------- | -------------------------------------------------------- |
| Backend    | Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Database   | PostgreSQL 16 (managed via Flyway migrations)            |
| Voting UI  | React 18 + Vite + TypeScript (+ a QR scanner library)    |
| Admin UI   | React 18 + Vite + TypeScript                             |
| Build      | Maven (backend), pnpm (frontends)                        |
| Local Dev  | Docker Compose (PostgreSQL + pgAdmin)                    |
| CI         | GitHub Actions                                           |

---

## Repository Layout

```
votevox/
├── backend/                # Spring Boot API (Auth, Voting, Election Mgmt, QR, Reporting, Audit)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/at/htlwels/votevox/
│       │   │   ├── auth/          # AuthenticationService
│       │   │   ├── voting/        # VotingService, Vote entity
│       │   │   ├── election/      # ElectionManagementService
│       │   │   ├── qrcode/        # QRCodeGenerationService
│       │   │   ├── reporting/     # ReportingService, exports
│       │   │   ├── audit/         # Security & Audit Service
│       │   │   ├── backup/        # BackupService
│       │   │   └── common/        # shared configs, errors, DTOs
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/  # Flyway SQL migrations (V1__, V2__, ...)
│       └── test/
├── voting-ui/              # Student Voting Frontend (browser + camera)
├── admin-ui/               # Admin Panel Frontend
├── docs/                   # Handbook, requirements, UML diagrams (PDFs)
├── .github/workflows/      # CI pipelines
├── docker-compose.yml      # Local PostgreSQL + pgAdmin
└── .env.example
```

The three modules (`backend`, `voting-ui`, `admin-ui`) each have their own build; the monorepo keeps them versioned together.

---

## Quick Start — one click (production, all in Docker)

For a packaged run that a non-developer can start, **everything runs in Docker**
(database, backend, and both UIs served by nginx over HTTPS):

1. Install **Docker Desktop** (and start it once).
2. Double-click **`VoteVox starten.bat`**.

The launcher automatically starts Docker if needed, detects this PC's LAN IP,
generates a trusted HTTPS certificate (mkcert) for it, builds & starts all
containers, and opens the admin UI. QR codes point at `https://<this-pc-ip>:5173/vote`,
so phones on the **same WiFi** can vote. Stop everything with **`VoteVox stoppen.bat`**.

End-user guides: [`docs/Manual-Admin.md`](docs/Manual-Admin.md) and
[`docs/Manual-Voter.md`](docs/Manual-Voter.md).

To package the project for someone else, double-click **`Projekt-packen.bat`** —
it builds a clean `VoteVox-<date>.zip` (no `node_modules`, build output, certs or
`.git`). The recipient only needs Docker Desktop + `VoteVox starten.bat`.

> First run downloads images and builds the app (a few minutes); later starts are fast.

---

## Getting Started (development)

> Windows shortcut for the dev stack (hot-reloading dev servers): run
> **`.\start-all.ps1`** (stop with **`.\stop-all.ps1`**). The steps below are the
> manual equivalent.

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+  (or use the included Maven Wrapper once generated)
- Node.js 20+ and pnpm 9+
- Docker + Docker Compose

### 1. Clone & configure

```bash
git clone <repo-url> votevox
cd votevox
cp .env.example .env
```

### 2. Start the database

```bash
docker compose up -d
```

Postgres is available on `localhost:5432`, pgAdmin on `http://localhost:5050`.

### 3. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

Flyway runs migrations automatically on startup. API is served on `http://localhost:8080`.

### 4. Start the voting UI

```bash
cd voting-ui
pnpm install
pnpm dev
```

### 5. Start the admin UI

```bash
cd admin-ui
pnpm install
pnpm dev
```

---

## Branching Strategy

Following the project rule *"All features must be tested before merging into the main branch."*:

- `main` — always deployable, protected, no direct commits
- `develop` — integration branch
- `feature/<wbs-code>-<short-desc>` — e.g. `feature/3.2-token-validation`
- `fix/<short-desc>` — bug fixes
- `docs/<short-desc>` — documentation-only changes

**Pull Request requirements**

1. CI green (build + tests)
2. At least one reviewer approval
3. Linked WBS code in the PR title or description
4. Branch name uses the conventions above

### Commit Messages

Conventional Commits, e.g.

```
feat(auth): add token validation endpoint (WP 3.2)
fix(qr): handle camera permission denial
docs(readme): add branching strategy
```

---

## Security Principles (non‑negotiable)

These rules follow from the project handbook §1.16.6 and the class diagram:

1. **Ballot secrecy.** The `votes` table must never reference a student. Linking a student to their ballot is impossible by schema design.
2. **Tokens stored as hashes only.** `tokens.token_value_hash` holds a SHA‑256 hash. The plaintext token exists only inside the printed QR code.
3. **Token is one‑time.** Marking `is_used` and inserting a `vote` happen in the same DB transaction.
4. **Tokens never appear in logs.** `AuditLogEntry.details` must not contain token values or hashes.
5. **Admin passwords use Argon2id** (Spring Security default) — never MD5/SHA‑1/plain.
6. **HTTPS only** in production.

---

## Documentation

All planning & design documents are stored in `/docs`:

- Project Handbook (`VoteVox_Handbook.pdf`)
- Requirements Specification (`VoteVox_Requirement_Specification.pdf`)
- Class Diagram (`VoteVox_Class_Diagram.pdf` + `VoteVox_Class_Diagram_Relationships.pdf`)
- Component Diagram (`VoteVox_Component_Diagram.pdf`)
- Use Case Diagram (`VoteVox_Use_Case_Diagram.pdf`)
- Sequence Diagrams (`VoteVox_Sequence_Diagram_Student.pdf`, `VoteVox_Sequence_Diagram_Management.pdf`)
- Work Breakdown Structure (`VoteVox_Work_Breakdown_Structure.pdf`)
- Gantt Bar Chart (`VoteVox_BarChart_1.pdf`, `VoteVox_BarChart_2.pdf`)

---

## License

Internal project — HTL Wels. Not for public distribution.
