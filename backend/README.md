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

Phase 0 in progress - see `../docs/adr/0001-foundations.md` for the
architectural decisions this project is built on.

## Local development

Requires Docker (for Postgres/Redis/MinIO/Kafka) - see `../infra/` for the
`docker-compose.yml` that provisions these dependencies.
