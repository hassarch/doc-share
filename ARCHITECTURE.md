# DocShare - System Architecture

## High-Level Overview (Phase 0)

```
┌─────────────────────────────────────────────────────────────┐
│                         Browser                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Next.js Frontend (React)                    │  │
│  │  - App Router (route groups)                         │  │
│  │  - TanStack Query (cache)                            │  │
│  │  - JWT in localStorage                               │  │
│  └────────────────┬─────────────────────────────────────┘  │
└───────────────────┼─────────────────────────────────────────┘
                    │ HTTP/REST
                    │ (JSON)
┌───────────────────▼─────────────────────────────────────────┐
│              Spring Boot Backend                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Controllers (REST API)                              │  │
│  │    - AuthController                                  │  │
│  │    - DocumentController                              │  │
│  │    - FolderController                                │  │
│  │    - ShareController                                 │  │
│  │    - ShareLinkController                             │  │
│  └──────────────────┬───────────────────────────────────┘  │
│  ┌──────────────────▼───────────────────────────────────┐  │
│  │  Services (Business Logic)                           │  │
│  │    - JwtService                                      │  │
│  │    - AuthenticationService                           │  │
│  │    - DocumentService                                 │  │
│  │    - FolderService                                   │  │
│  │    - SharingService                                  │  │
│  │    - StorageService                                  │  │
│  └──────────────────┬───────────────────────────────────┘  │
│  ┌──────────────────▼───────────────────────────────────┐  │
│  │  Repositories (JPA/Hibernate)                        │  │
│  │    - UserRepository                                  │  │
│  │    - DocumentRepository                              │  │
│  │    - FolderRepository                                │  │
│  │    - ShareRepository                                 │  │
│  │    - ShareLinkRepository                             │  │
│  │    - RefreshTokenRepository                          │  │
│  └──────────────────┬───────────────────────────────────┘  │
└───────────────────┼─────────────────────────────────────────┘
                    │ JDBC
                    │
┌───────────────────▼─────────────────────────────────────────┐
│              PostgreSQL 17                                   │
│  - users, documents, folders                                 │
│  - shares, share_links                                       │
│  - refresh_tokens                                            │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│              Local Filesystem (Phase 0)                      │
│  /storage/uploads/{userId}/{documentId}                      │
│  (Will become MinIO in Phase 1)                              │
└──────────────────────────────────────────────────────────────┘
```

## Authentication Flow

```
┌────────┐                                    ┌────────┐
│ Client │                                    │ Server │
└───┬────┘                                    └───┬────┘
    │                                             │
    │ 1. POST /auth/login                         │
    │    { email, password }                      │
    ├────────────────────────────────────────────►│
    │                                             │
    │                           2. BCrypt verify  │
    │                              password       │
    │                                             │
    │ 3. 200 OK                                   │
    │    { accessToken, refreshToken,             │
    │      expiresInSeconds: 900 }                │
    │◄────────────────────────────────────────────┤
    │                                             │
    │ 4. Store in localStorage                    │
    │    (Phase 0 simplification)                 │
    │                                             │
    │ 5. Subsequent requests:                     │
    │    Authorization: Bearer {accessToken}      │
    ├────────────────────────────────────────────►│
    │                                             │
    │                          6. JWT verify      │
    │                             signature       │
    │                                             │
    │ 7. After 15 min: 401 Unauthorized           │
    │◄────────────────────────────────────────────┤
    │                                             │
    │ 8. POST /auth/refresh                       │
    │    { refreshToken }                         │
    ├────────────────────────────────────────────►│
    │                                             │
    │                          9. Validate token  │
    │                             in database     │
    │                                             │
    │ 10. 200 OK (new pair)                       │
    │◄────────────────────────────────────────────┤
    │                                             │
    │ 11. Replay original request                 │
    │     (automatic, transparent)                │
    ├────────────────────────────────────────────►│
    │                                             │
```

## File Upload Flow

```
┌────────┐                                    ┌────────┐
│ Client │                                    │ Server │
└───┬────┘                                    └───┬────┘
    │                                             │
    │ 1. User selects file                        │
    │    (via UploadDropzone)                     │
    │                                             │
    │ 2. POST /api/v1/documents?folderId=X        │
    │    Content-Type: multipart/form-data        │
    │    Body: FormData with file                 │
    ├────────────────────────────────────────────►│
    │                                             │
    │                          3. Save to disk    │
    │                             /storage/...    │
    │                                             │
    │                          4. Calculate       │
    │                             SHA-256 hash    │
    │                                             │
    │                          5. Insert metadata │
    │                             to PostgreSQL   │
    │                                             │
    │ 6. 201 Created                              │
    │    { id, filename, sizeBytes,               │
    │      sha256Hash, ... }                      │
    │◄────────────────────────────────────────────┤
    │                                             │
    │ 7. Invalidate React Query cache             │
    │    queryKey: ["documents", folderId]        │
    │                                             │
    │ 8. Refetch document list                    │
    ├────────────────────────────────────────────►│
    │                                             │
    │ 9. 200 OK (updated list)                    │
    │◄────────────────────────────────────────────┤
    │                                             │
```

