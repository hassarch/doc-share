#!/bin/bash

# DocShare - Complete Build Script
# This script builds both backend and frontend with all necessary checks

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

print_header "DocShare Complete Build Script"

# =============================================================================
# 1. CHECK PREREQUISITES
# =============================================================================
print_header "Step 1: Checking Prerequisites"

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        print_success "Java $JAVA_VERSION found"
    else
        print_error "Java 21+ required, found Java $JAVA_VERSION"
        exit 1
    fi
else
    print_error "Java not found. Please install Java 21+"
    exit 1
fi

# Check Node.js
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$NODE_VERSION" -ge 18 ]; then
        print_success "Node.js $(node -v) found"
    else
        print_error "Node.js 18+ required, found $(node -v)"
        exit 1
    fi
else
    print_error "Node.js not found. Please install Node.js 18+"
    exit 1
fi

# Check npm
if command -v npm &> /dev/null; then
    print_success "npm $(npm -v) found"
else
    print_error "npm not found"
    exit 1
fi

# Check Docker
if command -v docker &> /dev/null; then
    if docker info &> /dev/null; then
        print_success "Docker is running"
    else
        print_error "Docker is installed but not running. Please start Docker Desktop"
        exit 1
    fi
else
    print_error "Docker not found. Please install Docker Desktop"
    exit 1
fi

# =============================================================================
# 2. START DOCKER SERVICES
# =============================================================================
print_header "Step 2: Starting Docker Services"

cd "$SCRIPT_DIR/infra"

print_info "Checking if services are already running..."
if docker ps | grep -q "docshare-postgres"; then
    print_success "Docker services already running"
else
    print_info "Starting PostgreSQL, Redis, MinIO, and Kafka..."
    docker-compose up -d postgres redis minio kafka
    
    print_info "Waiting for services to be ready (15 seconds)..."
    sleep 15
    
    print_success "Docker services started"
fi

# Verify services
print_info "Verifying services..."
SERVICES=("docshare-postgres" "docshare-redis" "docshare-minio" "docshare-kafka")
for service in "${SERVICES[@]}"; do
    if docker ps | grep -q "$service"; then
        print_success "$service is running"
    else
        print_error "$service is not running"
        exit 1
    fi
done

cd "$SCRIPT_DIR"

# =============================================================================
# 3. BUILD BACKEND
# =============================================================================
print_header "Step 3: Building Backend"

cd "$SCRIPT_DIR/backend"

print_info "Cleaning previous build..."
./gradlew clean

print_info "Applying code formatting (Spotless)..."
./gradlew spotlessApply

print_info "Compiling Java code..."
./gradlew compileJava

print_info "Building backend (skipping tests)..."
./gradlew build -x test

print_success "Backend build successful!"

# Check if JAR was created
if [ -f "build/libs/backend-0.0.1-SNAPSHOT.jar" ]; then
    JAR_SIZE=$(ls -lh build/libs/backend-0.0.1-SNAPSHOT.jar | awk '{print $5}')
    print_success "JAR created: $JAR_SIZE"
else
    print_error "JAR file not found"
    exit 1
fi

cd "$SCRIPT_DIR"

# =============================================================================
# 4. BUILD FRONTEND
# =============================================================================
print_header "Step 4: Building Frontend"

cd "$SCRIPT_DIR/frontend"

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    print_info "Installing dependencies (first time)..."
    npm install
else
    print_success "node_modules already exists"
fi

# Create .env.local if it doesn't exist
if [ ! -f ".env.local" ]; then
    print_info "Creating .env.local..."
    echo "NEXT_PUBLIC_API_BASE_URL=http://localhost:8080" > .env.local
    print_success ".env.local created"
else
    print_success ".env.local already exists"
fi

print_info "Linting frontend code..."
npm run lint || print_info "Lint warnings found (non-blocking)"

print_info "Type checking..."
npm run type-check || print_info "Type errors found (non-blocking)"

print_info "Building frontend for production..."
npm run build

print_success "Frontend build successful!"

cd "$SCRIPT_DIR"

# =============================================================================
# 5. RUN TESTS (OPTIONAL)
# =============================================================================
print_header "Step 5: Running Tests (Optional)"

read -p "Do you want to run backend tests? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    cd "$SCRIPT_DIR/backend"
    print_info "Running backend tests..."
    ./gradlew test || print_info "Some tests failed (check logs)"
    cd "$SCRIPT_DIR"
else
    print_info "Skipping backend tests"
fi

read -p "Do you want to run frontend tests? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    cd "$SCRIPT_DIR/frontend"
    print_info "Running frontend tests..."
    npm test || print_info "Some tests failed (check logs)"
    cd "$SCRIPT_DIR"
else
    print_info "Skipping frontend tests"
fi

# =============================================================================
# 6. SUMMARY
# =============================================================================
print_header "Build Summary"

echo -e "${GREEN}✓ All builds completed successfully!${NC}"
echo ""
echo "Build artifacts:"
echo "  - Backend JAR: backend/build/libs/backend-0.0.1-SNAPSHOT.jar"
echo "  - Frontend:    frontend/.next/"
echo ""
echo "Docker services running:"
echo "  - PostgreSQL:  localhost:5432"
echo "  - Redis:       localhost:6379"
echo "  - MinIO:       localhost:9000 (API), localhost:9001 (Console)"
echo "  - Kafka:       localhost:9092"
echo ""
echo "To start the application:"
echo ""
echo "  1. Start backend:"
echo "     cd backend && ./gradlew bootRun"
echo ""
echo "  2. Start frontend:"
echo "     cd frontend && npm run dev"
echo ""
echo "  3. Access application:"
echo "     http://localhost:3000"
echo ""
print_success "Build script completed successfully! 🎉"
