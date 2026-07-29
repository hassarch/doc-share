# backend

Spring Boot 3 / Java 21 API for the Distributed Document Sharing &
Synchronization Platform. Part of the `docshare` monorepo.

## What this is

Starts life as a **modular monolith** (Phase 0) and is incrementally split
into independently deployable services (Phases 1-5), per the project's
phased delivery plan.

Even in monolith form, this codebase is organized as if it were already
services: each top-level package under `src/main/java/.../` corresponds to a
future microservice, and cross-module calls happen only through service-layer
interfaces - never through direct repository access across module boundaries.

## Status

**Phase 5: Authentication** — JWT-based registration, login, token refresh, and logout now
implemented. Endpoints under `/api/v1/auth/*` are public; all other endpoints require a
valid `Authorization: Bearer <token>` header (FR-1.7).

Previous phases:
- Phase 4: Database schema and User entity with JPA auditing
- Phase 3: Security baseline (CORS, CSRF posture, password encoder)
- Phase 0: Modular monolith foundation

See `../docs/adr/0001-foundations.md` for the architectural decisions this project is built on.

## Local development

Requires Docker (for Postgres/Redis/MinIO/Kafka) - see `../infra/` for the
`docker-compose.yml` that provisions these dependencies.

### Authentication endpoints

The authentication system implements FR-1.1 through FR-1.7:

- **POST /api/v1/auth/register** - Register a new user (email, password, name)
  - Returns JWT access token (15 min TTL) and refresh token (7 day TTL)
  - Password must be at least 8 characters
  - Email is normalized to lowercase

- **POST /api/v1/auth/login** - Authenticate with email and password
  - Returns JWT access token and refresh token
  - Password is compared against bcrypt hash

- **POST /api/v1/auth/refresh** - Get new access token using refresh token
  - Reuses the same refresh token (no rotation)
  - Refresh token must still exist in Redis and not be expired

- **POST /api/v1/auth/logout** - Revoke a refresh token
  - Deletes refresh token from Redis
  - Access token remains valid until expiration (up to 15 min) — this is deliberate

- **POST /api/v1/auth/password-reset** - Initiate password reset
  - Always returns 200 to prevent email enumeration
  - Reset token is logged server-side (will be emailed in future Notification phase)

All other endpoints require `Authorization: Bearer <access_token>` header.
