# Deployment Checklist

Pre-flight checks before running the DocShare platform.

## ✅ Prerequisites

### Development Tools
- [ ] **Java 23** installed and in PATH
  ```bash
  java -version  # Should show 23.x
  ```

- [ ] **Node.js 18+** installed
  ```bash
  node --version  # Should show v18+ or v20+
  ```

- [ ] **Docker** installed and running
  ```bash
  docker --version
  docker ps  # Should not error
  ```

- [ ] **Git** installed (for version control)
  ```bash
  git --version
  ```

### System Requirements
- [ ] **8GB RAM minimum** (16GB recommended)
- [ ] **5GB disk space** for Docker images + storage
- [ ] **Ports available**: 3000, 5432, 8080

## 🐘 PostgreSQL Setup

### Option A: Docker (Recommended)
```bash
docker run -d \
  --name docshare-db \
  -e POSTGRES_DB=docshare \
  -e POSTGRES_USER=docshare \
  -e POSTGRES_PASSWORD=docshare123 \
  -p 5432:5432 \
  postgres:17

# Verify it's running
docker ps | grep docshare-db

# Should show: Up X seconds (healthy)
```

### Option B: Local Installation
```bash
# Install PostgreSQL 17
brew install postgresql@17  # macOS
# OR use your package manager

# Create database
createdb docshare

# Create user
psql -c "CREATE USER docshare WITH PASSWORD 'docshare123';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE docshare TO docshare;"
```

### Verification
```bash
# Test connection
docker exec -it docshare-db psql -U docshare -d docshare -c "SELECT version();"

# Should output PostgreSQL version info
```

## 🚀 Backend Setup

### 1. Navigate to Backend
```bash
cd backend
```

### 2. Verify Gradle Wrapper
```bash
./gradlew --version

# If not found, generate it:
gradle wrapper
```

### 3. Configure Application (Optional)
Backend uses sensible defaults. To customize:

```bash
# Copy example config (if provided)
cp src/main/resources/application.yml.example src/main/resources/application-local.yml

# Edit values
nano src/main/resources/application-local.yml
```

Default values:
- Database: `jdbc:postgresql://localhost:5432/docshare`
- JWT Secret: `change-me-in-every-environment-this-is-a-local-dev-only-default`
- Storage path: `./storage/uploads`

### 4. Run Database Migrations
Migrations run automatically on startup, but you can verify:

```bash
./gradlew bootRun

# Wait for: "Started BackendApplication in X.XXX seconds"
# Check logs for: "Flyway: Migrated to version X"
```

### 5. Verify Backend Health
```bash
# In another terminal:
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

### 6. Test Authentication Endpoint
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "Test User"
  }'

# Should return 201 with user data
```

## 🎨 Frontend Setup

### 1. Navigate to Frontend
```bash
cd frontend
```

### 2. Install Dependencies
```bash
npm install

# Should complete without errors
# Ignore peer dependency warnings (expected)
```

### 3. Create Environment File
```bash
cat > .env.local << EOF
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
EOF
```

### 4. Verify Configuration
```bash
cat .env.local

# Should show:
# NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### 5. Run Development Server
```bash
npm run dev

# Wait for: "✓ Ready in X.Xs"
# Should open on: http://localhost:3000
```

### 6. Verify Frontend Build
```bash
# In another terminal:
cd frontend
npm run build

# Should complete with:
# ✓ Compiled successfully
# Route (app) - 10 routes listed
```

## 🧪 Integration Testing

### 1. End-to-End Flow
- [ ] Open http://localhost:3000 in browser
- [ ] Click "Create one" (register link)
- [ ] Fill form: name, email, password
- [ ] Submit → Should redirect to /dashboard
- [ ] See "Welcome to docshare" message

### 2. File Operations
- [ ] Click "My Documents" in sidebar
- [ ] Click "Upload" button
- [ ] Select a test file
- [ ] File appears in list with size/date
- [ ] Click "•••" → "Download"
- [ ] File downloads successfully

### 3. Folder Operations
- [ ] Click "New Folder"
- [ ] Name it "Test Folder"
- [ ] Folder appears in list
- [ ] Click folder name
- [ ] URL shows `/documents/[id]`
- [ ] Click "Back" button

### 4. Sharing
- [ ] Click "•••" on a file
- [ ] Click "Share"
- [ ] Enter email: `friend@example.com`
- [ ] Select role: "Viewer"
- [ ] Click "Share"
- [ ] Email appears in "Shared with" list

### 5. Share Links
- [ ] In Share modal, click "Share link" tab
- [ ] Set expiry: 7 days
- [ ] Add password: `test123`
- [ ] Click "Create link"
- [ ] Link appears in "Active links"
- [ ] Click "Copy" icon
- [ ] Open link in incognito window
- [ ] Enter password
- [ ] Download button works (if not read-only)

## 🔍 Troubleshooting

### Backend Won't Start

#### Port 8080 Already in Use
```bash
# Find process
lsof -i :8080

# Kill it
kill -9 <PID>
```

#### Database Connection Failed
```bash
# Check PostgreSQL is running
docker ps | grep docshare-db

# Restart if needed
docker restart docshare-db

