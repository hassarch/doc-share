# DocShare Frontend

Distributed Document Sharing & Synchronization Platform - Frontend Implementation

## Phase 0 Complete ✅

This implementation covers **Phase 0: Core Loop** from the Frontend Plan, providing:

- ✅ Authentication (register/login/logout) with JWT handling
- ✅ Protected routes with auth guards
- ✅ File browser with list view
- ✅ File upload (single-shot, no chunking yet)
- ✅ File download with signed URLs
- ✅ File operations: rename, delete
- ✅ Folder management: create, navigate, delete
- ✅ Nested folder navigation
- ✅ React Query cache invalidation on mutations
- ✅ Responsive design (375px+)

## Tech Stack

| Concern | Choice | Version |
|---------|--------|---------|
| Framework | Next.js App Router | 16.2+ |
| Language | TypeScript (strict) | 5.x |
| Styling | Tailwind CSS 4 | 4.x |
| Data fetching | TanStack Query | 5.x |
| Forms | React Hook Form + Zod | 7.x / 3.x |
| UI Primitives | Radix UI | 2.x |
| Auth | JWT (memory) + refresh | — |

## Project Structure

```
src/
├── app/
│   ├── (app)/              # Protected routes group
│   │   ├── layout.tsx      # Auth guard + AppShell wrapper
│   │   ├── dashboard/      # Dashboard home
│   │   ├── documents/      # File browser (root + [folderId])
│   │   ├── shared/         # Shared with me (placeholder)
│   │   └── starred/        # Starred docs (placeholder)
│   ├── login/              # Auth pages (unprotected)
│   ├── register/
│   ├── layout.tsx          # Root layout with AuthProvider
│   └── page.tsx            # Root redirect
├── components/
│   ├── common/             # Reusable UI components
│   │   ├── Button.tsx
│   │   ├── EmptyState.tsx
│   │   └── ConfirmDeleteDialog.tsx
│   ├── documents/          # File/folder components
│   │   ├── FileBrowser.tsx
│   │   ├── FileRow.tsx
│   │   ├── FolderRow.tsx
│   │   ├── UploadDropzone.tsx
│   │   ├── CreateFolderDialog.tsx
│   │   └── RenameFileDialog.tsx
│   └── layout/             # App shell components
│       ├── AppShell.tsx
│       ├── Sidebar.tsx
│       └── Topbar.tsx
├── context/
│   └── AuthContext.tsx     # Global auth state
├── hooks/
│   └── use-documents.ts    # React Query hooks
├── lib/
│   ├── api.ts              # Core API client with refresh logic
│   ├── auth-api.ts         # Auth endpoints
│   ├── documents-api.ts    # Document/folder endpoints
│   ├── query-client.ts     # React Query config
│   └── utils.ts            # Helpers (cn, formatBytes, formatDate)
└── app/globals.css         # Global styles + design tokens
```

## Design System

The app uses a minimal design token layer on top of Tailwind:

- **Colors**: `teal` (primary), `graphite` (text), `mist` (soft), `brick` (danger), `paper` (surface)
- **Typography**: Fraunces (display), IBM Plex Sans (body), IBM Plex Mono (code)
- **Spacing**: Tailwind defaults (4px base unit)

See `globals.css` for full token definitions.

## Key Implementation Patterns

### 1. Auth Flow (FR-1.5 token refresh)

```typescript
// lib/api.ts - apiFetch wrapper
// On 401 → attempt silent refresh → replay request → redirect if failed
```

- Access token stored in `localStorage` (⚠️ Phase 0 simplification)
- Refresh token also in `localStorage` (⚠️ should be httpOnly cookie in production)
- Single-flight refresh prevents multiple concurrent refresh calls

### 2. React Query Cache Invalidation (FR-14.2)

```typescript
// hooks/use-documents.ts
onSuccess: (data) => {
  queryClient.invalidateQueries({ queryKey: ["documents", data.folderId] });
}
```

Every mutation invalidates relevant query keys, mirroring backend Redis invalidation.

### 3. Optimistic vs Pessimistic UI

- **Optimistic**: Rename (not yet implemented in mutation, but hooks ready)
- **Pessimistic**: Upload, delete (wait for server confirmation before invalidating)

### 4. File Operations

- **Upload**: `FormData` → `/api/v1/documents?folderId=X`
- **Download**: Fetch blob → trigger browser download (no navigation)
- **Rename**: PATCH with new filename
- **Delete**: Confirmation dialog → DELETE

### 5. Route Groups

- `(app)`: Protected routes with `AppShell` (sidebar + topbar)
- `login`, `register`: Unprotected, full-screen layouts

## Running the Frontend

### Development

```bash
npm run dev
```

Opens on `http://localhost:3000` (configurable via `.env.local`)

### Environment Variables

Create `.env.local`:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### Build

```bash
npm run build
npm start
```

## Phase Roadmap

### Phase 0 ✅ (Current)
- Basic file/folder CRUD
- Auth with JWT
- Single-file upload

### Phase 1 (Next)
- No UI changes (backend storage split is internal)
- Add loading states for network resilience

### Phase 2
- Replication status indicator on file rows
- Download retry UX for storage node failover
- `/status` page for demo

### Phase 3
- WebSocket connection for realtime notifications
- NotificationBell component
- ActivityFeed
- OAuth login buttons (Google/GitHub)

### Phase 4
- Chunked upload (5MB chunks, resume support)
- VersionHistoryPanel with restore
- Tag editor
- Search bar + filters

### Phase 5
- Gateway error standardization
- Correlation ID headers for tracing
- Rate-limit UX
- Circuit breaker states

## Security Notes

⚠️ **Phase 0 simplifications** (flagged for production hardening):

1. **localStorage for tokens**: Readable by any script (XSS risk). Should use httpOnly cookies for refresh tokens.
2. **No CSRF protection**: Will add when moving refresh token to cookies.
3. **No input sanitization**: Rely on backend validation (never trust client).

## Testing Strategy

Phase 0 has no tests yet. Planned for Phase 1+:

- **Unit**: Vitest + React Testing Library for components
- **E2E**: Playwright for critical flows (upload → share → download)

## Known Issues / TODOs

- [ ] Chunked upload (Phase 4)
- [ ] Version history UI (Phase 4)
- [ ] Sharing UI (Phase 0 backend complete, UI placeholder exists)
- [ ] Search (Phase 4)
- [ ] Dark mode (nice-to-have, not blocking)
- [ ] Move auth tokens to httpOnly cookies (security hardening)
- [ ] Add correlation IDs for distributed tracing (Phase 5)

## Contributing

When adding new features:

1. Match the backend phase progression (don't build UI for unshipped backend)
2. Invalidate React Query caches on mutations
3. Use the existing component patterns (Button, Dialog, etc.)
4. Keep the thin-client principle: no business logic duplication

## API Contract

See `backend/README.md` for full API documentation. Frontend expects:

- **Auth**: `POST /api/v1/auth/{login,register,logout,refresh}`
- **Documents**: `GET/POST/PATCH/DELETE /api/v1/documents`
- **Folders**: `GET/POST/DELETE /api/v1/folders`
- **Download**: `GET /api/v1/documents/:id/download`

All endpoints return `ErrorEnvelope` on error (see `lib/api.ts`).
