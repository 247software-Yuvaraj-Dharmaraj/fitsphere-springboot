# FitSphere — Spring Boot backend

A **Java / Spring Boot** reimplementation of the [FitSphere](https://github.com/247software-Yuvaraj-Dharmaraj/fitsphere) backend (originally Node + Express + Mongoose). Same REST API contract, so the existing React frontend works unchanged.

> Companion to [`incidentdesk-springboot`](https://github.com/247software-Yuvaraj-Dharmaraj/incidentdesk-springboot): that one is Spring + **JPA/PostgreSQL** (relational); this one is Spring + **MongoDB** (document) — the right datastore for each app.

## Tech stack

- **Java 21**, **Spring Boot 3.3**
- **Spring Web** (REST), **Spring Security** — JWT **access + refresh-token rotation** (Bearer), role-based access control
- **Spring Data MongoDB**
- **netty-socketio** for realtime (live occupancy + slot changes)
- **JUnit**, Maven wrapper, Dockerfile

## Feature parity with the Node backend

- Auth: signup / signin / **refresh (token rotation, revocable store)** / logout / me / profile / password / preferences
- RBAC: MEMBER / TRAINER / ADMIN
- **Attendance**: check-in/out, live **occupancy**, consecutive-day **streaks**, weekly/monthly totals, trend, best-time, calendar month
- **Workouts**: log, recent, stats (by type, totals, weekly)
- **Slots**: booking with capacity guard, **FIFO waitlist auto-promotion**, overlap detection, staff CRUD
- **Feedback**: trainer → member notes (role-gated)
- **Analytics**: gym overview (peak hours, daily trend, occupancy) + server-side **member directory** (search / sort / paginate / engagement status)
- **Realtime**: `occupancy` + `slots:changed` over Socket.IO

## API

```
POST /api/auth/signup | /signin | /refresh | /logout    GET /api/auth/me
PATCH /api/auth/me · POST /api/auth/me/password · PATCH /api/auth/me/preferences
POST /api/attendance/check-in | /check-out
GET  /api/attendance/summary | /month | /occupancy | /trend | /best-time
POST /api/workouts · GET /api/workouts · GET /api/workouts/stats
GET  /api/slots · /my-bookings · POST /{id}/book · DELETE /{id}/book
POST /{id}/waitlist · DELETE /{id}/waitlist
POST /api/slots · /bulk-delete · PATCH /{id} · DELETE /{id}   (staff)
GET  /api/feedback/me · POST /api/feedback · GET /members · /member/{id}   (staff)
GET  /api/analytics/overview · /members   (staff)
GET  /api/health
```

## Run locally

1. Copy `.env.example` → `.env`; set a `MONGODB_URI` (Atlas or local) + JWT secrets.
2. `./mvnw spring-boot:run` — API at `http://localhost:4001`, Socket.IO at `:9093`. Demo data is seeded on first run.
3. Demo logins (password `password123`): `member@fitsphere.app` · `trainer@fitsphere.app` · `admin@fitsphere.app`

## Build & Docker

```bash
./mvnw clean package
docker build -t fitsphere-springboot .
docker run -p 4001:4001 -p 9093:9093 --env-file .env fitsphere-springboot
```

## Using with the React frontend

Set the client's `VITE_API_URL` to this backend's base URL (it uses Bearer tokens — no cross-site cookie concerns). The realtime Socket.IO server runs on `SOCKET_PORT` (default `9093`).