# Check logs
docker logs docshare-db
```

#### Flyway Migration Error
```bash
# Reset database
docker exec -it docshare-db psql -U docshare -d docshare -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# Restart backend
./gradlew bootRun
```

### Frontend Won't Start

#### Port 3000 Already in Use
```bash
# Find process
lsof -i :3000

# Kill it
kill -9 <PID>
```

#### Dependencies Failed to Install
```bash
# Clear cache
rm -rf node_modules package-lock.json
npm cache clean --force

# Reinstall
npm install
```

#### API Calls Failing (CORS)
```bash
# Verify .env.local exists
cat .env.local

# Should show backend URL
# Verify backend is running
curl http://localhost:8080/actuator/health
```

### Integration Issues

#### JWT Refresh Not Working
```bash
# Check browser console for errors
# Network tab → Check /auth/refresh response

# Common causes:
# 1. Refresh token expired (wait 7 days or clear localStorage)
# 2. Backend restarted (tokens invalidated)

# Fix: Clear localStorage in DevTools → Refresh page → Login again
```

#### File Upload Fails
```bash
# Check backend logs for errors
# Common causes:
# 1. Storage directory not writable
# 2. File size too large (no limit in Phase 0, but check disk space)

# Verify storage directory exists
ls -la backend/storage/uploads

# If not, create it
mkdir -p backend/storage/uploads
```

#### Share Link Not Working
```bash
# Check browser console
# Network tab → Check /share-links/:token response

# Common causes:
# 1. Link expired
# 2. Password incorrect
# 3. Download limit reached

# Backend logs will show specific error
```

## 📊 Smoke Test Script

Quick automated verification:

```bash
#!/bin/bash

echo "=== DocShare Smoke Test ==="

# Check PostgreSQL
echo -n "PostgreSQL: "
docker exec docshare-db pg_isready -U docshare && echo "✅" || echo "❌"

# Check backend health
echo -n "Backend:    "
curl -sf http://localhost:8080/actuator/health > /dev/null && echo "✅" || echo "❌"

# Check frontend
echo -n "Frontend:   "
curl -sf http://localhost:3000 > /dev/null && echo "✅" || echo "❌"

# Test registration
echo -n "Auth API:   "
RESPONSE=$(curl -sf -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@test.com","password":"test123","name":"Smoke Test"}' \
  -w "%{http_code}" -o /dev/null)
[[ $RESPONSE == "201" || $RESPONSE == "409" ]] && echo "✅" || echo "❌"

echo "=== Test Complete ==="
```

Save as `smoke-test.sh`, make executable, and run:

```bash
chmod +x smoke-test.sh
./smoke-test.sh
```

## 🚢 Production Checklist

Before deploying to production (Phase 5+):

### Security
- [ ] Change JWT secret to secure random value
- [ ] Use httpOnly cookies for refresh tokens
- [ ] Enable HTTPS/TLS
- [ ] Add rate limiting (API Gateway)
- [ ] Implement CSRF protection
- [ ] Add password complexity requirements
- [ ] Enable security headers (HSTS, CSP, etc.)
- [ ] Audit all dependencies for vulnerabilities

### Database
- [ ] Use managed PostgreSQL (RDS, Cloud SQL, etc.)
- [ ] Enable automated backups
- [ ] Configure connection pooling
- [ ] Set up read replicas (if needed)
- [ ] Enable SSL/TLS for connections

### Storage
- [ ] Migrate from local FS to MinIO/S3
- [ ] Configure bucket policies
- [ ] Enable server-side encryption
- [ ] Set up lifecycle policies
- [ ] Configure CORS for frontend access

### Monitoring
- [ ] Add Prometheus metrics
- [ ] Set up Grafana dashboards
- [ ] Configure alerting (PagerDuty, Slack)
- [ ] Enable distributed tracing
- [ ] Set up log aggregation (ELK, CloudWatch)

### Infrastructure
- [ ] Use Docker Compose or Kubernetes
- [ ] Configure health checks
- [ ] Set resource limits (CPU, memory)
- [ ] Enable autoscaling (Phase 6)
- [ ] Set up CI/CD pipeline

### Testing
- [ ] Run integration test suite
- [ ] Perform load testing (k6, JMeter)
- [ ] Security audit (OWASP ZAP)
- [ ] Accessibility audit (WCAG AA)
- [ ] Manual QA on production-like environment

## 📝 Quick Reference

### Start Everything
```bash
# Terminal 1: PostgreSQL
docker start docshare-db

# Terminal 2: Backend
cd backend && ./gradlew bootRun

# Terminal 3: Frontend
cd frontend && npm run dev
```

### Stop Everything
```bash
# Ctrl+C in each terminal

# Stop PostgreSQL
docker stop docshare-db
```

### Reset Everything
```bash
# Stop services
docker stop docshare-db
docker rm docshare-db

# Clear storage
rm -rf backend/storage/uploads/*

# Restart from Step 1
```

### Logs
```bash
# PostgreSQL
docker logs docshare-db

# Backend (if running in background)
tail -f backend/logs/spring.log

# Frontend (shown in terminal where npm run dev was run)
```

---

**Last Updated**: Phase 0 Complete  
**Environment**: Local Development  
**Support**: See QUICKSTART.md or IMPLEMENTATION_COMPLETE.md
