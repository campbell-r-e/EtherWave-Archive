# Ham Radio Contest Logbook System

A comprehensive, multi-user web-based logbook application for amateur radio operators with support for contests, rig control, real-time collaboration, and advanced features.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21.0.1-red.svg)](https://angular.io/)
[![Node.js](https://img.shields.io/badge/Node.js-24-green.svg)](https://nodejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-blue.svg)](https://www.typescriptlang.org/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-purple.svg)](https://getbootstrap.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Quick Start](#quick-start)
- [Complete Setup Guide](#complete-setup-guide)
- [System Architecture](#system-architecture)
- [Documentation](#documentation)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)

## Overview

This system provides amateur radio operators with a modern, feature-rich logging solution that supports:

- **Personal and Shared Logbooks** - Individual logs or multi-operator contest stations
- **Real-time Rig Control** - Integration with Hamlib for radio control and automatic frequency/mode detection
- **Contest Validation** - Automated validation for major contests (CQ WW, ARRL, Field Day, POTA, etc.)
- **Multi-user Collaboration** - Role-based access control for team operations
- **Advanced Analytics** - QSO maps, statistics, and visualizations
- **Standards Compliance** - ADIF 3.1.4 and Cabrillo export formats
- **Production Ready** - Docker deployment with PostgreSQL or SQLite

## Key Features

### Authentication & Multi-User Support
- User registration and JWT-based authentication
- Spring Security with role-based access control
- Admin user configuration via environment variables
- Role-based access control (CREATOR, STATION, VIEWER)
- In-app invitation system for shared logs

### Logbook Management
- Personal and shared logbooks
- Log freeze functionality (manual or automatic after contest end)
- Multi-tenant architecture with log isolation
- QSO count and participant tracking

### QSO Entry & Validation
- Comprehensive QSO data capture (frequency, mode, RST, location, etc.)
- Real-time contest validation
- Duplicate checking
- Callsign lookup via QRZ.com API
- Grid square, DXCC, CQ/ITU zone support

### Rig Control
- Hamlib integration via rigctld
- Real-time frequency and mode updates
- WebSocket-based communication
- Per-user rig control containers

### Contest Support
- Plugin-based validation architecture
- Pre-configured validators for major contests:
  - CQ World Wide DX (CW/SSB)
  - ARRL Sweepstakes
  - ARRL Field Day
  - Parks on the Air (POTA)
  - Summits on the Air (SOTA)
  - State QSO Parties
- Automatic scoring and validation
- Reference data validation (parks, counties, etc.)

### Monitoring & Health Checks
- Spring Boot Actuator endpoints
- Health, liveness, and readiness probes
- Metrics and application monitoring
- Production-ready observability

## Quick Start

**🚀 New to the system? See [QUICKSTART.md](QUICKSTART.md) for a complete beginner's guide!**

### Using Docker (Recommended - 2 Minutes)

**1. Install Prerequisites:**
- [Docker Desktop](https://www.docker.com/products/docker-desktop) (Windows/Mac/Linux)

**2. Clone and Start:**
```bash
git clone https://github.com/campbell-r-e/Hamradiologbook.git
cd Hamradiologbook
docker-compose up -d
```

**3. Wait for services (~1-2 minutes):**
```bash
docker-compose ps
# All services should show "Up" or "Up (healthy)"
```

**4. Open Browser:**
- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080/api
- **Health Check**: http://localhost:8080/actuator/health

**5. Create Account & Start Logging!**
- Click "Register here"
- Create your account
- Create a logbook
- Start logging QSOs

### Field Deployment (Portable/Offline)

**For Field Day, portable operations, or offline use:**
```bash
docker-compose -f docker-compose.field.yml up -d
```

**Advantages:**
- ✅ No PostgreSQL dependency
- ✅ Single SQLite database file (easy backup)
- ✅ Perfect for offline operations
- ✅ Smaller resource footprint

### Local Development

**Prerequisites:**
- ☕ Java 21 or higher ([Download](https://adoptium.net/))
- 🟢 Node.js 24 or higher ([Download](https://nodejs.org/))
- 📦 Maven 3.9+ ([Download](https://maven.apache.org/))
- 🐳 Docker (optional, for rig control)

**Backend Setup:**
```bash
cd backend
mvn clean install    # Build (first time or after changes)
mvn spring-boot:run  # Start backend
```
Backend runs at: **http://localhost:8080**

**Frontend Setup:** (in new terminal)
```bash
cd frontend/logbook-ui
npm install          # Install dependencies (first time)
npm start            # Start dev server
```
Frontend runs at: **http://localhost:4200**

**Verify Setup:**
- ✅ Backend health: http://localhost:8080/actuator/health
- ✅ Frontend: http://localhost:4200
- ✅ Create account and start logging

## Complete Setup Guide

For detailed installation and configuration instructions, see:

- **[QUICKSTART.md](QUICKSTART.md)** - 5-minute quick start for beginners
- **[SETUP.md](SETUP.md)** - Comprehensive setup and configuration guide
- **[RIG_CONTROL_GUIDE.md](RIG_CONTROL_GUIDE.md)** - Complete rig control setup
- **[docs/USER_GUIDE.md](docs/USER_GUIDE.md)** - How to use the system
- **[docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md)** - Development guide

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend                             │
│  Angular 21.0.1 Standalone Components + Bootstrap 5.3       │
│  - TypeScript 5.9 with strict type checking                 │
│  - Reactive Forms with validation                           │
│  - Authentication (Login/Register) with JWT interceptor      │
│  - Log Management (Selector, Invitations)                    │
│  - QSO Entry & List with real-time updates                  │
│  - Rig Control Status (live frequency/mode display)         │
│  - Map Visualization (Leaflet integration)                   │
│  - Contest Selection                                         │
│  - Station/Operator Management                               │
│  - Export Panel (ADIF/Cabrillo)                              │
│  - Router with AuthGuard protection                          │
└─────────────────────────────────────────────────────────────┘
                            ↕ HTTP/WebSocket
┌─────────────────────────────────────────────────────────────┐
│                         Backend                              │
│  Spring Boot 3.2.0 + Spring Security + JWT                  │
│  - REST API Controllers                                      │
│  - Service Layer (Business Logic)                           │
│  - Permission Checking (LogService)                         │
│  - Contest Validation Engine                                │
│  - WebSocket (STOMP) for real-time updates                  │
│  - Spring Boot Actuator (Health & Metrics)                  │
└─────────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────────┐
│                        Database                              │
│  PostgreSQL 16 (Production) or SQLite (Development/Field)  │
│  - Multi-tenant with log_id foreign keys                    │
│  - Users, Logs, LogParticipants, Invitations                │
│  - QSOs, Stations, Operators, Contests                      │
│  - Callsign cache, Rig telemetry                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Rig Control Service                       │
│  Java Spring Boot Service + Hamlib rigctld                  │
│  - TCP socket communication with rigctld                    │
│  - Frequency/Mode polling                                   │
│  - WebSocket broadcast to backend                           │
│  - Per-user Docker containers                               │
└─────────────────────────────────────────────────────────────┘
```

## Documentation

Comprehensive documentation is available:

### Getting Started
- **[QUICKSTART.md](QUICKSTART.md)** - 5-minute quick start guide (START HERE!)
- **[SETUP.md](SETUP.md)** - Detailed installation and configuration
- **[RIG_CONTROL_GUIDE.md](RIG_CONTROL_GUIDE.md)** - Complete rig control setup

### Using the System
- **[User Guide](docs/USER_GUIDE.md)** - How to use the system as an operator
- **[API Reference](docs/API_REFERENCE.md)** - Complete REST API documentation

### For Developers
- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Architecture, APIs, and development
- **[Database Schema](docs/DATABASE_SCHEMA.md)** - Entity relationship diagrams

### Deployment
- **[DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)** - Production deployment with Docker

## Technology Stack

### Backend
- **Java 21** - Latest LTS version
- **Spring Boot 3.2.0** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Boot Actuator** - Production monitoring
- **JWT (JJWT 0.12.3)** - Stateless authentication tokens
- **Spring Data JPA** - Database ORM
- **Hibernate 6.3** - JPA implementation with Java 21 support
- **Lombok 1.18.34** - Boilerplate reduction (Java 21 compatible)
- **PostgreSQL 16 / SQLite** - Database options
- **Spring WebSocket** - Real-time communication

### Frontend
- **Angular 21.0.1** - Latest Angular framework with standalone components
- **TypeScript 5.9** - Type-safe JavaScript with strict mode
- **RxJS 7.8** - Reactive programming and state management
- **Bootstrap 5.3** - Modern UI framework with responsive design
- **Leaflet** - Interactive map visualization for QSO plotting
- **SockJS + STOMP** - WebSocket communication for real-time updates
- **Angular Router** - Client-side routing with route guards
- **Reactive Forms** - Form validation and data binding

### Rig Control
- **Hamlib 4** - Radio control library
- **Docker** - Containerization
- **WebSocket** - Real-time updates

### Infrastructure
- **Docker & Docker Compose** - Containerization and orchestration
- **Nginx** - Frontend web server
- **PostgreSQL 16** - Production database
- **SQLite** - Development/portable database

## Environment Configuration

### Backend Configuration

Create `.env` file or set environment variables:

```env
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/hamradio_logbook
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect

# For SQLite (field deployment)
# SPRING_DATASOURCE_URL=jdbc:sqlite:logbook.db
# SPRING_JPA_DATABASE_PLATFORM=org.hibernate.community.dialect.SQLiteDialect

# JWT Configuration
JWT_SECRET=YourSecretKeyMinimum256BitsForHS512Algorithm
JWT_EXPIRATION_MS=86400000

# Admin User (created on first startup)
ADMIN_USERNAME=admin
ADMIN_PASSWORD=SecurePassword123
ADMIN_EMAIL=admin@hamradio.local

# QRZ API (optional)
QRZ_USERNAME=your-qrz-username
QRZ_PASSWORD=your-qrz-password

# Actuator Configuration (exposed endpoints)
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics
```

### Docker Compose Variables

See `docker-compose.yml` for production setup and `docker-compose.field.yml` for portable deployment.

## Project Structure

```
Hamradiologbook/
├── backend/                          # Spring Boot backend
│   ├── src/main/java/com/hamradio/logbook/
│   │   ├── config/                   # Security, CORS, WebSocket config
│   │   ├── controller/               # REST API endpoints
│   │   ├── dto/                      # Data transfer objects
│   │   ├── entity/                   # JPA entities
│   │   ├── repository/               # Data access layer
│   │   ├── service/                  # Business logic
│   │   └── validation/               # Contest validators
│   ├── src/main/resources/
│   │   ├── application.properties    # App configuration
│   │   └── contest-definitions/      # Contest JSON configs
│   ├── Dockerfile                    # Backend container (Java 21)
│   └── pom.xml                       # Maven dependencies
│
├── frontend/logbook-ui/              # Angular 17 frontend
│   ├── src/app/
│   │   ├── components/               # UI components
│   │   ├── models/                   # TypeScript models
│   │   ├── services/                 # HTTP/WebSocket services
│   │   └── guards/                   # Auth guards
│   ├── nginx.conf                    # Nginx configuration
│   ├── Dockerfile                    # Frontend container
│   └── package.json                  # npm dependencies
│
├── rig-control-service/              # Hamlib integration
│   ├── src/main/java/com/hamradio/rigcontrol/
│   ├── Dockerfile                    # Rig control container (Java 21 + Hamlib)
│   └── pom.xml
│
├── docs/                             # Comprehensive documentation
│   ├── README.md                     # System overview
│   ├── USER_GUIDE.md                 # User manual
│   ├── DEVELOPER_GUIDE.md            # Development guide
│   ├── API_REFERENCE.md              # API docs
│   └── DATABASE_SCHEMA.md            # Database documentation
│
├── docker-compose.yml                # Production deployment (PostgreSQL)
├── docker-compose.field.yml          # Field deployment (SQLite)
├── SETUP.md                          # Setup instructions
└── README.md                         # This file
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token

### Health & Monitoring
- `GET /actuator/health` - Application health status
- `GET /actuator/info` - Application information
- `GET /actuator/metrics` - Application metrics (authenticated)

### QSO Management
- `GET /api/qsos` - List QSOs
- `POST /api/qsos` - Create new QSO
- `PUT /api/qsos/{id}` - Update QSO
- `DELETE /api/qsos/{id}` - Delete QSO

### Logs
- `GET /api/logs` - List accessible logs
- `POST /api/logs` - Create new log
- `GET /api/logs/{id}` - Get log details

See [API_REFERENCE.md](docs/API_REFERENCE.md) for complete API documentation.

## Testing

```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd frontend/logbook-ui
npm test

# Integration tests
docker-compose up -d
# Run integration test suite
```

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

[Specify your license here]

## Support

- **Issues**: https://github.com/campbell-r-e/Hamradiologbook/issues
- **Email**: support@hamradio.local
- **Documentation**: See `/docs` folder

## Acknowledgments

- **Hamlib** - Radio control library
- **QRZ.com** - Callsign lookup API
- **ARRL** - Contest rules and specifications
- **Spring Boot Team** - Excellent framework
- Amateur Radio community for feedback and testing

---

**Built with ❤️ for the Amateur Radio Community - 73!**
