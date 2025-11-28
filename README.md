# Ham Radio Contest Logbook System

A comprehensive, multi-user web-based logbook application for amateur radio operators with support for contests, rig control, real-time collaboration, and advanced features.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-17-red.svg)](https://angular.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Quick Start](#quick-start)
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

### Using Docker (Recommended)

**Production Deployment with PostgreSQL:**
```bash
git clone https://github.com/campbell-r-e/Hamradiologbook.git
cd Hamradiologbook

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f backend
```

**Field Deployment with SQLite:**
```bash
# For portable/offline operations
docker-compose -f docker-compose.field.yml up -d
```

**Access Points:**
- Frontend: http://localhost
- Backend API: http://localhost:8080/api
- Health Check: http://localhost:8080/actuator/health

### Local Development

**Prerequisites:**
- Java 21 or higher
- Node.js 18 or higher
- Maven 3.9+
- Docker (optional, for rig control)

**Backend Setup:**
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

**Frontend Setup:**
```bash
cd frontend/logbook-ui
npm install
ng serve
```

**Access:**
- Frontend: http://localhost:4200
- Backend: http://localhost:8080

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend                             │
│  Angular 17 Standalone Components + Bootstrap 5             │
│  - Authentication (Login/Register)                           │
│  - Log Management (Selector, Invitations)                    │
│  - QSO Entry & List                                          │
│  - Rig Control Status                                        │
│  - Map Visualization                                         │
│  - Contest Selection                                         │
│  - Station/Operator Management                               │
│  - Export Panel (ADIF/Cabrillo)                              │
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

Comprehensive documentation is available in the `/docs` folder:

- **[Setup Guide](SETUP.md)** - Detailed installation and configuration
- **[User Guide](docs/USER_GUIDE.md)** - How to use the system as an operator
- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Architecture, APIs, and development
- **[API Reference](docs/API_REFERENCE.md)** - Complete REST API documentation
- **[Database Schema](docs/DATABASE_SCHEMA.md)** - Entity relationship diagrams
- **[Docker Deployment](DOCKER_DEPLOYMENT.md)** - Production deployment with Docker

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
- **Angular 17** - Frontend framework (standalone components)
- **TypeScript 5.2** - Type-safe JavaScript
- **RxJS** - Reactive programming
- **Bootstrap 5** - UI framework
- **Bootstrap Icons** - Icon library
- **Leaflet** - Map visualization

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