## Sharing Flow (Direct)

```
┌─────────┐         ┌────────┐         ┌─────────┐
│ User A  │         │ Server │         │ User B  │
└────┬────┘         └───┬────┘         └────┬────┘
     │                  │                   │
     │ 1. Click "Share" │                   │
     │    on document   │                   │
     │                  │                   │
     │ 2. POST /shares  │                   │
     │    { documentId, │                   │
     │      email: B,   │                   │
     │      role: VIEWER│                   │
     ├─────────────────►│                   │
     │                  │                   │
     │               3. Verify B exists     │
     │                  (lookup by email)   │
     │                  │                   │
     │               4. Insert share row    │
     │                  in database         │
     │                  │                   │
     │ 5. 201 Created   │                   │
     │◄─────────────────┤                   │
     │                  │                   │
     │               6. (Phase 3: Kafka     │
     │                   event + WebSocket  │
     │                   notification to B) │
     │                  │                   │
     │                  │ 7. B opens app    │
     │                  │◄──────────────────┤
     │                  │                   │
     │                  │ 8. GET /documents │
     │                  │   ?sharedWithMe   │
     │                  │◄──────────────────┤
     │                  │                   │
     │                  │ 9. Document list  │
     │                  │   (includes A's   │
     │                  │   shared doc)     │
     │                  ├──────────────────►│
     │                  │                   │
```

## Share Link Flow (Public)

```
┌─────────┐         ┌────────┐         ┌─────────────┐
│ Owner   │         │ Server │         │ Public User │
└────┬────┘         └───┬────┘         └──────┬──────┘
     │                  │                      │
     │ 1. Create link   │                      │
     │    POST /share-  │                      │
     │    links         │                      │
     │    { documentId, │                      │
     │      expiresAt,  │                      │
     │      password,   │                      │
     │      readOnly }  │                      │
     ├─────────────────►│                      │
     │                  │                      │
     │               2. Generate token         │
     │                  (UUID)                 │
     │                  │                      │
     │               3. Hash password          │
     │                  (BCrypt)               │
     │                  │                      │
     │               4. Save to DB             │
     │                  │                      │
     │ 5. 201 Created   │                      │
     │    { token, ... }│                      │
     │◄─────────────────┤                      │
     │                  │                      │
     │ 6. Copy URL:     │                      │
     │    /share/{token}│                      │
     │                  │                      │
     │ 7. Share via     │                      │
     │    email/chat    │                      │
     ├─────────────────────────────────────────┤
     │                  │                      │
     │                  │ 8. Visit /share/X    │
     │                  │◄─────────────────────┤
     │                  │                      │
     │                  │ 9. POST /share-links │
     │                  │    /{token}          │
     │                  │    { password }      │
     │                  │◄─────────────────────┤
     │                  │                      │
     │              10. Verify token exists    │
     │                  not expired            │
     │                  password matches       │
     │                  │                      │
     │                  │ 11. 200 OK           │
     │                  │     { documentId,    │
     │                  │       filename,      │
     │                  │       canDownload }  │
     │                  ├─────────────────────►│
     │                  │                      │
     │                  │ 12. Click Download   │
     │                  │◄─────────────────────┤
     │                  │                      │
     │              13. Increment download_    │
     │                  count, check limit     │
     │                  │                      │
     │                  │ 14. File blob        │
     │                  ├─────────────────────►│
     │                  │                      │
```

## React Query Cache Strategy

```
Frontend Cache (React Query)
┌──────────────────────────────────────────────────┐
│                                                  │
│  Query Keys:                                     │
│    ["documents", folderId]                       │
│    ["folders", parentFolderId]                   │
│    ["shares", documentId]                        │
│    ["shareLinks", documentId]                    │
│                                                  │
│  Stale Time: 30s                                 │
│  Retry: 1 attempt                                │
│                                                  │
└──────────────────────────────────────────────────┘
              │
              │ On Mutation Success:
              │
              ▼
┌──────────────────────────────────────────────────┐
│  Invalidate Strategy:                            │
│                                                  │
│  uploadDocument()                                │
│    → invalidate ["documents", folderId]          │
│                                                  │
│  deleteDocument()                                │
│    → invalidate ["documents"] (all)              │
│                                                  │
│  createFolder()                                  │
│    → invalidate ["folders", parentId]            │
│                                                  │
│  shareDocument()                                 │
│    → invalidate ["shares", documentId]           │
│                                                  │
│  createShareLink()                               │
│    → invalidate ["shareLinks", documentId]       │
│                                                  │
└──────────────────────────────────────────────────┘
```

## Database Schema Relationships

