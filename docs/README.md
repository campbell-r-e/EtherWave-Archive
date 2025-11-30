# Ham Radio Contest Logbook System

A comprehensive, multi-user web-based logbook application for amateur radio operators with support for contests, rig control, real-time collaboration, and advanced features.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Getting Started](#getting-started)
- [Documentation](#documentation)
- [Technology Stack](#technology-stack)

## Overview

This system provides amateur radio operators with a modern, feature-rich logging solution that supports:

- **Personal and Shared Logbooks** - Individual logs or multi-operator contest stations
- **Real-time Rig Control** - Integration with Hamlib for radio control and automatic frequency/mode detection
- **Contest Validation** - Automated validation for major contests (CQ WW, ARRL, Field Day, POTA, etc.)
- **Multi-user Collaboration** - Role-based access control for team operations
- **Advanced Analytics** - QSO maps, statistics, and visualizations
- **Standards Compliance** - ADIF 3.1.4 and Cabrillo export formats

## Key Features

### Authentication & Multi-User Support
- User registration and JWT-based authentication
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

### Export & Import
- ADIF 3.1.4 format export
- Cabrillo format for contest submissions
- Bulk import with validation
- Re-scoring on import

### Visualization
- Interactive map showing contacted stations
- State/Province boundary visualization
- QSO count heatmaps
- Statistics dashboard

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
│  - Export Panel                                              │
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
└─────────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────────┐
│                        Database                              │
│  SQLite (Development/Field) or PostgreSQL (Production)      │
│  - Multi-tenant with log_id foreign keys                    │
│  - Users, Logs, LogParticipants, Invitations                │
│  - QSOs, Stations, Operators, Contests                      │
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

## Getting Started

### Prerequisites
- **Java 25** or higher (Eclipse Adoptium Temurin 25 recommended)
- **Node.js 24** or higher
- Docker and Docker Compose (for rig control and deployment)
- SQLite (included) or PostgreSQL 16

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/campbell-r-e/Hamradiologbook.git
   cd Hamradiologbook
   ```

2. **Backend Setup**
   ```bash
   cd backend
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

3. **Frontend Setup**
   ```bash
   cd frontend/logbook-ui
   npm install
   ng serve
   ```

4. **Access the Application**
   - Frontend: http://localhost:4200
   - Backend API: http://localhost:8080/api

### Environment Configuration

Create `.env` file in backend directory:

```env
# Database
SPRING_DATASOURCE_URL=jdbc:sqlite:hamradio.db

# JWT
JWT_SECRET=YourSecretKeyHere
JWT_EXPIRATION_MS=86400000

# Admin User
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
ADMIN_EMAIL=admin@hamradio.local

# QRZ API (optional)
QRZ_API_KEY=your-qrz-api-key
```

## Documentation

Detailed documentation is available in the `/docs` folder:

- **[User Guide](USER_GUIDE.md)** - How to use the system as an operator
- **[Developer Guide](DEVELOPER_GUIDE.md)** - Architecture, APIs, and development
- **[API Reference](API_REFERENCE.md)** - Complete REST API documentation
- **[Database Schema](DATABASE_SCHEMA.md)** - Entity relationship diagrams and schema
- **[Deployment Guide](DEPLOYMENT.md)** - Production deployment instructions
- **[Contest Validation](CONTEST_VALIDATION.md)** - How to add new contest validators

## Technology Stack

### Backend
- **Java 25** - Latest JDK with cutting-edge features
- **Spring Boot 4.0.0** - Application framework
- **Spring Framework 7.0.0** - Core framework
- **Spring Security 7.x** - Authentication and authorization
- **JWT (JJWT 0.12.6)** - Stateless authentication tokens
- **Spring Data JPA** - Database ORM
- **Hibernate 7.1.8** - JPA implementation with Java 25 support
- **PostgreSQL 16 / SQLite** - Database options
- **Spring WebSocket** - Real-time communication
- **Lombok 1.18.38** - Boilerplate reduction (Java 25 compatible)
- **Jackson 3.0** - JSON processing (tools.jackson)

### Frontend
- **Angular 21.0.1** - Latest Angular framework with standalone components
- **TypeScript 5.9** - Type-safe JavaScript with strict mode
- **RxJS 7.8** - Reactive programming and state management
- **Bootstrap 5.3** - Modern UI framework with responsive design
- **Bootstrap Icons** - Icon library
- **Leaflet** - Interactive map visualization for QSO plotting

### Rig Control
- **Hamlib** - Radio control library (rigctld)
- **Docker** - Containerization for per-user instances
- **WebSocket** - Real-time frequency/mode updates

### Development Tools
- **Maven** - Java build tool
- **npm** - Node package manager
- **Angular CLI** - Angular development tools
- **Git** - Version control

## Project Structure

```
Hamradiologbook/
├── backend/                          # Spring Boot backend
│   ├── src/main/java/
│   │   └── com/hamradio/logbook/
│   │       ├── config/               # Configuration classes
│   │       ├── controller/           # REST controllers
│   │       ├── dto/                  # Data transfer objects
│   │       ├── entity/               # JPA entities
│   │       ├── repository/           # Data access layer
│   │       ├── service/              # Business logic
│   │       ├── util/                 # Utilities
│   │       └── validation/           # Contest validators
│   ├── src/main/resources/
│   │   ├── application.properties    # App configuration
│   │   └── contest-definitions/      # Contest JSON configs
│   └── pom.xml                       # Maven dependencies
│
├── frontend/logbook-ui/              # Angular frontend
│   ├── src/app/
│   │   ├── components/               # UI components
│   │   │   ├── auth/                 # Login/Register
│   │   │   ├── dashboard/            # Main dashboard
│   │   │   ├── log/                  # Log management
│   │   │   ├── qso-entry/            # QSO entry form
│   │   │   ├── qso-list/             # QSO data grid
│   │   │   ├── rig-status/           # Rig control panel
│   │   │   ├── map-visualization/    # Map display
│   │   │   ├── contest-selection/    # Contest picker
│   │   │   ├── station-management/   # Station config
│   │   │   └── export-panel/         # ADIF/Cabrillo export
│   │   ├── models/                   # TypeScript interfaces
│   │   ├── services/                 # HTTP/WebSocket services
│   │   └── guards/                   # Route guards
│   └── package.json                  # npm dependencies
│
├── rig-control-service/              # Hamlib integration service
│   ├── src/main/java/
│   │   └── com/hamradio/rigcontrol/
│   └── Dockerfile                    # Container definition
│
├── docs/                             # Documentation
│   ├── README.md                     # This file
│   ├── USER_GUIDE.md                 # User documentation
│   ├── DEVELOPER_GUIDE.md            # Developer documentation
│   ├── API_REFERENCE.md              # API documentation
│   ├── DATABASE_SCHEMA.md            # Database documentation
│   ├── DEPLOYMENT.md                 # Deployment guide
│   └── CONTEST_VALIDATION.md         # Contest plugin guide
│
└── docker-compose.yml                # Multi-container orchestration
```

## License

[Specify your license here]

## Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting pull requests.

## Support

For issues, questions, or feature requests, please open an issue on GitHub.

## Acknowledgments

- **Hamlib** - Radio control library
- **QRZ.com** - Callsign lookup API
- **ARRL** - Contest rules and specifications
- Amateur Radio community for feedback and testing
