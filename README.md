# FreakCode Top 100

[![Frontend](https://img.shields.io/badge/Frontend-Vercel-black?style=flat-square&logo=vercel)](https://interview-preparation-platform-roan.vercel.app)
[![Backend](https://img.shields.io/badge/Backend-Render-46E3B7?style=flat-square&logo=render&logoColor=white)](https://freakcode-backend.onrender.com/actuator/health)
[![Database](https://img.shields.io/badge/Database-PostgreSQL-336791?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)

A full-stack DSA interview-prep tracker: 100 curated problems across 10 topics, per-user Solved / Revised tracking, and progress broken down by difficulty and company.

**Live:** https://interview-preparation-platform-roan.vercel.app

<img width="959" height="499" alt="Dashboard" src="https://github.com/user-attachments/assets/158493bb-b6b8-4fc7-8bf5-6c43fed35f2e" />

---

## Repository layout

```
.
├── backend/    Spring Boot 4 REST API (Java 17, PostgreSQL, JWT)   → deployed on Render (Docker)
└── frontend/   React 19 single-page app (Create React App, Axios)  → deployed on Vercel
```

## Tech stack

| Layer | Choices |
| --- | --- |
| Frontend | React 19 (hooks), Axios with auth + 401 interceptors, plain CSS with design tokens, responsive down to 360px |
| Backend | Spring Boot 4.1, Spring Security 7 (stateless JWT), Spring Data JPA / Hibernate, Bean Validation, Actuator |
| Auth | BCrypt passwords, HS256 JWT, single active session per account (a new login revokes older tokens), server-side logout, brute-force lockout |
| Errors | Every API error is RFC 9457 `application/problem+json` with a stable `code` field |
| Data | PostgreSQL. Question bank lives in `backend/src/main/resources/seed/questions.psv` and is seeded idempotently |
| Tests | Backend: JUnit 5 + MockMvc on in-memory H2. Frontend: Jest + React Testing Library |

---

## Running locally

### Backend
Prerequisites: Java 17+, a local PostgreSQL with a database named `interview_prep_db`.

```bash
cd backend
# uses src/main/resources/application-local.properties (dev JWT secret, SQL logging)
DB_PASS=<your-postgres-password> SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```
The API starts on http://localhost:8080 and seeds the 100 questions on first boot.
With the `local` profile, the user named `admin` gets admin rights (can `POST /api/questions`).

Run the tests (no Postgres needed, they use in-memory H2):
```bash
./mvnw verify
```

### Frontend
Prerequisites: Node 18+.

```bash
cd frontend
cp .env.example .env.local      # REACT_APP_API_URL=http://localhost:8080
npm install
npm start                       # http://localhost:3000
npm run test:ci                 # run tests once
```

---

## Configuration

### Backend environment variables (Render → Environment)

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `JWT_SECRET` | **yes** | – | Random string, ≥ 32 chars (`openssl rand -base64 48`). App refuses to start without it. |
| `DB_HOST`, `DB_NAME`, `DB_USER` | yes | `localhost`, `interview_prep_db`, `postgres` | Postgres connection |
| `DB_PASS` | **yes** | – | Postgres password (no fallback on purpose) |
| `DB_PORT` | no | `5432` | |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:3000,https://interview-preparation-platform-roan.vercel.app` | Comma-separated frontend origins. Add Vercel preview URLs here if you want previews to work. |
| `ADMIN_USERNAMES` | no | empty | Comma-separated usernames allowed to add questions via the API |
| `ALLOWED_EMAIL_DOMAINS` | no | empty (any) | e.g. `gmail.com` to restrict sign-ups to Gmail, as v1 did |
| `JWT_EXPIRATION_MS` | no | `86400000` (24h) | Token lifetime |
| `DDL_AUTO` | no | `update` | Hibernate schema mode |

### Frontend environment variables (Vercel → Settings → Environment Variables)

| Variable | Purpose |
| --- | --- |
| `REACT_APP_API_URL` | Backend base URL, e.g. `https://freakcode-backend.onrender.com` |

---

## Deploying (Vercel + Render from this GitHub repo)

Both services build from the `main` branch. After the move to `backend/` + `frontend/`, update:

**Vercel** → Project → Settings → Build & Deployment
- Root Directory: `frontend`
- Framework preset: Create React App (build `npm run build`, output `build`)

**Render** → `freakcode-backend` → Settings → Build & Deploy
- Root Directory: `backend`
- Dockerfile Path: `./Dockerfile` (relative to the root directory)
- Health Check Path: `/actuator/health`
- Environment: make sure `JWT_SECRET` and `DB_PASS` are set (see table above)

Existing users, questions and progress are kept. The new code uses the same tables and column names, and the seeder only inserts questions whose URL isn't in the database yet.

---

## REST API

All endpoints except register/login/logout/health need `Authorization: Bearer <token>`.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Create an account `{username, email, password}` → `201` |
| `POST` | `/api/auth/login` | `{username, password}` → `{token, username, expiresAt}`. 5 failed attempts per username+IP → `429` for 15 min |
| `POST` | `/api/auth/logout` | Revokes the current token server-side → `204` |
| `GET` | `/api/users/me` | Current user's profile |
| `GET` | `/api/questions` | All questions. Optional filters: `?company=Google&difficulty=HARD&category=Trees` |
| `POST` | `/api/questions` | Add a question (**admin only**) |
| `GET` | `/api/progress/my-status` | `{ "<questionId>": { "solved": true, "revised": false } }` |
| `PUT` | `/api/progress/{questionId}` | Set `{solved, revised}`. Both `false` removes the record |
| `POST` | `/api/progress/toggle` | Legacy form of the above (`{questionId, solved, revised}`) |
| `GET` | `/actuator/health` | Liveness for Render |

Error example:
```json
{ "type": "about:blank", "title": "Unauthorized", "status": 401,
  "detail": "You were signed out because this account logged in somewhere else.", "code": "SESSION_REVOKED" }
```

## Adding questions

Append a line to `backend/src/main/resources/seed/questions.psv`:
```
Trees | MEDIUM | Binary Tree Right Side View | https://leetcode.com/problems/binary-tree-right-side-view/ | Amazon, Meta | Return the values visible from the right side.
```
Deploy, and it appears for everyone. No SQL needed, and existing progress is untouched.
