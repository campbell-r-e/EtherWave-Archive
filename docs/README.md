# EtherWave Archive - Documentation Index

A comprehensive, multi-user web-based logbook application for amateur radio operators with support for contests, rig control, real-time collaboration, and advanced features.

## Quick Links

**New Users**: Start with [QUICKSTART.md](QUICKSTART.md) or [START_HERE.md](START_HERE.md)

**Operators**: See [USER_GUIDE.md](USER_GUIDE.md) for complete usage instructions

**Developers**: See [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for architecture and development

## Overview

EtherWave Archive provides amateur radio operators with a modern, feature-rich logging solution that supports:

- **Personal and Shared Logbooks** - Individual logs or multi-operator contest stations
- **Real-time Rig Control** - Integration with Hamlib for radio control and automatic frequency/mode detection
- **Contest Validation** - Automated validation for major contests (Field Day, POTA, SOTA, Winter Field Day, etc.)
- **Multi-user Collaboration** - Role-based access control for team operations
- **Advanced Analytics** - QSO maps, statistics, and visualizations
- **Standards Compliance** - ADIF 3.1.4 and Cabrillo export formats
- **Modern UI/UX** - Dark/Light theme support, accessibility features, keyboard shortcuts

## Key Features

### Authentication & Multi-User Support
- User registration and JWT-based authentication
- Admin user configuration via environment variables
- Role-based access control (CREATOR, STATION, VIEWER)
- In-app invitation system for shared logs
- Dark/Light theme toggle with system preference detection
- Theme persistence across sessions
- EtherWave Archive branding on all pages (theme-aware logos)

### Logbook Management
- Personal and shared logbooks
- Log freeze functionality (manual or automatic after contest end)
- Multi-tenant architecture with log isolation
- QSO count and participant tracking
- Station assignment system for multi-operator contests
- Numbered stations (Station 1-10) with color-coded badges
- GOTA (Get On The Air) station designation
- Station-specific QSO tracking and filtering

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

### Participant Management
- Real-time participant invitation system
- Role-based access control (CREATOR, STATION, VIEWER)
- Station assignment and reassignment
- Participant removal and permission management
- Invitation tracking (pending, accepted, declined, cancelled)
- Station callsign assignment for operators

### Export & Import
- ADIF 3.1.4 format export
- Cabrillo format for contest submissions
- Bulk import with validation
- Re-scoring on import
- Export filtering by station assignment

### Visualization
- Interactive map showing contacted stations
- State/Province boundary visualization
- QSO count heatmaps
- Statistics dashboard

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend                             │
│  Angular 21 Standalone Components + Bootstrap 5             │
│  - Authentication (Login/Register with theme-aware logos)    │
│  - Log Management (Selector, Invitations)                    │
│  - QSO Entry & List                                          │
│  - Rig Control Status                                        │
│  - Map Visualization                                         │
│  - Contest Selection                                         │
│  - Station Management (Assignment & Configuration)           │
│  - Participant Management (Invites & Permissions)            │
│  - Export Panel (ADIF/Cabrillo with station filtering)       │
│  - Import Panel (ADIF with validation)                       │
│  - Theme Toggle (Dark/Light mode)                            │
│  - Score Summary Dashboard                                   │
└─────────────────────────────────────────────────────────────┘
                            ↕ HTTP/WebSocket
┌─────────────────────────────────────────────────────────────┐
│                         Backend                              │
│  Spring Boot 4.0.3 + Spring Security + JWT                  │
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
- **Node.js 25** or higher
- Docker and Docker Compose (for rig control and deployment)
- SQLite (included) or PostgreSQL 18

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
# ADMIN_EMAIL removed — email not required

# QRZ API (optional — enables callsign lookup)
QRZ_USERNAME=your-qrz-username
QRZ_PASSWORD=your-qrz-password
```

## Documentation Structure

All documentation lives in the `docs/` directory.

### Getting Started

- **[START_HERE.md](START_HERE.md)** - Absolute beginner's first steps
- **[QUICKSTART.md](QUICKSTART.md)** - 5-minute quick start guide
- **[SETUP.md](SETUP.md)** - Comprehensive installation and configuration
- **[REGISTRATION_GUIDE.md](REGISTRATION_GUIDE.md)** - User registration walkthrough
- **[RIG_CONTROL_GUIDE.md](RIG_CONTROL_GUIDE.md)** - Complete rig control setup

### User Documentation

- **[USER_GUIDE.md](USER_GUIDE.md)** - Complete user manual for operators
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Common issues and solutions
- **[KEYBOARD_SHORTCUTS.md](KEYBOARD_SHORTCUTS.md)** - Keyboard navigation and shortcuts

### Developer Documentation

- **[DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)** - Architecture, APIs, and development guide
- **[API_REFERENCE.md](API_REFERENCE.md)** - Complete REST API documentation
- **[DATABASE_SCHEMA.md](DATABASE_SCHEMA.md)** - Entity relationship diagrams and schema
- **[TEST_STRATEGY.md](TEST_STRATEGY.md)** - Testing approach and guidelines
- **[TEST_IMPLEMENTATION_GUIDE.md](TEST_IMPLEMENTATION_GUIDE.md)** - Test implementation details
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - How to run and write tests
- **[TESTING.md](TESTING.md)** - Backend test suite details
- **[MAPS_ARCHITECTURE.md](MAPS_ARCHITECTURE.md)** - Map visualization system design
- **[IMPLEMENTATION_PROGRESS.md](IMPLEMENTATION_PROGRESS.md)** - Development progress tracking

### Configuration & Deployment

- **[CONFIGURATION.md](CONFIGURATION.md)** - System configuration options
- **[DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)** - Docker deployment guide
- **[SYSTEM_REQUIREMENTS.md](SYSTEM_REQUIREMENTS.md)** - Hardware and software requirements
- **[UPGRADE_GUIDE.md](UPGRADE_GUIDE.md)** - Version upgrade procedures

### Project Specification & Design

- **[AGILE_PRODUCT_SPECIFICATION.md](AGILE_PRODUCT_SPECIFICATION.md)** - Product requirements and specifications
- **[DATABASE_DESIGN.md](DATABASE_DESIGN.md)** - Backend database design rationale
- **[BRANDING.md](BRANDING.md)** - EtherWave Archive branding guidelines
- **[ACCESSIBILITY_REPORT.md](ACCESSIBILITY_REPORT.md)** - Accessibility compliance details
- **[ACCESSIBILITY_AUDIT_FIXES.md](ACCESSIBILITY_AUDIT_FIXES.md)** - Accessibility improvements made

### Rig Control

- **[RIG_CONTROL_INTEGRATION.md](RIG_CONTROL_INTEGRATION.md)** - Technical architecture
- **[RIG_CONTROL_QUICKSTART.md](RIG_CONTROL_QUICKSTART.md)** - Quick start guide
- **[RIG_CONTROL_INDEX.md](RIG_CONTROL_INDEX.md)** - Rig control documentation index
- **[RIG_CONTROL_USER_GUIDE.md](RIG_CONTROL_USER_GUIDE.md)** - User guide for rig control
- **[RIG_CONTROL_DEVELOPER_GUIDE.md](RIG_CONTROL_DEVELOPER_GUIDE.md)** - Developer integration guide
- **[RIG_CONTROL_API_REFERENCE.md](RIG_CONTROL_API_REFERENCE.md)** - API reference
- **[REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md)** - Rig control refactoring notes
- **[TESTING_WITHOUT_HARDWARE.md](TESTING_WITHOUT_HARDWARE.md)** - Testing rig control without hardware

## Technology Stack

### Backend
- **Java 25** - Latest JDK with cutting-edge features
- **Spring Boot 4.0.3** - Application framework
- **Spring Framework 7.0.0** - Core framework
- **Spring Security 7.x** - Authentication and authorization
- **JWT (JJWT 0.12.6)** - Stateless authentication tokens
- **Spring Data JPA** - Database ORM
- **Hibernate 7.1.8** - JPA implementation with Java 25 support
- **PostgreSQL 18 / SQLite** - Database options
- **Spring WebSocket** - Real-time communication
- **Lombok 1.18.42** - Boilerplate reduction (Java 25 compatible)
- **Jackson 3.0** - JSON processing (tools.jackson)

### Frontend
- **Angular 21.2.4** - Latest Angular framework with standalone components
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
│   │   └── contest-configs/          # Contest JSON configs
│   └── pom.xml                       # Maven dependencies
│
├── frontend/logbook-ui/              # Angular frontend
│   ├── src/app/
│   │   ├── components/               # UI components
│   │   │   ├── auth/                 # Login/Register
│   │   │   │   ├── login/            # Login component
│   │   │   │   └── register/         # Register component
│   │   │   ├── dashboard/            # Main dashboard
│   │   │   ├── log/                  # Log management
│   │   │   │   ├── log-selector/     # Log dropdown selector
│   │   │   │   └── invitations/      # Invitation management
│   │   │   ├── qso-entry/            # QSO entry form
│   │   │   ├── qso-list/             # QSO data grid
│   │   │   ├── rig-status/           # Rig control panel
│   │   │   ├── map-visualization/    # Map display
│   │   │   ├── contest-selection/    # Contest picker
│   │   │   ├── station-management/   # Station assignment & config
│   │   │   ├── participant-management/ # User invites & permissions
│   │   │   ├── score-summary/        # Contest scoring dashboard
│   │   │   ├── export-panel/         # ADIF/Cabrillo export
│   │   │   └── import-panel/         # ADIF import
│   │   ├── models/                   # TypeScript interfaces
│   │   ├── services/                 # HTTP/WebSocket services
│   │   │   ├── auth/                 # Authentication service
│   │   │   ├── log/                  # Log service
│   │   │   ├── qso/                  # QSO service
│   │   │   ├── theme/                # Theme toggle service
│   │   │   └── websocket/            # WebSocket service
│   │   ├── guards/                   # Route guards
│   │   ├── config/                   # Station colors & configs
│   │   └── assets/branding/          # EtherWave Archive logos
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

Contributions are welcome! Please open an issue on GitHub to discuss your proposed change before submitting a pull request.

## Support

For issues, questions, or feature requests, please open an issue on GitHub.

## Acknowledgments

- **Hamlib** - Radio control library
- **QRZ.com** - Callsign lookup API
- **ARRL** - Contest rules and specifications
- Amateur Radio community for feedback and testing
