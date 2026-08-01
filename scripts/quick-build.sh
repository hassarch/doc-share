#!/bin/bash

# Quick Build Script - No prompts, just build everything
set -e

echo "🚀 Quick Build - Building everything..."

# Start Docker services if needed
echo "📦 Starting Docker services..."
cd infra
docker-compose up -d postgres redis minio kafka 2>/dev/null || true
cd ..

echo "⏳ Waiting for services to start..."
sleep 10

# Build backend
echo "☕ Building backend..."
cd backend
./gradlew clean build -x test --quiet
echo "✓ Backend build successful"
cd ..

# Build frontend
echo "⚛️  Building frontend..."
cd frontend
[ ! -f .env.local ] && echo "NEXT_PUBLIC_API_BASE_URL=http://localhost:8080" > .env.local
npm install --silent 2>/dev/null || true
npm run build --silent
echo "✓ Frontend build successful"
cd ..

echo ""
echo "✅ All builds completed successfully!"
echo ""
echo "To start the app:"
echo "  Backend:  cd backend && ./gradlew bootRun"
echo "  Frontend: cd frontend && npm run dev"
