#!/bin/bash

# Start Application Script - Runs backend and frontend together
# This script starts both services in separate terminal tabs/windows

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo -e "${BLUE}Starting DocShare Application...${NC}"
echo ""

# Check if Docker services are running
if ! docker ps | grep -q "docshare-postgres"; then
    echo -e "${GREEN}Starting Docker services...${NC}"
    cd "$SCRIPT_DIR/infra"
    docker-compose up -d postgres redis minio kafka
    echo "Waiting for services to start (15 seconds)..."
    sleep 15
    cd "$SCRIPT_DIR"
fi

echo -e "${GREEN}✓ Docker services running${NC}"
echo ""

# Detect OS and open terminals accordingly
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS - Use Terminal app
    echo "Starting backend in new terminal..."
    osascript <<EOF
tell application "Terminal"
    do script "cd '$SCRIPT_DIR/backend' && echo 'Starting Backend...' && ./gradlew bootRun"
    activate
end tell
EOF
    
    sleep 2
    
    echo "Starting frontend in new terminal..."
    osascript <<EOF
tell application "Terminal"
    do script "cd '$SCRIPT_DIR/frontend' && echo 'Starting Frontend...' && npm run dev"
    activate
end tell
EOF
    
    echo ""
    echo -e "${GREEN}✓ Backend and Frontend started in separate terminals${NC}"
    echo ""
    echo "Application will be available at:"
    echo "  Frontend: http://localhost:3000"
    echo "  Backend:  http://localhost:8080"
    echo ""
    echo "To stop: Press Ctrl+C in each terminal window"
    
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux - Use gnome-terminal or xterm
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal -- bash -c "cd '$SCRIPT_DIR/backend' && ./gradlew bootRun; exec bash"
        gnome-terminal -- bash -c "cd '$SCRIPT_DIR/frontend' && npm run dev; exec bash"
    elif command -v xterm &> /dev/null; then
        xterm -e "cd '$SCRIPT_DIR/backend' && ./gradlew bootRun" &
        xterm -e "cd '$SCRIPT_DIR/frontend' && npm run dev" &
    else
        echo "Please start manually:"
        echo "  Terminal 1: cd backend && ./gradlew bootRun"
        echo "  Terminal 2: cd frontend && npm run dev"
    fi
else
    echo "Please start manually in separate terminals:"
    echo "  Terminal 1: cd backend && ./gradlew bootRun"
    echo "  Terminal 2: cd frontend && npm run dev"
fi
