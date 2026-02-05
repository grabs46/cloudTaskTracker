# Cloud Task Tracker
Using Spring Boot, Postgres, Google OAuth, and AWS deployment to demonstrate cloud/backend engineering skills via a full-stack task management app.


## Architecture

     Client (React + Vite)
       |
       v
     Nginx (static files + reverse proxy)
       |
       v
     Spring Boot API (Docker container on EC2)
       |
       v
     PostgreSQL (AWS RDS)

     Nginx serves the React SPA and proxies /api requests to the
     Spring Boot backend. 
     The backend handles authentication, business logic, and database access.
     RDS provides managed Postgres in production.


## Tech Stack

**Backend:** Java 17, Spring Boot 4, Spring Security, Spring Data JPA, Flyway, jjwt
**Frontend:** React 19, Vite, Google Identity Services
**Database:** PostgreSQL 16
**Infrastructure:** AWS EC2, AWS RDS, Docker, Nginx, Let's Encrypt SSL
**CI:** GitHub Actions, Testcontainers
**Tools:** Maven, Bash scripts, springdoc-openapi (Swagger)

## Features

- Google OAuth login - frontend gets ID token (JWT) via GIS, backend verifies and issues its own JWT in an httpOnly cookie
- Full CRUD for tasks with per-user ownership enforcement
- Server-side search, filtering by status, and sorting
- API documentation via Swagger UI at /docs
- Health monitoring via Spring Actuator
- Rate limiting on the auth endpoint (in-memory limiter)
- Structured request logging with trace IDs
- HTTPS with auto-renewing Let's Encrypt certificates

## API Documentation

     Swagger UI is available at `tasktracker.nicolasgrabner.com/docs` (locally: http://localhost:8080/docs).

**Auth endpoints:**
     - POST /api/auth/google — exchange Google ID token for JWT cookie
     - POST /api/auth/logout — clear auth cookie
     - GET /api/me — current user info

**Task endpoints:**
     - POST /api/tasks — create a task
     - GET /api/tasks — list tasks (supports ?query=, ?status=, ?page=, ?size=, ?sortBy=, ?sortDir=)
     - GET /api/tasks/{id} — get a single task
     - PUT /api/tasks/{id} — update a task
     - DELETE /api/tasks/{id} — delete a task

**Health/Docs:**
     - GET /actuator/health
     - GET /api-docs (OpenAPI JSON)

## Testing

     Integration tests use **Testcontainers** to spin up a real Postgres instance in Docker.

     **What's tested:**
     - TaskRepositoryTest — ownership isolation, search/filter queries, pagination, delete behavior
     - TaskControllerTest — full HTTP lifecycle (create, read, update, delete), validation errors,
       404 handling, auth enforcement, ownership isolation, search filtering

     **Run tests locally:**
     ```
     cd backend
     mvn verify
     ```
     Docker must be running (Testcontainers needs it).

     **CI:** Tests run automatically on push/PR

## Scripts Reference

     | Script | Description |
     |--------|-------------|
     | `scripts/deploy_ec2.sh <pem> [user@host]` | Build images, push to Docker Hub, deploy to EC2 |
     | `scripts/setup_ssl.sh <email>` | Provision Let's Encrypt certs on EC2, configure auto-renewal |
     | `scripts/smoke.sh [base_url]` | Run smoke tests against deployed app (health, swagger, auth) |

## Security Notes

- **Auth:** Google OAuth login + backend-issued JWT (15 min) in httpOnly cookie.
- **Rate limiting:** Fixed-window limiter on POST /api/auth/google to prevent brute force.
- **Secrets management:** All secrets (JWT_SECRET, DB credentials, GOOGLE_CLIENT_ID) are
       env vars, never committed. `.env.prod.example` shows the shape; real `.env.prod` is gitignored.
- **Network:** EC2 security group allows inbound 80/443 only. SSH restricted to personal IP.
       RDS security group accepts connections only from the EC2 security group.
- **HTTPS:** TLS 1.2+ via Let's Encrypt with auto-renewal. HTTP redirects to HTTPS.
- **CSRF:** Disabled for v1 (stateless JWT auth); noted for v2.

## Local Development Setup

1. **Prerequisites:** Java 17, Maven, Docker, Node 20
2. **Clone the repo:** `git clone <repo-url> && cd taskTracker`
3. **Start Postgres:** `docker compose up -d` (uses docker-compose.yml with local credentials)
4. **Configure secrets:** Copy `.env.prod.example` to see the shape; locally set JWT_SECRET
        and GOOGLE_CLIENT_ID as env vars or in application-local.yml
5. **Run the backend:** `cd backend && mvn spring-boot:run`
        - Flyway runs migrations automatically; Swagger UI is at http://localhost:8080/docs
6. **Run the frontend:** `cd frontend && npm install && npm run dev`
        - Set VITE_API_BASE=http://localhost:8080 and VITE_GOOGLE_CLIENT_ID in a .env file

## Deployment

1. **Infrastructure:** EC2 instance (Docker installed) + RDS Postgres. Security groups lock
        SSH to personal IP; RDS only accepts connections from the EC2 security group.
2. **Build & deploy:** `./scripts/deploy_ec2.sh <path-to-pem>` — builds Docker images for
        the backend and nginx (which includes the frontend build), pushes to Docker Hub, SSHs
        into EC2 to pull and restart containers.
3. **SSL setup (first time):** SSH into EC2 and run `./scripts/setup_ssl.sh your@email.com`
        to provision Let's Encrypt certs. Auto-renewal is configured via cron.
4. **Smoke test:** `./scripts/smoke.sh` verifies health, Swagger, auth-gated endpoints,
        and HTTPS redirect.
