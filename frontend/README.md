# frontend

Next.js / TypeScript / Tailwind client for the Distributed Document Sharing & Synchronization Platform. Part of the `docshare` monorepo.

Scaffolded with `create-next-app` (App Router, `src/` directory, Tailwind, ESLint, TypeScript, import alias `@/*`).

## Local development

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

By default this expects the backend API at `http://localhost:8080` - see `../infra/docker-compose.yml` for standing up the full local stack, and `../backend/README.md` for running the API itself.

## Status

Phase 2 (Project Initialization) - bare scaffold only, no real pages/features yet. Those land starting in the Frontend phase, once the backend APIs they depend on exist.

## Structure

```
frontend/
  src/app/       App Router pages/layouts
  public/        Static assets
```
