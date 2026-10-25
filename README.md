# Lovelace Poetical — Travel Management Platform

A web application for planning trips and keeping their costs under control, developed as part of the Software Engineering Project (SEP) at the University of Duisburg-Essen.

Users can manage travel entries with budgets and documents, track costs (including AI-assisted receipt scanning and importing costs directly from their mailbox), plan trips together with friends via chat, and see everything on a shared map and calendar.

## Features

- **Travel entries** — create, edit, and delete trips with destination, dates, color, and description; each trip automatically appears in the calendar.
- **Budgets and costs** — attach a budget to every trip, add cost items manually, or scan a receipt (PDF/photo) and let the AI extract the total.
- **Email cost import** — connect a Gmail mailbox (IMAP app password) and import travel costs found in booking and payment emails.
- **Calendar** — personal calendar with manual events and AI ticket scanning that turns an uploaded ticket PDF into a calendar entry.
- **Friends and chat** — friend requests, 1:1 and group chats over WebSocket, and group trip proposals that members accept or decline.
- **Trip map** — all own and friends' trips geocoded on a map, plus live friend locations for users who share theirs.
- **Documents** — upload tickets and other files per trip; stored in Cloudflare R2.
- **Dashboard, search, sightseeing tips** — aggregated overview, global search over all data, and AI sightseeing suggestions per destination.

## Tech Stack

| Layer     | Technology                                       |
|-----------|--------------------------------------------------|
| Frontend  | Angular 21, TypeScript, Tailwind CSS             |
| Backend   | Spring Boot, Java 21, Gradle                     |
| Database  | PostgreSQL 18                                    |
| Realtime  | WebSocket (STOMP)                                |
| Storage   | Cloudflare R2 (S3-compatible)                    |
| AI        | Groq API (Llama models) for scanning/suggestions |
| DevOps    | Docker, Docker Compose, GitLab CI/CD             |

## Prerequisites

- Docker Desktop (for the containerized setup)
- Git

For local development additionally:

- Java 21 (JDK)
- Node.js (LTS) and the Angular CLI (`npm install -g @angular/cli`)

## Quick Start (Docker)

```bash
git clone <repository-url>
cd team-lovelace-poetical
docker compose up --build
```

| Service  | URL                                     |
|----------|-----------------------------------------|
| Frontend | http://localhost:4200                   |
| Backend  | http://localhost:8080                   |
| API docs | http://localhost:8080/swagger-ui.html   |
| Database | localhost:5432 (`sep` / see compose.yaml) |

## Using the Application

1. Open http://localhost:4200 and **register** an account. A verification link is sent by email — confirm it, then **log in**.
2. Create your first **travel entry** (title, destination, dates, budget).
3. From the trip detail page, manage the trip's **budget**, **calendar events**, and **documents**.
4. Under **Friends**, search for other users and send requests; accepted friends can chat with you and see your trips on the map.
5. In a **group chat**, propose a trip — everyone who accepts gets it added to their own calendar.
6. Under **Email Costs**, connect your mailbox to import travel expenses automatically.

A detailed user guide is maintained in the GitLab Wiki.

## Local Development

The backend expects the database at host `db` (see `backend/src/main/resources/application.properties`). The simplest local setup is to keep the database and backend in Docker and only run the frontend locally — or override `spring.datasource.url` when running the backend outside Docker.

### Frontend

```bash
cd frontend
npm install
npm start        # ng serve → http://localhost:4200
npm run lint     # ESLint
npm test         # Karma/Jasmine unit tests
```

### Backend

```bash
cd backend
./gradlew bootRun    # requires a reachable PostgreSQL instance
./gradlew test       # unit and integration tests (H2 in-memory)
./gradlew check      # tests + Checkstyle (config/checkstyle/checkstyle.xml)
```

## Configuration

Backend settings live in `backend/src/main/resources/application.properties`. The relevant keys:

| Key                          | Purpose                              |
|------------------------------|--------------------------------------|
| `spring.datasource.*`        | PostgreSQL connection                |
| `spring.mail.*`              | SMTP account for verification emails |
| `jwt.secret`, `jwt.expiration` | Token signing and lifetime         |
| `groq.api.key`, `groq.api.url` | AI provider for scanning features  |
| `unsplash.access-key`        | Destination images on the dashboard  |
| `cloudflare.r2.*`            | Document storage bucket              |

Secrets must not be committed; provide them via environment-specific configuration.

## Project Structure

```
team-lovelace-poetical/
├── frontend/          # Angular app (src/app/<feature>/)
├── backend/           # Spring Boot app (src/main/java/com/sep/sep_backend/<feature>/)
│   ├── config/        # Checkstyle rules
│   └── src/test/      # JUnit tests
├── compose.yaml       # Docker Compose: frontend, backend, PostgreSQL
└── README.md
```

Both applications are organized by feature (e.g. `travelentry`, `budget`, `chat`, `calendar`), with each backend feature package containing its controller, service, repository, entity, and DTOs.

## Contributing (Team Workflow)

- One GitLab issue per unit of work; every change is traceable Issue → Branch → Merge Request → `main`.
- Branch from `main`, named `<issue-number>-<short-description>` (e.g. `42-password-reset`).
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org): `feat(auth): add password reset` … `Closes #42`.
- Every merge request needs a green CI pipeline and at least one approval from another team member; direct pushes to `main` are blocked.
- A feature is done when it meets the team's Definition of Done (see GitLab Wiki), including tests and documentation.

## Team

Team Lovelace Poetical — Software Engineering Project (SEP), Summer Term 2026, University of Duisburg-Essen.

This project was created for educational purposes as part of the SEP course.
