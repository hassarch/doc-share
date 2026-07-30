# DocShare 📄

> **Distributed Document Sharing & Synchronization Platform**

A modern, full-stack document management system built with Next.js and Spring Boot, featuring real-time collaboration, advanced sharing controls, and distributed storage architecture.

## 🎉 Phase 0 Complete!

This implementation delivers a **production-quality foundation** with:

✅ **Authentication** - JWT-based auth with automatic refresh  
✅ **File Management** - Upload, download, rename, delete  
✅ **Folder Organization** - Nested folder hierarchy  
✅ **Direct Sharing** - Share with users by email (VIEWER/EDITOR roles)  
✅ **Public Links** - Password-protected, expiring, download-limited  
✅ **Responsive UI** - Mobile-friendly (375px+)  
✅ **Type-Safe** - TypeScript + Java with strict validation

## 🚀 Quick Start

Get running in **5 minutes**:

```bash
# 1. Start PostgreSQL
docker run -d --name docshare-db \
  -e POSTGRES_DB=docshare \
  -e POSTGRES_USER=docshare \
  -e POSTGRES_PASSWORD=docshare123 \
  -p 5432:5432 postgres:17

# 2. Start backend
cd backend && ./gradlew bootRun

# 3. Start frontend (new terminal)
cd frontend && npm install && npm run dev
```

Open **http://localhost:3000** and register a new account!

📖 **Detailed guide**: [QUICKSTART.md](./QUICKSTART.md)

## 📦 What's Inside

### Backend (Spring Boot 3.4 + Java 23)
- RESTful API with consistent error handling
- JWT authentication with refresh token rotation
- JPA/Hibernate for type-safe database access
- BCrypt password hashing
- File storage (local → MinIO in Phase 1)
- Comprehensive test coverage (JUnit, Mockito, Testcontainers)

### Frontend (Next.js 16 + TypeScript)
- App Router with route groups (protected/public)
- TanStack Query for server state management
- Radix UI for accessible components
- Tailwind CSS 4 for styling
- Optimistic UI updates where safe

## 🏗️ Architecture

```
Frontend (Next.js) ──HTTP/REST──► Backend (Spring Boot) ──JDBC──► PostgreSQL
                                         │
                                         └──► Local FS (storage)
```

**Phase 1+** will add: MinIO, Kafka, WebSocket notifications, API Gateway

📐 **Full diagrams**: [ARCHITECTURE.md](./ARCHITECTURE.md)

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| **[QUICKSTART.md](./QUICKSTART.md)** | Get running in 5 minutes |
| **[IMPLEMENTATION_COMPLETE.md](./IMPLEMENTATION_COMPLETE.md)** | Full feature list, tech stack, roadmap |
| **[ARCHITECTURE.md](./ARCHITECTURE.md)** | System design, flows, schemas |
| **[frontend/FRONTEND_README.md](./frontend/FRONTEND_README.md)** | Frontend architecture guide |
| **[backend/README.md](./backend/README.md)** | API documentation |

## 🎯 Key Features

### Authentication & Security
- User registration and login
- JWT tokens (15min access, 7-day refresh)
- Automatic token refresh (transparent to user)
- BCrypt password hashing (strength 12)
- Protected routes with auth guards

### File Management
- Upload files with metadata extraction
- Download with proper content types
- Rename with instant feedback
- Delete with confirmation
- SHA-256 hash for integrity

### Folder Organization
- Create nested folder structures
- Navigate hierarchy with breadcrumbs
- Move files between folders
- Cascade delete folders

### Sharing
- **Direct shares**: Email + role (VIEWER/EDITOR)
- **Share links**: Public URLs with:
  - Password protection
  - Expiration dates (days)
  - Download limits
  - Read-only mode
- Permission management UI
- Revoke access instantly

### UI/UX
- Clean, modern interface
- Mobile-responsive (375px+)
- Loading states and error handling
- Empty states with helpful actions
- Keyboard accessible (Radix UI)

## 🛠️ Tech Stack

### Frontend
```
Framework:      Next.js 16 (App Router)
Language:       TypeScript 5 (strict)
Styling:        Tailwind CSS 4
State:          TanStack Query 5
Forms:          React Hook Form 7 + Zod 3
UI:             Radix UI 2
Icons:          Lucide React
```

### Backend
```
Framework:      Spring Boot 3.4.1
Language:       Java 23
Database:       PostgreSQL 17
ORM:            JPA/Hibernate
Security:       Spring Security + JWT
Build:          Gradle 8.12
Testing:        JUnit 5, Mockito, Testcontainers
```

## 📋 API Endpoints

```
Auth:        POST /api/v1/auth/{register,login,logout,refresh}
Documents:   GET/POST/PATCH/DELETE /api/v1/documents
Folders:     GET/POST/DELETE /api/v1/folders
Shares:      POST/GET/DELETE /api/v1/shares
Share Links: POST/GET/DELETE /api/v1/share-links
Public:      POST /api/v1/share-links/{token}
```

📘 **Full API docs**: [backend/README.md](./backend/README.md)

## 🗺️ Roadmap

### ✅ Phase 0 (Current) - Core Loop
- Auth, files, folders, sharing

### Phase 1 - Storage/Metadata Split
- MinIO for object storage
- Separate metadata service
- Storage service API

### Phase 2 - Replication & Resilience
- Multi-node storage (3x replication)
- Download failover UX
- Replication status UI

### Phase 3 - Events & Notifications
- Kafka event bus
- WebSocket real-time notifications
- Activity feed & audit log

### Phase 4 - Advanced Features
- Chunked uploads (5MB chunks, resume)
- Version history with restore
- Document tagging
- Full-text search (Postgres FTS → Elasticsearch)

### Phase 5 - Gateway & Observability
- API Gateway (rate limiting, circuit breaker)
- Distributed tracing with correlation IDs
- Prometheus metrics + Grafana dashboards

### Phase 6 - Kubernetes
- Helm charts
- Horizontal Pod Autoscaling
- Production readiness

## 🧪 Testing

### Backend
```bash
cd backend
./gradlew test           # Run all tests
./gradlew test --tests "*ControllerTest"  # Controllers only
```

### Frontend
```bash
cd frontend
npm test                 # Unit tests (Phase 1+)
npm run test:e2e         # E2E with Playwright (Phase 4+)
```

## 🔒 Security Notes

**⚠️ Phase 0 Simplifications** (for production):

1. **Tokens in localStorage**: Should use httpOnly cookies for refresh tokens
2. **No rate limiting**: Phase 5 adds API Gateway
3. **Local file storage**: Should use S3/MinIO
4. **Basic password validation**: No complexity requirements yet

**✅ Current Security**:
- Passwords hashed with BCrypt (strength 12)
- JWT signed with HMAC-SHA256
- SQL injection prevention (JPA)
- Input validation (Jakarta Validation)
- CORS configured

## 🤝 Contributing

When extending this codebase:

1. Follow the **thin client** principle (business logic in backend)
2. Maintain **cache invalidation** (React Query patterns)
3. Add **tests** for new features
4. Use **ErrorEnvelope** pattern for errors
5. Follow **phase progression** (don't skip ahead)

## 📝 License

MIT License - see [LICENSE](./LICENSE)

## 🙏 Acknowledgments

Built following the **Frontend Plan** and **Platform PRD v1.0** specifications, implementing a distributed systems architecture with a focus on scalability, security, and developer experience.

---

**Status**: Phase 0 Complete ✅  
**Demo-Ready**: Yes 🎬  
**Production-Ready**: Partially (see Security Notes)  
**Next Milestone**: Phase 1 - Storage/Metadata Split
