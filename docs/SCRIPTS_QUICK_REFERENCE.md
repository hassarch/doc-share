# Scripts Quick Reference

## 🎯 Quick Commands

### Build Everything
```bash
# Full build with checks (recommended first time)
./build-all.sh

# Quick build without prompts (for daily use)
./quick-build.sh
```

### Start Application
```bash
# Start backend and frontend in separate terminals
./start-app.sh

# Or manually:
# Terminal 1:
cd backend && ./gradlew bootRun

# Terminal 2:
cd frontend && npm run dev
```

### Docker Services
```bash
# Start services
cd infra && docker-compose up -d postgres redis minio kafka

# Stop services
cd infra && docker-compose down

# Reset all data (WARNING: deletes everything!)
cd infra && docker-compose down -v
```

---

## 📁 Script Files

| Script | Purpose | When to Use |
|--------|---------|-------------|
| `build-all.sh` | Complete build with checks | First time, before deploy |
| `quick-build.sh` | Fast build without tests | Daily development |
| `start-app.sh` | Start backend + frontend | After building |

---

## 🚀 Typical Workflows

### First Time Setup
```bash
1. ./build-all.sh          # Build everything
2. ./start-app.sh          # Start the app
3. Open http://localhost:3000
```

### Daily Development
```bash
1. ./quick-build.sh        # Quick rebuild
2. ./start-app.sh          # Start the app
```

### After Pulling Changes
```bash
1. cd infra && docker-compose up -d postgres redis minio kafka
2. ./quick-build.sh
3. ./start-app.sh
```

### Before Committing
```bash
1. ./build-all.sh          # Full build with checks
2. cd backend && ./gradlew test     # Run tests
3. cd frontend && npm test          # Run frontend tests
```

---

## 🛠️ Manual Commands

### Backend Only
```bash
cd backend

# Clean and build
./gradlew clean build -x test

# Format code
./gradlew spotlessApply

# Run tests
./gradlew test

# Start server
./gradlew bootRun
```

### Frontend Only
```bash
cd frontend

# Install dependencies
npm install

# Build
npm run build

# Start dev server
npm run dev

# Lint
npm run lint

# Type check
npm run type-check
```

---

## 📊 Build Outputs

After successful build:

```
backend/build/libs/backend-0.0.1-SNAPSHOT.jar  ← Backend JAR
frontend/.next/                                 ← Frontend build
```

---

## 🐛 Common Issues

### "Permission denied"
```bash
chmod +x build-all.sh quick-build.sh start-app.sh
```

### "Port already in use"
```bash
# Backend (port 8080)
lsof -ti:8080 | xargs kill -9

# Frontend (port 3000)
lsof -ti:3000 | xargs kill -9
```

### "Docker not running"
```bash
open -a Docker
# Wait 15 seconds, then retry
```

### "Build failed"
```bash
# Clean everything and rebuild
cd backend && ./gradlew clean
cd ../frontend && rm -rf node_modules .next
cd .. && ./build-all.sh
```

---

## 🌐 Application URLs

After starting:

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | Register new user |
| Backend API | http://localhost:8080 | N/A |
| MinIO Console | http://localhost:9001 | docshare / docshare123 |

---

## ⚡ Performance Tips

### Speed up Gradle builds:
Add to `~/.gradle/gradle.properties`:
```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

### Speed up npm:
```bash
npm install -g pnpm
cd frontend
pnpm install  # Instead of npm install
pnpm build    # Instead of npm run build
```

---

## 📝 Cheat Sheet

```bash
# BUILD
./quick-build.sh              # Fast build
./build-all.sh                # Full build with checks

# START
./start-app.sh                # Start both services
cd backend && ./gradlew bootRun     # Backend only
cd frontend && npm run dev          # Frontend only

# DOCKER
cd infra && docker-compose up -d postgres redis minio kafka  # Start
cd infra && docker-compose down     # Stop
docker ps                           # Check status

# CLEAN
cd backend && ./gradlew clean       # Clean backend
cd frontend && rm -rf .next         # Clean frontend
cd infra && docker-compose down -v  # Clean database (WARNING!)

# TEST
cd backend && ./gradlew test        # Backend tests
cd frontend && npm test             # Frontend tests

# FORMAT
cd backend && ./gradlew spotlessApply   # Format Java
cd frontend && npm run lint --fix       # Format TypeScript
```

---

**Need more details? See [BUILD_SCRIPTS.md](./BUILD_SCRIPTS.md)**
