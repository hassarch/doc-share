# docshare

Distributed Document Sharing & Synchronization Platform — a portfolio-grade
demonstration of distributed systems engineering (horizontal scalability,
fault tolerance, replication, event-driven architecture, distributed
caching, observability), built incrementally from a working monolith into a
distributed architecture.

## Structure

```
docshare/
  backend/         Spring Boot 3 / Java 21 API — modular monolith (Phase 0),
                    organized so future services can be extracted cleanly.
  frontend/        Next.js / TypeScript / Tailwind client.
  infra/           Docker Compose, Kubernetes manifests (later), observability config.
  docs/adr/        Architecture Decision Records — the source of truth for
                    why this project is built the way it is. Start with
                    0001-foundations.md, then 0002-monorepo-migration.md.
```

This is a **folder-based monorepo** — no build-graph tooling (Nx/Turborepo/
Bazel). Each folder is independently buildable with its own native tool
(Gradle in `backend/`, npm/pnpm in `frontend/`, Docker Compose in `infra/`).

## Status

🚧 Phase 0 (Working Monolith) in progress. See `docs/adr/` for all
foundational decisions, and the project's phased delivery plan for what's
coming next.

## Local development

Requires Docker (for Postgres/Redis/MinIO/Kafka) — see `infra/` for the
Docker Compose stack. Setup instructions will be filled in as Phase 0
progresses.
