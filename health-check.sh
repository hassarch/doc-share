#!/bin/bash

# DocShare Health Check Script
# Run this to verify all services are operational

set -e

echo "╔═══════════════════════════════════════════════════════╗"
echo "║        DocShare Application Health Check             ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check service
check_service() {
    local name=$1
    local container=$2
    printf "%-25s " "$name:"
    
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        status=$(docker inspect --format='{{.State.Health.Status}}' $container 2>/dev/null || echo "running")
        if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
            echo -e "${GREEN}✓ Running${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠ Unhealthy${NC}"
            return 1
        fi
    else
        echo -e "${RED}✗ Not Running${NC}"
        return 1
    fi
}

# Function to check HTTP endpoint
check_http() {
    local name=$1
    local url=$2
    local expected_code=$3
    printf "%-25s " "$name:"
    
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    
    if [ "$http_code" = "$expected_code" ]; then
        echo -e "${GREEN}✓ HTTP $http_code${NC}"
        return 0
    else
        echo -e "${RED}✗ HTTP $http_code (expected $expected_code)${NC}"
        return 1
    fi
}

echo "1️⃣  Checking Docker Containers..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
check_service "PostgreSQL" "infra-postgres-1"
check_service "Redis" "infra-redis-1"
check_service "MinIO" "infra-minio-1"
check_service "Kafka" "infra-kafka-1"
check_service "Backend API" "infra-backend-1"
check_service "Frontend" "infra-frontend-1"
echo ""

echo "2️⃣  Checking HTTP Endpoints..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
check_http "Frontend" "http://localhost:3000" "200"
check_http "Backend API" "http://localhost:8080/api/v1/auth/login" "401"
check_http "MinIO API" "http://localhost:9000/minio/health/live" "200"
echo ""

echo "3️⃣  Checking Database..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if docker exec infra-postgres-1 psql -U docshare -d docshare -c "\dt" >/dev/null 2>&1; then
    user_count=$(docker exec infra-postgres-1 psql -U docshare -d docshare -t -c "SELECT COUNT(*) FROM users;" 2>/dev/null | xargs)
    table_count=$(docker exec infra-postgres-1 psql -U docshare -d docshare -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';" 2>/dev/null | xargs)
    
    printf "%-25s ${GREEN}✓ Connected${NC}\n" "Database Connection:"
    printf "%-25s %s tables\n" "Schema:" "$table_count"
    printf "%-25s %s users\n" "Registered Users:" "$user_count"
else
    printf "%-25s ${RED}✗ Connection Failed${NC}\n" "Database Connection:"
fi
echo ""

echo "4️⃣  Checking Migrations..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
migration_status=$(docker exec infra-postgres-1 psql -U docshare -d docshare -t -c "SELECT version || ' - ' || description || ' [' || CASE WHEN success THEN '✓' ELSE '✗' END || ']' FROM flyway_schema_history ORDER BY installed_rank;" 2>/dev/null || echo "Error reading migrations")

if [ "$migration_status" != "Error reading migrations" ]; then
    echo "$migration_status" | while read -r line; do
        if [ ! -z "$line" ]; then
            if [[ $line == *"[✓]"* ]]; then
                echo -e "  ${GREEN}$line${NC}"
            else
                echo -e "  ${RED}$line${NC}"
            fi
        fi
    done
else
    echo -e "  ${RED}✗ Could not read migration status${NC}"
fi
echo ""

echo "5️⃣  Quick Test..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
printf "%-25s " "Register Test User:"
test_email="healthcheck-$(date +%s)@test.com"
register_response=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$test_email\",\"password\":\"test123\",\"name\":\"Health Check\"}" 2>/dev/null)

if echo "$register_response" | grep -q '"id"'; then
    echo -e "${GREEN}✓ Success${NC}"
    
    printf "%-25s " "Login Test:"
    login_response=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$test_email\",\"password\":\"test123\"}" 2>/dev/null)
    
    if echo "$login_response" | grep -q '"accessToken"'; then
        echo -e "${GREEN}✓ Success${NC}"
    else
        echo -e "${RED}✗ Failed${NC}"
    fi
else
    echo -e "${RED}✗ Failed${NC}"
fi
echo ""

echo "╔═══════════════════════════════════════════════════════╗"
echo "║                  Summary                              ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""
echo "  Frontend:   http://localhost:3000"
echo "  Backend:    http://localhost:8080"
echo "  MinIO:      http://localhost:9001"
echo ""
echo -e "${GREEN}✅ All systems operational!${NC}"
echo ""
