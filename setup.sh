#!/bin/bash

# Ham Radio Logbook - Initial Setup Script
# This script helps configure the application for first-time setup

set -e

echo "========================================="
echo "Ham Radio Logbook - Initial Setup"
echo "========================================="
echo ""

# Color codes for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_info() {
    echo -e "${NC}→${NC} $1"
}

# Check if Java is installed
echo "Checking prerequisites..."
echo ""

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        print_success "Java $JAVA_VERSION detected"
    else
        print_warning "Java $JAVA_VERSION detected, but Java 17+ is recommended"
        echo "   Install Java 21+ from: https://adoptium.net/"
    fi
else
    print_error "Java is not installed"
    echo "   Please install Java 21+ from: https://adoptium.net/"
    exit 1
fi

# Check Node.js
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$NODE_VERSION" -ge 20 ]; then
        print_success "Node.js v$NODE_VERSION detected"
    else
        print_warning "Node.js v$NODE_VERSION detected, but v20+ is recommended"
        echo "   Install Node.js 24+ from: https://nodejs.org/"
    fi
else
    print_error "Node.js is not installed"
    echo "   Please install Node.js 24+ from: https://nodejs.org/"
    exit 1
fi

# Check Maven
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn --version | head -n1 | awk '{print $3}')
    print_success "Maven $MVN_VERSION detected"
else
    print_warning "Maven is not installed (optional for manual builds)"
    echo "   Install from: https://maven.apache.org/"
fi

# Check Docker
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version | awk '{print $3}' | sed 's/,//')
    print_success "Docker $DOCKER_VERSION detected"
else
    print_warning "Docker is not installed (optional, but recommended)"
    echo "   Install from: https://www.docker.com/products/docker-desktop"
fi

echo ""
echo "========================================="
echo "Setting up configuration files..."
echo "========================================="
echo ""

# Set up Java version
if [ ! -f ".java-version" ]; then
    echo "25" > .java-version
    print_success "Created .java-version file"
else
    print_info ".java-version already exists"
fi

# Set up backend .env if it doesn't exist
if [ ! -f "backend/.env" ]; then
    if [ -f ".env.example" ]; then
        cp .env.example backend/.env
        print_success "Created backend/.env from template"
        print_warning "Please edit backend/.env to configure your settings"
    else
        print_error "Template file .env.example not found"
    fi
else
    print_info "backend/.env already exists"
fi

# Set up frontend environment files
if [ ! -f "frontend/logbook-ui/src/environments/environment.prod.ts" ]; then
    cat > frontend/logbook-ui/src/environments/environment.prod.ts << 'EOF'
export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080/api'
};
EOF
    print_success "Created frontend production environment file"
else
    print_info "Frontend environment.prod.ts already exists"
fi

echo ""
echo "========================================="
echo "Installing dependencies..."
echo "========================================="
echo ""

# Install frontend dependencies
if [ -d "frontend/logbook-ui" ]; then
    print_info "Installing frontend dependencies..."
    cd frontend/logbook-ui
    npm install
    cd ../..
    print_success "Frontend dependencies installed"
fi

echo ""
echo "========================================="
echo "Setup Complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo ""
echo "1. Configure your environment:"
echo "   → Edit backend/.env with your settings"
echo "   → Set database type (SQLite for local, PostgreSQL for production)"
echo "   → Configure admin credentials"
echo ""
echo "2. Choose how to run the application:"
echo ""
echo "   Option A: Docker (Recommended - Easiest)"
echo "   → docker-compose up -d"
echo "   → Access at http://localhost"
echo ""
echo "   Option B: Local Development"
echo "   → Terminal 1: cd backend && mvn spring-boot:run"
echo "   → Terminal 2: cd frontend/logbook-ui && npm start"
echo "   → Access at http://localhost:4200"
echo ""
echo "   Option C: Field Deployment (SQLite, no PostgreSQL)"
echo "   → docker-compose -f docker-compose.field.yml up -d"
echo "   → Access at http://localhost"
echo ""
echo "3. For detailed instructions, see:"
echo "   → QUICKSTART.md - Quick start guide"
echo "   → SETUP.md - Comprehensive setup guide"
echo "   → README.md - Full documentation"
echo ""
print_success "Happy logging! 73!"
