# ADR-0001: Project Foundations

**Status:** Accepted (2026-07-28) — **decision #1 superseded by ADR-0002** (2026-07-28)
**Owner:** [Your Name]

## Context

Before writing any application code for the Distributed Document Sharing &
Synchronization Platform, we need to fix the foundational engineering
decisions that every later phase depends on: repo strategy, module
boundaries, local dev environment, Git workflow, API conventions, error
handling, logging, testing, and CI. Changing these mid-project is expensive;
deciding them once, in writing, up front is cheap.

## Decisions

1. ~~**Repo strategy: Polyrepo.**~~ **SUPERSEDED by ADR-0002 — now a monorepo.**
   Original text (kept for history): Three repositories —
   `docshare-backend`, `docshare-frontend`, `docshare-infra` — rather than a
   monorepo, on the reasoning that it mirrors independent ownership/
   deployment. See `docs/adr/0002-monorepo-migration.md` for the updated
   decision and rationale.

2. **Backend module layout (Phase 0): package-per-future-service.**
   Inside the Phase 0 Spring Boot monolith, each top-level package
   (`auth/`, `users/`, `documents/`, `storage/`, `sharing/`) corresponds to a
   future microservice. Hard rule: **no package accesses another package's
   repository layer directly** — cross-module calls go through a
   service-layer interface only. This is what makes Phase 1's Storage
   Service extraction a mechanical "move this package behind HTTP" operation
   instead of a rewrite.

3. **Local dev environment: Docker Compose from day one.**
   Postgres, Redis, and MinIO are run via Docker Compose starting in Phase 0
   — even though only Postgres strictly matters that early — to establish
   environment parity before Kafka and multi-node storage arrive in Phase 2.

4. **Messaging: Kafka (real Kafka, not Redpanda/RabbitMQ).**
   Chosen over lighter alternatives because this project's explicit purpose
   is demonstrating real distributed-systems engineering; the operational
   weight of real Kafka in local Docker Compose is accepted as a worthwhile
   trade-off for portfolio authenticity.

5. **Branching model: Trunk-based, short-lived feature branches.**
   `feature/<name>`, `fix/<name>`, `chore/<name>`, `docs/<name>`, merged into
   `main` via PR. Used even solo, as a review/history checkpoint.

6. **Commit convention: Conventional Commits.**
   `feat(scope): ...`, `fix(scope): ...`, `refactor(scope): ...`,
   `docs(scope): ...`, `chore(scope): ...`.

7. **API conventions:**
   - RESTful, resource-based URLs, versioned from the path from day one:
     `/api/v1/documents/{id}`.
   - Consistent JSON error envelope: `{ "error": { "code", "message", "traceId" } }`.
   - Correlation ID (`X-Trace-Id`) threaded through every request from
     Phase 0, to avoid a painful retrofit when distributed tracing (FR-20.3)
     matters in Phase 5.

8. **Error handling:** Centralized `@ControllerAdvice` mapping a small
   exception hierarchy (`NotFoundException`, `ForbiddenException`,
   `ValidationException`, `ConflictException`) to consistent HTTP responses.

9. **Logging:** Structured JSON logs (Logback +
   `logstash-logback-encoder`) from Phase 0, ahead of Loki/ELK arriving in
   Phase 5.

10. **Testing:** JUnit 5 + Mockito for unit tests; Testcontainers for
    integration tests against real Postgres/Redis/MinIO in ephemeral
    containers (not mocks). Bar: every service class has a unit test, every
    controller has an integration test — not a specific coverage
    percentage target.

11. **CI:** GitHub Actions. Every PR runs lint + unit tests + integration
    tests. `main` additionally builds the Docker image.

12. **Coding standards:** Google Java Style enforced via Spotless/Checkstyle
    in CI (backend); ESLint + Prettier enforced via Husky pre-commit hook
    (frontend).

## Consequences

- Extracting a service in Phases 1–5 should require moving a package and
  adding an HTTP boundary, not redesigning business logic.
- Local dev has more upfront setup cost (Docker Compose with Kafka) than a
  bare `./mvnw spring-boot:run`, in exchange for environment parity later.
- Amendments to this ADR must be written as a new ADR that supersedes the
  relevant section here, with reasoning — not a silent change in code.
