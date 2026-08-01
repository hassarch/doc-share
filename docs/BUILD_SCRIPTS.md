# Build Scripts Guide

This document explains the build scripts available for the DocShare project.

## Available Scripts

### 1. `build-all.sh` - Complete Build with Checks ⭐

**Full-featured build script with interactive prompts and detailed output.**

#### Features:
- ✅ Checks all prerequisites (Java, Node.js, Docker)
- ✅ Starts Docker services automatically
- ✅ Verifies service health
- ✅ Applies code formatting
- ✅ Builds backend with full output
- ✅ Builds frontend with type checking
- ✅ Optional test execution
- ✅ Colored output and progress indicators
- ✅ Detailed error messages

#### Usage:
```bash
./build-all.sh
```

#### What it does:
1. Checks Java 21+, Node.js 18+, npm, and Docker
2. Starts PostgreSQL, Redis, MinIO, and Kafka
3. Waits for services to be ready
4. Cleans and builds backend:
   - Runs Spotless formatter
   - Compiles Java code
   - Creates JAR file
5. Builds frontend:
   - Installs dependencies (if needed)
   - Creates .env.local (if needed)
   - Runs linter
   - Type checks
   - Creates production build
6. Optionally runs tests (asks you)
7. Shows summary of what was built

#### When to use:
- First time building the project
- After major changes
- Before deploying
- When you want comprehensive checks

---

### 2. `quick-build.sh` - Fast Build without Tests ⚡

**Minimal script that just builds everything quickly.**

#### Features:
- 🚀 No interactive prompts
- 🚀 Minimal output
- 🚀 Skips tests by default
- 🚀 Fast execution

#### Usage:
```bash
./quick-build.sh
```

#### What it does:
1. Starts Docker services (silently)
2. Waits 10 seconds
3. Builds backend (skips tests)
4. Builds frontend
5. Done!

#### When to use:
- Daily development
- Quick rebuild after changes
- CI/CD pipelines
- When you're confident everything works

---

## Prerequisites

Before running any build script, ensure you have:

### Required Software:
- **Java 21+** - Backend compilation
  ```bash
  java -version  # Should show 21 or higher
  ```

- **Node.js 18+** - Frontend build
  ```bash
  node -v  # Should show v18.x.x or higher
  ```

- **npm** - Package management
  ```bash
  npm -v
  ```

- **Docker Desktop** - Infrastructure services
  ```bash
  docker ps  # Should not error
  ```

### Installation:
If you're missing any prerequisites:

**macOS:**
```bash
# Java 21
brew install openjdk@21

# Node.js 18+
brew install node@18

# Docker Desktop
# Download from: https://www.docker.com/products/docker-desktop
```

**Linux:**
```bash
# Java 21
sudo apt install openjdk-21-jdk

# Node.js 18+
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Docker
sudo apt install docker.io docker-compose
```

---

## Build Process Details

### Backend Build Steps

1. **Clean**
   ```bash
   ./gradlew clean
   ```
   Removes previous build artifacts

2. **Format Code**
   ```bash
   ./gradlew spotlessApply
   ```
   Applies Google Java Format

3. **Compile**
   ```bash
   ./gradlew compileJava
   ```
   Compiles all Java source files

4. **Build JAR**
   ```bash
   ./gradlew build -x test
   ```
   Creates executable JAR (skipping tests)

5. **Output**
   - Location: `backend/build/libs/backend-0.0.1-SNAPSHOT.jar`
   - Size: ~80-100 MB

### Frontend Build Steps

1. **Install Dependencies**
   ```bash
   npm install
   ```
   Installs all npm packages (only if needed)

2. **Create Environment**
   ```bash
   echo "NEXT_PUBLIC_API_BASE_URL=http://localhost:8080" > .env.local
   ```
   Sets API endpoint

3. **Lint**
   ```bash
   npm run lint
   ```
   Checks code style

4. **Type Check**
   ```bash
   npm run type-check
   ```
   Validates TypeScript types

