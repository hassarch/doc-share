# ADR-0002: Switch repo strategy to monorepo (supersedes part of ADR-0001)

**Status:** Accepted (2026-07-28)
**Supersedes:** ADR-0001, decision #1 ("Repo strategy: Polyrepo")

## Context

ADR-0001 chose a polyrepo (`docshare-backend`, `docshare-frontend`,
`docshare-infra`) on the reasoning that it mirrors independent ownership/
deployment and avoids monorepo tooling overhead. After scaffolding all three
repos, we've decided to switch to a single monorepo instead.

## Decision

All code lives in one repository, `docshare`, with top-level folders:

```
docshare/
  backend/         (Spring Boot monolith — was docshare-backend)
  frontend/        (Next.js app — was docshare-frontend)
  infra/           (Docker Compose, k8s, observability — was docshare-infra)
  docs/adr/        (Architecture Decision Records — repo-wide)
  README.md
  .gitignore       (root-level, covers all sub-stacks)
  .editorconfig    (root-level, covers all sub-stacks)
```

No monorepo build-graph tooling (Nx, Turborepo, Bazel) is introduced. Each
top-level folder remains independently buildable with its native tool
(Gradle in `backend/`, npm/pnpm in `frontend/`, Docker Compose in `infra/`).
This is a "folder-based" monorepo, not a build-orchestrated one — appropriate
for a solo project with only three components and no shared-library problem
to solve yet.

## Rationale for the change

- **Cross-cutting changes are common in this project.** A single feature
  (e.g. adding a new document field) often touches backend DTOs, an API
  contract, and the frontend form in the same logical unit of work. A
  monorepo lets that land as one PR with one commit history, instead of
  three PRs across three repos that have to be manually kept in sync.
- **Solo developer, not a multi-team org.** The main argument for polyrepo
  — independent team ownership and independent release cadences — doesn't
  apply here; there's one owner (you) working on all three components.
- **Still fine.** Nothing about the module-boundary rules inside `backend/`,
  the Git branching model, commit conventions, tech stack, or the Kafka
  decision changes — those were never repo-strategy-dependent.

## Consequences

- CI (GitHub Actions, set up in a later phase) needs path filters so that a
  frontend-only change doesn't trigger a backend build and vice versa —
  this is a small addition, not a blocker.
- If `backend/` is later extracted into fully independent deployables
  (Phase 1 of the PRD: Storage Service split), it can still live inside this
  same monorepo as a sibling folder (e.g. `services/storage/`), or be
  carved out into its own repo at that point — that's a decision we can
  defer until we get there, and doesn't need to be resolved now.
- Old polyrepo scaffolding (`docshare-backend`, `docshare-frontend`,
  `docshare-infra` as separate repos) is discarded in favor of this
  structure.
