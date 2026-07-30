# DocShare - Quick Start Guide

Get the distributed document sharing platform running in 5 minutes.

## Prerequisites

- **Java 23+** (backend)
- **Node.js 18+** (frontend)
- **Docker** (for PostgreSQL)
- **Git**

## Step 1: Clone & Navigate

```bash
cd /Users/donut/Desktop/Dev/docshare
```

## Step 2: Start PostgreSQL

```bash
docker run -d \
  --name docshare-db \
  -e POSTGRES_DB=docshare \
  -e POSTGRES_USER=docshare \
  -e POSTGRES_PASSWORD=docshare123 \
  -p 5432:5432 \
  postgres:17
```

Verify it's running:
```bash
docker ps | grep docshare-db
```

## Step 3: Start Backend

```bash
cd backend
./gradlew bootRun
```

Wait for: `Started BackendApplication in X.XXX seconds`

Backend will be at: **http://localhost:8080**

## Step 4: Start Frontend (New Terminal)

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Create environment file
echo "NEXT_PUBLIC_API_BASE_URL=http://localhost:8080" > .env.local

# Start dev server
npm run dev
```

Frontend will be at: **http://localhost:3000**

## Step 5: Test the Application

### A. Register & Login
1. Open http://localhost:3000
2. Click **"Create one"** (register link)
3. Fill in:
   - Name: `Test User`
   - Email: `test@example.com`
   - Password: `password123`
4. Click **"Create account"**
5. You'll auto-login and land on the dashboard

### B. Upload a File
1. Click **"My Documents"** in sidebar
2. Click **"Upload"** button
3. Select any file from your computer
4. File appears in the list instantly

### C. Create a Folder
1. Click **"New Folder"**
2. Name it `Work Docs`
3. Click **"Create"**
4. Click on the folder to open it

### D. Share a File
1. Hover over a file row
2. Click **"•••"** (more actions)
3. Click **"Share"**
4. **Direct share tab**:
   - Enter email: `colleague@example.com`
   - Select role: `Viewer (read only)`
   - Click **"Share"**
5. **Share link tab**:
   - Set expiry: `7` days
   - Add password: `secret123`
   - Check **"Read-only"**
   - Click **"Create link"**
   - Click **"Copy"** icon to copy the link

### E. Test Share Link
1. Open the copied link in an **incognito window**
2. Enter password: `secret123`
3. Click **"Access file"**
4. Click **"Download"** (if not read-only)

## Verification Checklist

- [ ] Backend running on port 8080
- [ ] Frontend running on port 3000
- [ ] Can register a new user
- [ ] Can login
- [ ] Can upload a file
- [ ] Can create a folder
- [ ] Can share with another user
- [ ] Can create a share link
- [ ] Share link works in incognito mode

## Stopping the Application

```bash
# Stop frontend (Ctrl+C in terminal)

# Stop backend (Ctrl+C in terminal)

# Stop PostgreSQL
docker stop docshare-db

# Optional: Remove PostgreSQL container
docker rm docshare-db
```

## Troubleshooting

### Backend won't start
```bash
# Check if port 8080 is in use
lsof -i :8080

# Check PostgreSQL is running
docker ps | grep docshare-db

# Check database connection
docker exec -it docshare-db psql -U docshare -d docshare
```

### Frontend won't start
```bash
# Check if port 3000 is in use
lsof -i :3000

# Clear Next.js cache
rm -rf .next

# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install
```

### Can't connect to backend
```bash
# Verify .env.local exists
cat .env.local

# Should show:
# NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

# Check backend health
curl http://localhost:8080/api/v1/auth/login
# Should return 401 or 400, not 404
```

### Database issues
```bash
# Reset database
docker stop docshare-db
docker rm docshare-db

# Restart from Step 2
```

## Next Steps

- Read [`IMPLEMENTATION_COMPLETE.md`](./IMPLEMENTATION_COMPLETE.md) for full feature list
- See [`frontend/FRONTEND_README.md`](./frontend/FRONTEND_README.md) for frontend architecture
- Check [`backend/README.md`](./backend/README.md) for API documentation
- Explore Phase 1+ roadmap in the Frontend Plan

## Common Use Cases

### Test Multi-User Sharing
1. Register two users in different browsers/profiles
2. User A uploads and shares with User B
3. User B sees it in "Shared with me"

### Test Link Expiration
1. Create a share link with 0.01 days expiry (15 minutes)
2. Wait 15 minutes
3. Try to access → Should show "expired"

### Test Password Protection
1. Create share link with password
2. Try wrong password → Error
3. Try correct password → Success

### Test Read-Only Links
1. Create read-only link
2. Access link
3. Download button should be disabled

## Production Deployment

⚠️ **This is a Phase 0 prototype**. For production:

1. Move tokens to httpOnly cookies
2. Use MinIO/S3 for file storage
3. Add rate limiting
4. Add HTTPS
5. Use environment-specific secrets
6. Add monitoring (Prometheus, Grafana)
7. Deploy to Kubernetes (Phase 6)

See `IMPLEMENTATION_COMPLETE.md` → Security Considerations

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review error logs in terminal
3. Check browser console (F12)
4. See implementation docs for architecture details

---

**Happy coding!** 🚀
