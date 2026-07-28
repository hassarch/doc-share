# infra

Infrastructure-as-code and operational configuration for the Distributed
Document Sharing & Synchronization Platform. Part of the `docshare` monorepo.

## Contents

- `docker-compose.yml` - local dev stack (Postgres, Redis, MinIO, Kafka, and
  from Phase 5 onward, Prometheus/Grafana). Built out starting in Phase 0.
- `k8s/` - Kubernetes manifests (Phase 6, optional/stretch).
- `observability/` - Grafana dashboards, Prometheus scrape configs (Phase 5).

## Local development stack

```bash
docker compose up -d
```

Starts:

| Service | Port(s) | Notes |
|---|---|---|
| Postgres | 5432 | db `docshare`, user/pass `docshare`/`docshare` |
| Redis | 6379 | no auth (local dev only) |
| MinIO | 9000 (S3 API), 9001 (console) | user/pass `docshare`/`docshare123` — this is storage node "A"; nodes B/C/D are added in the Multi-Node Storage phase |
| Kafka | 9092 | single broker, KRaft mode (no Zookeeper) |

Check everything is healthy:

```bash
docker compose ps
```

Tear down (keeps volumes/data):

```bash
docker compose down
```

Tear down and wipe all data:

```bash
docker compose down -v
```

## Status

Base stack (Postgres/Redis/MinIO/Kafka) is in place as of Phase 2 (Project Initialization). `k8s/` and `observability/` remain empty until their respective phases.
