# infra

Infrastructure-as-code and operational configuration for the Distributed
Document Sharing & Synchronization Platform. Part of the `docshare` monorepo.

## Contents

- `docker-compose.yml` - local dev stack (Postgres, Redis, MinIO, Kafka, and
  from Phase 5 onward, Prometheus/Grafana). Built out starting in Phase 0.
- `k8s/` - Kubernetes manifests (Phase 6, optional/stretch).
- `observability/` - Grafana dashboards, Prometheus scrape configs (Phase 5).

## Status

`docker-compose.yml` for Postgres/Redis/MinIO will be added as part of
Phase 0.
