# infra

Infrastructure-as-code and operational configuration for the Distributed
Document Sharing & Synchronization Platform. Part of the `docshare` monorepo.

## Contents

- `docker-compose.yml` - local dev **dependencies only** (Postgres, Redis, MinIO, Kafka) - run the actual apps yourself via `./gradlew bootRun` / `npm run dev` for fast iteration with a debugger attached.
- `docker-compose.prod.yml` - the **complete system**, including built backend/frontend images. Use this to run the whole thing end-to-end, or as a deployment starting point.
- `.env.prod.example` - template for the secrets `docker-compose.prod.yml` needs. Copy to `.env.prod` (gitignored) and fill in real values.
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

## Running the full stack

```bash
cp .env.prod.example .env.prod
# edit .env.prod - see its comments for what each value needs to be

docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Frontend: `http://localhost:3000` (or whatever `FRONTEND_ORIGIN` you configured maps to). Backend health: `http://localhost:8080/actuator/health`.

Tear down:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod down
```

**Secrets note:** `.env.prod` is a local/self-hosted-demo convenience, not a real production secrets story — for an actual cloud deployment, `DB_PASSWORD`/`JWT_SECRET`/`MINIO_SECRET_KEY` belong in a real secrets manager (your cloud provider's, or Vault/Doppler/etc.), injected as environment variables at deploy time, never sitting in a file on a server's disk.

## Status

Base stack (Postgres/Redis/MinIO/Kafka) is in place as of Phase 2 (Project Initialization). `k8s/` and `observability/` remain empty until their respective phases.
