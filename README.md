# docshare

Distributed Document Sharing & Synchronization Platform — a portfolio-grade demonstration of distributed systems engineering (horizontal scalability, fault tolerance, replication, event-driven architecture, distributed caching, observability), built incrementally from a working monolith into a distributed architecture.

## Quick Start

### Prerequisites

- **Java 21** - for the backend
- **Gradle 8.10+** - included via wrapper (or use your IDE)
- **Node.js 18+** and **npm** - for the frontend
- **Docker** and **Docker Compose** - for local infrastructure

### 1. Start the Infrastructure

Start Postgres, Redis, MinIO, and Kafka:

```bash
cd infra
docker compose up -d
```

Verify all services are healthy:

```bash
docker compose ps
```

You should see all services with status "healthy". Services available at:
- **Postgres**: `localhost:5432` (db: `docshare`, user/pass: `docshare`/`docshare`)
- **Redis**: `localhost:6379`
- **MinIO S3 API**: `localhost:9000`
- **MinIO Console**: `http://localhost:9001` (user/pass: `docshare`/`docshare123`)
- **Kafka**: `localhost:9092`

### 2. Run the Backend

Generate Gradle wrapper files (first time only):

```bash
cd backend
gradle wrapper
```

Run the Spring Boot application:

```bash
./gradlew bootRun
```

The backend API will start at `http://localhost:8080`.

Health check: `http://localhost:8080/actuator/health`

### 3. Run the Frontend

Install dependencies (first time only):

```bash
cd frontend
npm install
```

Start the development server:

```bash
npm run dev
```

The frontend will start at `http://localhost:3000`.

## Repository Structure

```
docshare/
├── backend/           Spring Boot monolith (Java 21, Gradle)
│   ├── src/main/java/com/docshare/backend/
│   │   ├── auth/      Authentication & JWT
│   │   ├── users/     User profiles & quota
│   │   ├── documents/ Document metadata & versioning
│   │   ├── storage/   MinIO abstraction & chunking
│   │   ├── sharing/   Permissions & share links
│   │   ├── common/    Shared DTOs & utilities
│   │   └── config/    Spring configuration
│   └── build.gradle.kts
├── frontend/          Next.js app (TypeScript, Tailwind)
│   ├── src/app/       App Router pages/layouts
│   └── package.json
├── infra/             Infrastructure as code
│   └── docker-compose.yml
└── docs/              Architecture decision records
    └── adr/
```

This is a **folder-based monorepo** — no build-graph tooling (Nx/Turborepo/Bazel). Each folder is independently buildable with its own native tool (Gradle in `backend/`, npm in `frontend/`, Docker Compose in `infra/`).

## Development Workflow

### Backend Development

```bash
cd backend

# Run with live reload
./gradlew bootRun

# Run tests
./gradlew test

# Format code (Spotless)
./gradlew spotlessApply

# Build JAR
./gradlew build
```

### Frontend Development

```bash
cd frontend

# Development server with hot reload
npm run dev

# Build for production
npm run build

# Lint
npm run lint
```

### Infrastructure Management

```bash
cd infra

# Start all services
docker compose up -d

# View logs
docker compose logs -f [service-name]

# Stop services (keep data)
docker compose down

# Stop and remove all data
docker compose down -v

# Restart a specific service
docker compose restart [service-name]
```

## Testing

### Backend

The project includes a smoke test that validates Spring context wiring:

```bash
cd backend
./gradlew test
```

Integration tests using Testcontainers will be added in subsequent phases.

### Frontend

Standard Next.js testing setup (to be expanded in frontend development phase):

```bash
cd frontend
npm test  # (when tests are added)
```

## Configuration

### Backend Configuration

Configuration is in `backend/src/main/resources/`:
- `application.yml` - Base configuration with environment variable bindings
- `application-local.yml` - Local dev overrides (auto-active profile)
- `application-test.yml` - Test profile

All sensitive values use environment variables with safe local defaults. Example:

```yaml
docshare:
  jwt:
    secret: ${JWT_SECRET:change-me-in-every-environment-this-is-a-local-dev-only-default}
```

### Frontend Configuration

Frontend expects the backend API at `http://localhost:8080` by default. Configuration can be customized via environment variables (to be documented in frontend phase).

## Troubleshooting

### Port Already in Use

If you see port conflict errors:

```bash
# Check what's using the port
lsof -i :8080   # Backend
lsof -i :3000   # Frontend
lsof -i :5432   # Postgres
```

Either stop the conflicting process or change the port in configuration.

### Database Connection Failed

Ensure Docker containers are running and healthy:

```bash
cd infra
docker compose ps
docker compose logs postgres
```

### Gradle Wrapper Not Found

Generate the wrapper files:

```bash
cd backend
gradle wrapper
```

This requires Gradle to be installed on your system. Alternatively, use an IDE with Gradle support (IntelliJ IDEA, VS Code with Java extensions).

### Java 21 Not Found

If Gradle can't find Java 21, make sure it's properly symlinked:

```bash
# Install Java 21 via Homebrew (macOS)
brew install openjdk@21

# Symlink it so the system can find it
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk

# Verify it's available
/usr/libexec/java_home -V
```

For other operating systems, install Java 21 from [Adoptium](https://adoptium.net/) or your package manager.

### Backend Won't Start - Flyway/JPA Errors

The smoke test excludes database autoconfiguration to remain fast and dependency-free. Once real migrations are added (Database phase), the backend will require Postgres to be running. Ensure `docker compose up -d` completed successfully.

## Documentation

- **`docs/adr/`** - Architecture Decision Records (start here!)
  - `0001-foundations.md` - Core architectural decisions
  - `0002-monorepo-migration.md` - Monorepo structure rationale
- `backend/README.md` - Backend-specific documentation
- `frontend/README.md` - Frontend-specific documentation
- `infra/README.md` - Infrastructure documentation

## Status

**Phase 5: Authentication** ✅

JWT-based authentication is now fully implemented with:
- ✅ User registration, login, logout endpoints
- ✅ JWT access tokens (15 min TTL, stateless validation)
- ✅ Refresh tokens (7 day TTL, stored in Redis)
- ✅ Password reset token issuance
- ✅ SecurityConfig wired with JWT authentication filter
- ✅ All endpoints except `/api/v1/auth/*` now require valid JWT

See `backend/TEST_AUTH.md` for manual testing guide.

Previous phases:
- **Phase 4: Database Schema** ✅ - User entity, Flyway migrations, JPA auditing
- **Phase 3: Security Baseline** ✅ - CORS, CSRF, password encoder
- **Phase 2: Project Initialization** ✅ - Backend/frontend scaffold, Docker infrastructure
- **Phase 0: Foundations** ✅ - Modular monolith architecture

Next phases will add document storage, folder management, sharing, and synchronization.

## License

[To be determined]
