# DocShare Scripts

All build and utility scripts for the DocShare project.

## 📜 Available Scripts

### 1. `build-all.sh` - Complete Build ⭐

**Full-featured build with all checks**

```bash
./build-all.sh
```

**What it does:**
- ✅ Checks prerequisites (Java, Node, Docker)
- ✅ Starts Docker services
- ✅ Verifies service health
- ✅ Formats code (Spotless)
- ✅ Builds backend (JAR)
- ✅ Builds frontend (production)
- ✅ Optional test execution
- ✅ Shows detailed summary

**Use when:**
- First time building
- Before deploying
- Need comprehensive checks

---

### 2. `quick-build.sh` - Fast Build ⚡

**Minimal rebuild for development**

```bash
./quick-build.sh
```

**What it does:**
- 🚀 Starts Docker services (silent)
- 🚀 Builds backend (no tests)
- 🚀 Builds frontend
- 🚀 Completes in 30-60 seconds

**Use when:**
- Daily development
- Quick iteration
- CI/CD pipelines

---

### 3. `start-app.sh` - Run Application 🚀

**Starts backend and frontend automatically**

```bash
./start-app.sh
```

**What it does:**
- 🎯 Checks Docker services
- 🎯 Starts if not running
- 🎯 Opens backend in Terminal 1
- 🎯 Opens frontend in Terminal 2
- 🎯 Shows application URLs

**Use when:**
- Ready to run the app
- After building successfully

---

### 4. `health-check.sh` - Service Health ❤️

**Checks all service health**

```bash
./health-check.sh
```

**What it does:**
- 🏥 Checks Docker running
- 🏥 Checks PostgreSQL
- 🏥 Checks Redis
- 🏥 Checks MinIO
- 🏥 Checks Kafka
- 🏥 Shows detailed status

**Use when:**
- Debugging service issues
- Verifying startup
- Before running tests

---

## 🚀 Quick Start

### First Time Setup

```bash
# 1. Make scripts executable
chmod +x *.sh

# 2. Build everything
./build-all.sh

# 3. Start the app
./start-app.sh

# 4. Open browser
# http://localhost:3000
```

### Daily Development

```bash
# Quick rebuild
./quick-build.sh

# Start app
./start-app.sh
```

---

## 📋 Script Details

### build-all.sh

**Workflow:**
```
1. Check prerequisites
   ├── Java 21+
   ├── Node.js 18+
   ├── npm
   └── Docker
2. Start services
   ├── PostgreSQL
   ├── Redis
   ├── MinIO
   └── Kafka
3. Build backend
   ├── Clean
   ├── Format (Spotless)
   ├── Compile
   └── Create JAR
4. Build frontend
   ├── Install deps
   ├── Lint
   ├── Type check
   └── Build
5. Optional tests
6. Show summary
```

**Exit codes:**
- `0` - Success
- `1` - Prerequisite missing
- `1` - Build failed

---

### quick-build.sh

**Workflow:**
```
1. Start Docker (silent)
2. Wait 10 seconds
3. Build backend (quiet)
4. Build frontend (quiet)
5. Done!
```

**Exit codes:**
- `0` - Success
- `1` - Build failed

---

### start-app.sh

**Workflow:**
```
1. Check Docker services
2. Start if needed
3. Open backend terminal
   └── cd backend && ./gradlew bootRun
4. Open frontend terminal
   └── cd frontend && npm run dev
5. Show URLs
```

**Platforms:**
- ✅ macOS (Terminal.app)
- ✅ Linux (gnome-terminal, xterm)
- ⚠️ Windows (manual start required)

---

### health-check.sh

**Checks:**
```
Docker Daemon       → docker ps
PostgreSQL          → port 5432
Redis               → port 6379
MinIO               → ports 9000, 9001
Kafka               → port 9092
Backend             → port 8080 (if running)
Frontend            → port 3000 (if running)
```

**Output:**
- ✅ Service healthy
- ❌ Service not running
- ⚠️ Service issue

