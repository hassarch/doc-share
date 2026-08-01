# Deployment Verification Checklist

## Quick Start

```bash
cd infra
cp .env.prod.example .env.prod
# Edit .env.prod with real values (see below)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

## Environment Variables to Fill

Edit `infra/.env.prod` with these values:

```bash
# Database
DB_PASSWORD=your-secure-password-here

# MinIO (Object Storage)
MINIO_ACCESS_KEY=your-minio-access-key
MINIO_SECRET_KEY=your-minio-secret-at-least-8-chars

# JWT (Generate with: openssl rand -base64 48)
JWT_SECRET=your-48-char-base64-secret-here

# Networking (adjust for your environment)
FRONTEND_ORIGIN=http://localhost:3000
BACKEND_PUBLIC_URL=http://localhost:8080
```

## Verification Steps

### 1. Build & Start

```bash
cd infra
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Expected output: All containers starting

### 2. Check Container Health

```bash
docker compose -f docker-compose.prod.yml ps
```

Expected output:
```
NAME                  STATUS
infra-backend-1       Up (healthy)
infra-frontend-1      Up (healthy)
infra-postgres-1      Up (healthy)
infra-redis-1         Up (healthy)
infra-minio-1         Up (healthy)
infra-kafka-1         Up (healthy)
```

Wait 30-60 seconds if containers show "starting" - health checks need time to pass.

### 3. Backend Health Check

```bash
curl http://localhost:8080/actuator/health
```

Expected output:
```json
{"status":"UP"}
```

Or with details (if logged in as admin):
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### 4. Frontend Loads

```bash
open http://localhost:3000
# Or: curl -I http://localhost:3000
```

Expected: Browser shows the login/register page

### 5. Full Authentication Flow

1. Open http://localhost:3000
2. Click "Sign up" → Register new account
3. Should redirect to dashboard after registration
4. Should see sidebar, topbar, "My Documents" section

### 6. File Upload

1. Click "Upload" button
2. Select a test file (PDF, image, anything < 100MB)
3. Upload should succeed
4. File should appear in document list
5. Click file → should download successfully

### 7. Sharing Features

1. Click a document → "Share" button
2. Enter an email → Share with user
3. Should see user in permissions list
4. Click "Create Link" → Generate share link
5. Copy link, open in incognito/private window
6. Should be able to access without login (if public link)

### 8. WebSocket Connection

1. Open browser DevTools → Network → WS tab
2. Navigate to dashboard
3. Should see WebSocket connection to `/ws`
4. Status: 101 Switching Protocols
5. Connection should stay open (not repeatedly reconnecting)

### 9. CORS Check

1. Open browser DevTools → Console
2. Navigate through app (dashboard, upload, share)
3. Should see NO CORS errors
4. All API calls should succeed

### 10. Logs Check

```bash
# Backend logs
docker compose -f docker-compose.prod.yml logs backend --tail=50

# Frontend logs
docker compose -f docker-compose.prod.yml logs frontend --tail=50
```

Expected:
- Backend: INFO-level logs, no ERRORs
- Frontend: Normal Next.js output, no crash loops
- No repeated connection failures

## Tear Down

```bash
# Stop containers, keep data
docker compose -f docker-compose.prod.yml --env-file .env.prod down

# Stop containers AND wipe all data
docker compose -f docker-compose.prod.yml --env-file .env.prod down -v
```

## Troubleshooting

### Backend won't start: "JWT secret must be at least 256 bits"

**Fix:** Generate a longer secret
```bash
openssl rand -base64 48
```
Copy output to `JWT_SECRET` in `.env.prod`

### Frontend shows 404 for API calls

**Check 1:** Is `BACKEND_PUBLIC_URL` correct?
```bash
# In .env.prod, should match how browser reaches backend
BACKEND_PUBLIC_URL=http://localhost:8080  # local
# OR
BACKEND_PUBLIC_URL=https://api.yourdomain.com  # deployed
```

**Check 2:** Is backend healthy?
```bash
curl http://localhost:8080/actuator/health
```

### CORS errors in browser console

**Check:** Does `FRONTEND_ORIGIN` match browser URL?
```bash
# .env.prod
FRONTEND_ORIGIN=http://localhost:3000  # must match browser exactly

# NOT: http://localhost:3000/
# NOT: http://127.0.0.1:3000
```

**Verify backend received it:**
```bash
docker compose -f docker-compose.prod.yml logs backend | grep CORS
```

### WebSocket won't connect

**Cause:** Same as CORS - origin mismatch

**Fix:** Ensure `FRONTEND_ORIGIN` in `.env.prod` matches browser origin exactly

### MinIO: "Access Denied" on upload

**Check:** Are credentials correct?
```bash
# .env.prod
MINIO_ACCESS_KEY=docshare
MINIO_SECRET_KEY=docshare123  # must be at least 8 characters
```