```
┌───────────────┐
│     users     │
│───────────────│
│ id (PK)       │◄───────────────┐
│ email (UNIQUE)│                │
│ password_hash │                │
│ name          │                │
└───────────────┘                │
        │                        │
        │ owner_id               │ shared_by_user_id
        │                        │
        ▼                        │
┌───────────────┐                │
│   documents   │                │
│───────────────│                │
│ id (PK)       │◄───────┐       │
│ filename      │        │       │
│ folder_id (FK)│        │       │
│ owner_id (FK) │        │       │
│ storage_path  │        │       │
│ sha256_hash   │        │       │
└───────────────┘        │       │
        │                │       │
        │ document_id    │       │
        │                │       │
        ▼                │       │
┌───────────────┐        │       │
│    shares     │        │       │
│───────────────│        │       │
│ id (PK)       │        │       │
│ document_id(FK├────────┘       │
│ shared_by (FK)├────────────────┘
│ shared_with(FK├───────┐
│ role          │       │
└───────────────┘       │
                        │ shared_with_user_id
┌───────────────┐       │
│  share_links  │       │
│───────────────│       │
│ id (PK)       │       │
│ token (UNIQUE)│       │
│ document_id(FK├───────┤
│ created_by(FK)├───────┘
│ expires_at    │
│ password_hash │
│ download_limit│
│ download_count│
│ read_only     │
└───────────────┘

┌───────────────┐
│    folders    │
│───────────────│
│ id (PK)       │◄───┐
│ name          │    │
│ owner_id (FK) │    │ parent_folder_id
│ parent_id (FK)├────┘
└───────────────┘

┌───────────────────┐
│  refresh_tokens   │
│───────────────────│
│ token (PK, UUID)  │
│ user_id (FK)      │
│ expires_at        │
│ revoked           │
└───────────────────┘
```

## Phase Evolution

### Phase 0 (Current)
```
Frontend → Backend → PostgreSQL
              ↓
           Local FS
```

### Phase 1 (Storage Split)
```
Frontend → Backend → Metadata DB (PostgreSQL)
              ↓
           Storage Service → MinIO
```

### Phase 2 (Replication)
```
Frontend → Backend → Metadata DB
              ↓
           Storage Service → MinIO (3 nodes)
                            Replication Factor: 3
```

### Phase 3 (Events)
```
Frontend ←──WebSocket─── Notification Service
   ↓                              ↑
Backend → Kafka Event Bus ────────┤
   ↓           ↑                   ↑
Database   Storage Service    Audit Service
```

### Phase 4+ (Full Distributed)
```
                    API Gateway
                   (Rate Limit, Circuit Breaker)
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
    Frontend         Metadata Svc      Storage Svc
        │                 │                 │
        │            PostgreSQL          MinIO Cluster
        │                                    │
        │                            Redis (Dedup Cache)
        │
   WebSocket ← Notification Service
                     ↑
                 Kafka Bus
                     ↑
              ┌──────┴──────┐
         Audit Svc      Event Processors
```

## Security Layers

```
┌──────────────────────────────────────────────────┐
│              Application Layer                    │
│  - Input validation (Jakarta Validation)         │
│  - SQL injection prevention (JPA/Hibernate)      │
│  - XSS prevention (React escape by default)      │
└────────────────┬─────────────────────────────────┘
                 │
┌────────────────▼─────────────────────────────────┐
│           Authentication Layer                    │
│  - JWT signature verification (HMAC-SHA256)      │
│  - Token expiry enforcement (15min access)       │
│  - Refresh token rotation (7 days)               │
│  - BCrypt password hashing (strength 12)         │
└────────────────┬─────────────────────────────────┘
                 │
┌────────────────▼─────────────────────────────────┐
│           Authorization Layer                     │
│  - Document ownership checks                     │
│  - Share permission verification (VIEWER/EDITOR) │
│  - Folder access inheritance                     │
│  - Share link validation (expiry, password)      │
└────────────────┬─────────────────────────────────┘
                 │
┌────────────────▼─────────────────────────────────┐
│              Network Layer                        │
│  - CORS (localhost:3000 in dev)                  │
│  - HTTPS (production only)                       │
│  - Rate limiting (Phase 5)                       │
└──────────────────────────────────────────────────┘
```

## Technology Choices Rationale

| Component | Choice | Why |
|-----------|--------|-----|
| **Frontend Framework** | Next.js 16 App Router | SSR for shared links, RSC for metadata, file-based routing |
| **Backend Framework** | Spring Boot 3.4 | Production-ready, excellent DI, Spring Security integration |
| **Database** | PostgreSQL 17 | ACID transactions, JSON support, FTS for Phase 4 search |
| **ORM** | JPA/Hibernate | Type-safe, prevents SQL injection, lazy loading for performance |
| **Auth** | JWT (HMAC-SHA256) | Stateless, scalable, refresh pattern for security/UX balance |
| **Password Hash** | BCrypt (strength 12) | Industry standard, adaptive work factor, salt built-in |
| **State Management** | TanStack Query | Server state cache, automatic invalidation, optimistic updates |
| **UI Components** | Radix UI | Accessible, unstyled primitives, composable |
| **Styling** | Tailwind CSS 4 | Utility-first, fast iteration, consistent scale |
| **File Storage** | Local FS → MinIO | Local for Phase 0, S3-compatible for Phase 1+ |
| **Event Bus** | (Phase 3) Kafka | Durable, scalable, event sourcing support |
| **Observability** | (Phase 5) Prometheus + Grafana | Metrics, alerting, distributed tracing |

---

**Last Updated**: Phase 0 Complete  
**Next**: Phase 1 - Storage/Metadata Service Split