---

## 🎯 Usage Examples

### Example 1: Clean Build
```bash
# Clean everything first
cd ../backend && ./gradlew clean
cd ../frontend && rm -rf .next node_modules
cd ../scripts

# Full build
./build-all.sh
```

### Example 2: Quick Iteration
```bash
# Make code changes...

# Quick rebuild
./quick-build.sh

# Restart app
./start-app.sh
```

### Example 3: Debug Services
```bash
# Check service health
./health-check.sh

# If issues, restart services
cd ../infra
docker-compose restart

# Check again
cd ../scripts
./health-check.sh
```

### Example 4: Pre-Deployment
```bash
# Full build with tests
./build-all.sh
# Select 'y' for tests

# Verify
./health-check.sh

# Deploy
cd ../backend/build/libs
ls -lh backend-0.0.1-SNAPSHOT.jar
```

---

## 🔧 Customization

### Modify Wait Time

Edit any script:
```bash
sleep 10  # Change to desired seconds
```

### Skip Docker

Edit `quick-build.sh`:
```bash
# Comment out:
# cd infra
# docker-compose up -d ...
```

### Change Terminal App

Edit `start-app.sh` for your terminal:
```bash
# For iTerm on macOS:
osascript <<EOF
tell application "iTerm"
    # ...
end tell
EOF
```

---

## 🐛 Troubleshooting

### "Permission denied"
```bash
chmod +x *.sh
```

### "Command not found: ./build-all.sh"
```bash
# Use full path
/Users/donut/Desktop/Dev/docshare/scripts/build-all.sh

# Or add to PATH
export PATH="$PATH:/Users/donut/Desktop/Dev/docshare/scripts"
```

### Scripts won't start Docker
```bash
# Start Docker manually
open -a Docker
sleep 20

# Then run script
./build-all.sh
```

### Build fails
```bash
# Check detailed logs
./build-all.sh 2>&1 | tee build.log

# Review log
cat build.log
```

---

## 📚 Documentation

Full documentation in [`../docs/`](../docs/):

- [BUILD_SCRIPTS.md](../docs/BUILD_SCRIPTS.md) - Comprehensive guide
- [SCRIPTS_QUICK_REFERENCE.md](../docs/SCRIPTS_QUICK_REFERENCE.md) - Command cheat sheet
- [BUILD_SUCCESS.md](../docs/BUILD_SUCCESS.md) - Success indicators
- [ALL_BUILD_ERRORS_FIXED.md](../docs/ALL_BUILD_ERRORS_FIXED.md) - Error fixes

---

## 🔗 Related Resources

### Project Root
- [Main README](../README.md)
- [Documentation](../docs/)

### Backend
- [Backend README](../backend/README.md)
- [Backend Build](../backend/build.gradle.kts)

### Frontend
- [Frontend README](../frontend/README.md)
- [Package.json](../frontend/package.json)

### Infrastructure
- [Docker Compose](../infra/docker-compose.yml)
- [Production Config](../infra/docker-compose.prod.yml)

---

## 📊 Script Stats

- **Total Scripts:** 4
- **Lines of Code:** ~400
- **Languages:** Bash
- **Last Updated:** 2026-08-02

---

## 💡 Tips

### Run from anywhere
```bash
# Add alias to ~/.zshrc or ~/.bashrc
alias docshare-build='cd /path/to/docshare/scripts && ./build-all.sh'
alias docshare-quick='cd /path/to/docshare/scripts && ./quick-build.sh'
alias docshare-start='cd /path/to/docshare/scripts && ./start-app.sh'
```

### Use with make
Create `Makefile` in project root:
```makefile
build:
	scripts/build-all.sh

quick:
	scripts/quick-build.sh

start:
	scripts/start-app.sh

check:
	scripts/health-check.sh
```

Then run:
```bash
make build
make start
```

---

**Need help?** Check the [documentation](../docs/) or the [main README](../README.md).