5. **Build**
   ```bash
   npm run build
   ```
   Creates optimized production build

6. **Output**
   - Location: `frontend/.next/`
   - Ready for production deployment

---

## Docker Services

Both scripts start these services automatically:

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache/Sessions |
| MinIO | 9000 (API)<br>9001 (Console) | Object Storage |
| Kafka | 9092 | Event Streaming |

### Verify Services:
```bash
docker ps
```

Should show 4 running containers with names:
- `docshare-postgres`
- `docshare-redis`
- `docshare-minio`
- `docshare-kafka`

### Stop Services:
```bash
cd infra
docker-compose down
```

### Reset Everything:
```bash
cd infra
docker-compose down -v  # Deletes all data!
```

---

## Troubleshooting

### "Java not found"
```bash
# Install Java 21
brew install openjdk@21

# Add to PATH (add to ~/.zshrc or ~/.bashrc)
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
```

### "Docker is not running"
```bash
# Start Docker Desktop
open -a Docker

# Wait 15-30 seconds, then retry
```

### "Port 8080 already in use"
```bash
# Find and kill the process
lsof -ti:8080 | xargs kill -9
```

### "Backend build failed"
```bash
# Try cleaning everything
cd backend
./gradlew clean
rm -rf build
./gradlew build -x test
```

### "Frontend build failed"
```bash
# Remove node_modules and rebuild
cd frontend
rm -rf node_modules .next
npm install
npm run build
```

### "Permission denied"
```bash
# Make scripts executable
chmod +x build-all.sh quick-build.sh
```

---

## CI/CD Integration

### GitHub Actions Example:
```yaml
name: Build
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build All
        run: ./quick-build.sh
```

### GitLab CI Example:
```yaml
build:
  script:
    - ./quick-build.sh
  artifacts:
    paths:
      - backend/build/libs/*.jar
      - frontend/.next/
```

---

## Manual Build (Without Scripts)

If you prefer to build manually:

### Backend:
```bash
cd backend
./gradlew clean build -x test
```

### Frontend:
```bash
cd frontend
npm install
npm run build
```

### Start Services:
```bash
cd infra
docker-compose up -d postgres redis minio kafka
```

---

## After Building

Once the build completes successfully:

### Start Backend:
```bash
cd backend
./gradlew bootRun
```

Wait for: `Started BackendApplication in X seconds`

### Start Frontend:
```bash
cd frontend
npm run dev
```

### Access Application:
- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080
- **MinIO Console:** http://localhost:9001 (user: docshare, pass: docshare123)

---

## Build Artifacts

After successful build:

```
backend/
├── build/
│   ├── libs/
│   │   └── backend-0.0.1-SNAPSHOT.jar  ← Executable JAR
│   └── classes/                        ← Compiled classes

frontend/
├── .next/                              ← Production build
│   ├── static/
│   └── server/
└── .env.local                          ← Environment config
```

---

## Performance

### Build Times (Approximate):

**Full Build (build-all.sh):**
- First time: 3-5 minutes
- Subsequent: 1-2 minutes

**Quick Build (quick-build.sh):**
- First time: 2-3 minutes
- Subsequent: 30-60 seconds

### Optimizations:

**Speed up Gradle:**
Add to `~/.gradle/gradle.properties`:
```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

**Speed up npm:**
Use a faster package manager:
```bash
npm install -g pnpm
cd frontend
pnpm install
pnpm build
```

---

## Summary

Choose the right script for your needs:

| Scenario | Use This |
|----------|----------|
| First time setup | `./build-all.sh` |
| Before deployment | `./build-all.sh` |
| Daily development | `./quick-build.sh` |
| CI/CD pipeline | `./quick-build.sh` |
| Debugging build issues | `./build-all.sh` |
| Quick rebuild | `./quick-build.sh` |

Both scripts ensure:
- ✅ All dependencies are installed
- ✅ Code is formatted correctly
- ✅ Everything compiles without errors
- ✅ Docker services are running
- ✅ Build artifacts are created

**Happy building! 🚀**