**Verify MinIO is healthy:**
```bash
docker compose -f docker-compose.prod.yml ps minio
# Should show "healthy"
```

### Database connection failed

**Check 1:** Is Postgres healthy?
```bash
docker compose -f docker-compose.prod.yml ps postgres
```

**Check 2:** Is password correct?
```bash
# .env.prod - same password in both places
DB_PASSWORD=your-password
```

**Check 3:** Logs
```bash
docker compose -f docker-compose.prod.yml logs postgres
docker compose -f docker-compose.prod.yml logs backend | grep -i database
```

### Gradle wrapper not found during Docker build

**Symptom:**
```
./gradlew: not found
```

**Fix:**
```bash
cd backend
gradle wrapper
git add gradle/
git commit -m "chore: add gradle wrapper files"
```

### Frontend build fails: can't reach Google Fonts

**Symptom:** Build fails with network error for `fonts.googleapis.com`

**Cause:** Network restriction (sandbox, firewall)

**Note:** Not a code issue - will build fine with normal internet access. If persistent in your environment, consider:
1. Using a font proxy
2. Self-hosting fonts
3. Disabling Google Fonts temporarily

## Success Criteria

- [ ] All 6 containers show "healthy" status
- [ ] Backend `/actuator/health` returns `{"status":"UP"}`
- [ ] Frontend loads at http://localhost:3000
- [ ] Can register new account
- [ ] Can login with registered account
- [ ] Dashboard loads with sidebar/topbar
- [ ] Can upload a file
- [ ] Can download the uploaded file
- [ ] Can share document with another user email
- [ ] Can create a public share link
- [ ] Can access share link in incognito/private window
- [ ] WebSocket connects (no errors in DevTools)
- [ ] No CORS errors in browser console
- [ ] Backend logs show INFO level (not DEBUG)
- [ ] No repeated errors/crashes in logs

## CI/CD Verification

After merging to `main`:

### Check GitHub Actions

1. Go to your repo → Actions tab
2. Backend CI workflow should:
   - Run on push to main
   - Pass lint + unit tests
   - Pass integration tests
   - Build and push Docker image
3. Frontend CI workflow should:
   - Run on push to main
   - Pass typecheck + lint + build
   - Build and push Docker image

### Check GitHub Container Registry

1. Go to your repo → Packages
2. Should see:
   - `docshare-backend` package
   - `docshare-frontend` package
3. Each should have:
   - `:latest` tag
   - `:sha` tag (commit SHA)

### Pull and Test Images

```bash
# Pull the built images
docker pull ghcr.io/<your-username>/docshare-backend:latest
docker pull ghcr.io/<your-username>/docshare-frontend:latest

# Update docker-compose.prod.yml to use pulled images instead of building
# (comment out `build:` sections, add `image:` lines)

# Run with pulled images
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

Expected: Everything works identically to locally-built images

## Common Issues Summary

| Issue | Symptom | Fix |
|-------|---------|-----|
| JWT too short | Backend won't start | `openssl rand -base64 48` |
| CORS errors | API calls fail in browser | Fix `FRONTEND_ORIGIN` in `.env.prod` |
| WebSocket fails | Real-time updates don't work | Same as CORS fix |
| 404 for APIs | Frontend can't reach backend | Check `BACKEND_PUBLIC_URL` |
| MinIO denied | Upload fails | Check `MINIO_SECRET_KEY` is 8+ chars |
| DB connection fail | Backend crash loop | Check `DB_PASSWORD` matches |

## Next Steps After Verification

Once all checks pass:

1. **Document your deployment:**
   - Add your domain configuration
   - Document any reverse proxy setup (nginx/Traefik)
   - Document SSL/TLS certificate setup

2. **Set up monitoring:**
   - Prometheus scrapes `/actuator/prometheus`
   - Grafana dashboards in `infra/observability/`
   - Alerting for container health failures

3. **Configure backups:**
   - Postgres: `pg_dump` scheduled backups
   - MinIO: S3 replication or backup jobs
   - Redis: RDB snapshots (refresh tokens, sessions)

4. **Secrets rotation:**
   - Replace `.env.prod` with proper secrets manager
   - AWS Secrets Manager, Vault, Doppler, etc.
   - Rotate JWT secret, DB password regularly

5. **Scale testing:**
   - Test with multiple backend instances
   - Test with load balancer
   - Verify session stickiness not required (stateless JWT design)

6. **Production checklist:**
   - Enable HTTPS (Let's Encrypt, ACM, etc.)
   - Configure domain names
   - Set up CDN for frontend (optional)
   - Configure log aggregation (ELK, CloudWatch, etc.)
   - Set up error tracking (Sentry, Rollbar, etc.)
