# Dhruv Backend

Spring Boot 3.2 / Java 21 API for the Dhruv readiness platform.

## Running locally

```bash
mvn spring-boot:run          # dev profile: in-memory H2, demo data seeded
mvn verify                   # full test suite
```

The `dev` profile seeds two students and one parent. Credentials are printed at startup
and are only ever valid against the in-memory database.

The API listens on `:8080`. Run the frontend with an empty `VITE_BACKEND_URL` so Vite
proxies to it and the browser stays on one origin.

## Security model

Authentication is a **server-side session** referenced by an HttpOnly cookie
(`DHRUVSESSION`). The client never holds a credential it can read, and it cannot
influence who the server thinks it is.

- **Passwords** are BCrypt-hashed. `V3__hash_passwords_and_harden.sql` nulls any legacy
  plaintext value, which makes those accounts unauthenticatable rather than leaving a
  readable secret in the table. There is no password-reset flow yet, so an affected
  account must be re-registered — see *Known gaps*.
- **Authorization** is derived from the session principal only. No endpoint accepts a
  phone number or user id that identifies the caller.
- **CSRF**: the SPA fetches a token from `GET /api/v1/auth/csrf` and echoes it in
  `X-XSRF-TOKEN`. Login is deliberately *not* exempt, so login CSRF is also blocked.
- **Rate limiting** applies to `POST /api/v1/auth/*` only.

### The parent/student link

A parent can only see a student who nominated that parent's mobile number
(`PUT /api/v1/student/parent-contact`). The parent then confirms with
`POST /api/v1/parent/link-student`.

This is a deliberate behaviour change. Previously the link endpoint wrote a
client-supplied phone number onto any student found by id, so any caller could attach an
arbitrary child to themselves and read that child's reports.

## Configuration

All settings come from the environment; see [`.env.example`](.env.example) for the full
list. Under `SPRING_PROFILES_ACTIVE=prod`, `StartupConfigValidator` **refuses to start**
unless:

| Requirement | Why |
|---|---|
| `SPRING_DATASOURCE_URL` is a real PostgreSQL URL | the H2 default is in-memory and loses every account on restart |
| `ddl-auto` is `validate` or `none` | Flyway owns the schema; `update` lets Hibernate silently alter production tables |
| `CORS_ALLOWED_ORIGINS` is set, no wildcard | credentialed CORS forbids `*` |
| `WEBSOCKET_ALLOWED_ORIGINS` is set, no wildcard | the browser same-origin policy does not cover WebSockets |
| `SESSION_COOKIE_SECURE=true` | the session cookie must never traverse plaintext HTTP |

### Cookie SameSite and where you deploy

Prefer hosting the SPA and API on **subdomains of one registrable domain**
(`app.example.com` + `api.example.com`). That is same-site, so the cookie can stay
`SameSite=Lax` and SameSite remains a second CSRF defence.

Splitting across unrelated domains (`*.vercel.app` + `*.onrender.com`) forces
`SESSION_COOKIE_SAMESITE=None`, which requires HTTPS and leaves the strict CORS
allowlist plus the CSRF token as the only defences.

## Database migrations

Flyway, in `src/main/resources/db/migration`. Never edit an applied migration; add a new
one. Hibernate is set to `validate`, so an entity that drifts from the schema fails at
startup instead of silently altering a table.

## Endpoints

| Method | Path | Access |
|---|---|---|
| `POST` | `/api/v1/auth/student` | public (rate-limited) |
| `POST` | `/api/v1/auth/parent` | public (rate-limited) |
| `GET` | `/api/v1/auth/csrf` | public |
| `GET` | `/api/v1/auth/session` | public; 401 when signed out |
| `POST` | `/api/v1/auth/logout` | public |
| `GET` | `/api/v1/readiness/**` | `ROLE_STUDENT` |
| `GET` | `/api/v1/plan/daily` | `ROLE_STUDENT` |
| `PUT` | `/api/v1/student/parent-contact` | `ROLE_STUDENT` |
| `POST` | `/api/v1/student/send-report` | `ROLE_STUDENT` |
| `GET` | `/api/v1/parent/students` | `ROLE_PARENT` |
| `POST` | `/api/v1/parent/link-student` | `ROLE_PARENT` |
| `GET` | `/api/v1/costudy/room-state` | authenticated |
| `GET` | `/actuator/health`, `/actuator/info` | public |

Errors share one envelope: `{"code": "...", "message": "...", "fields": {...}}`. Branch on
`code`, never on `message`.

## Known gaps

These are understood and unbuilt, not oversights:

1. **Readiness figures are a fixed reference model.** `ERI`, the syllabus heatmap, the
   backlog ledger, and the daily plan return the same constants for every student. Real
   values need an activity-ingestion and decay model that does not exist yet. Endpoints
   are already scoped and authorised per student so the contract will not change.
2. **No password reset.** An account whose password was nulled by `V3`, or a user who
   forgets theirs, cannot recover it. Needs an SMS OTP flow.
3. **Sessions and rate limits are per instance.** Both are in-memory, so running more
   than one replica requires sticky sessions, or Spring Session + Redis for sessions and
   a shared limiter store. The effective auth rate limit is `N × configured` across
   `N` replicas.
4. **Account enumeration on registration.** Sign-in returns one message for both unknown
   account and wrong password, but registration must report "already exists", which
   reveals whether a number is registered. Closing it means moving registration behind
   OTP verification.
5. **No structured audit log.** Sign-ins and link attempts are logged as plain text.
