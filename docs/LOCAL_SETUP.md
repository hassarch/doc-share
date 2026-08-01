# Your Local Setup

## ✅ Database Configuration

You're using **local PostgreSQL** (Homebrew installation, version 16.11) instead of Docker.

### Connection Details
```
Host:     localhost
Port:     5432
Database: docshare
User:     docshare
Password: docshare123
```

### Database Status
```bash
# Check if PostgreSQL is running
lsof -i :5432

# Connect to database
psql -h localhost -U docshare -d docshare

# Check database size
psql -h localhost -U docshare -d docshare -c "\l+ docshare"
```

## 🚀 Starting the Application

### 1. PostgreSQL (Already Running)
Your local PostgreSQL is already running via Homebrew. No action needed!

```bash
# If you need to restart it:
brew services restart postgresql@16
```

### 2. Backend
```bash
cd backend
./gradlew bootRun
```

Wait for: `Started BackendApplication in X.XXX seconds`

### 3. Frontend
```bash
cd frontend
npm run dev
```

Open: http://localhost:3000

## 🛑 Stopping the Application

### Backend
Press `Ctrl+C` in the backend terminal

### Frontend
Press `Ctrl+C` in the frontend terminal

### PostgreSQL
```bash
# Stop PostgreSQL (optional, but will affect other projects)
brew services stop postgresql@16

# Or keep it running - it won't hurt anything
```

## 🔄 Reset Database

If you need to start fresh:

```bash
# Drop and recreate database
psql postgres -c "DROP DATABASE IF EXISTS docshare;"
psql postgres -c "CREATE DATABASE docshare OWNER docshare;"

# Restart backend (migrations will run automatically)
cd backend
./gradlew bootRun
```

## 📊 Database Management

### View tables
```bash
psql -h localhost -U docshare -d docshare -c "\dt"
```

### View data
```bash
# Users
psql -h localhost -U docshare -d docshare -c "SELECT id, email, name FROM users;"

# Documents
psql -h localhost -U docshare -d docshare -c "SELECT id, filename, size_bytes FROM documents;"

# Folders
psql -h localhost -U docshare -d docshare -c "SELECT id, name, parent_folder_id FROM folders;"
```

### Backup database
```bash
pg_dump -h localhost -U docshare docshare > docshare_backup.sql
```

### Restore database
```bash
psql -h localhost -U docshare docshare < docshare_backup.sql
```

## 🐳 Docker Alternative (If Needed)

If you want to use Docker instead of local PostgreSQL:

```bash
# 1. Stop local PostgreSQL
brew services stop postgresql@16

# 2. Start Docker PostgreSQL
docker run -d \
  --name docshare-db \
  -e POSTGRES_DB=docshare \
  -e POSTGRES_USER=docshare \
  -e POSTGRES_PASSWORD=docshare123 \
  -p 5432:5432 \
  postgres:17

# 3. Verify it's running
docker ps | grep docshare-db

# No backend config changes needed - same connection details!
```

## 🔍 Troubleshooting

### "Connection refused"
```bash
# Check if PostgreSQL is running
brew services list | grep postgresql

# Start if needed
brew services start postgresql@16
```

### "Database doesn't exist"
```bash
# Recreate database
psql postgres -c "CREATE DATABASE docshare OWNER docshare;"
```

### "Role doesn't exist"
```bash
# Recreate user
psql postgres -c "CREATE USER docshare WITH PASSWORD 'docshare123';"
psql postgres -c "ALTER DATABASE docshare OWNER TO docshare;"
```

### "Too many connections"
```bash
# View active connections
psql postgres -c "SELECT * FROM pg_stat_activity WHERE datname='docshare';"

# Kill all connections (restart backend after)
psql postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='docshare';"
```

### Backend won't start - port 8080 in use
```bash
# Find what's using port 8080
lsof -i :8080

# Kill it (replace PID)
kill -9 <PID>
```

### Frontend won't start - port 3000 in use
```bash
# Find what's using port 3000
lsof -i :3000

# Kill it
kill -9 <PID>
```

## ⚙️ Configuration Files

Your backend will automatically connect using these settings from `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/docshare
    username: docshare
    password: docshare123
```

No changes needed! The defaults match your local setup.

## 📝 Quick Commands

```bash
# Full restart
brew services restart postgresql@16
cd backend && ./gradlew bootRun
# (in new terminal) cd frontend && npm run dev

# Check everything is running
lsof -i :5432  # PostgreSQL
lsof -i :8080  # Backend
lsof -i :3000  # Frontend

# View logs
tail -f backend/logs/spring.log  # Backend logs
# Frontend logs are in terminal where you ran npm run dev
```

---

**Setup Type**: Local PostgreSQL (Homebrew)  
**PostgreSQL Version**: 16.11  
**Status**: ✅ Ready to use  
**Docker**: Not needed (but available as alternative)
