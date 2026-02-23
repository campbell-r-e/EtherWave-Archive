# EtherWave Archive - Quick Start Guide

**Get your professional ham radio logging system running in 5 minutes!**

This guide will get you up and running with the complete EtherWave Archive as quickly as possible.

## What You'll Get

-  **Professional EtherWave Archive UI** - professional interface with dark/light mode toggle
-  **Multi-user logbook system** - Create personal or shared contest logs
-  **Real-time QSO entry** - Log contacts with instant validation
-  **Map visualization** - See your QSOs plotted on a world map
-  **Rig control** - Automatic frequency/mode detection via Hamlib
-  **Contest support** - Built-in validators for CQ WW, ARRL, Field Day, POTA, SOTA
-  **Export** - ADIF 3.1.4 and Cabrillo formats
-  **Multi-operator** - Role-based access with station/operator management
-  **Theme toggle** - Switch between light and dark modes with a single click

---

## Option 1: Docker (Easiest - Recommended)

**Perfect for: Windows, Mac, Linux users who want the easiest setup**

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop) installed and running
- 2GB RAM available
- 10GB disk space

### Setup Steps

**1. Download the project**
```bash
git clone https://github.com/campbell-r-e/Hamradiologbook.git
cd Hamradiologbook
```

**2. Create your `.env` file**
```bash
cp .env.example .env
```

Edit `.env` and set secure values for `POSTGRES_PASSWORD`, `JWT_SECRET`, and `ADMIN_PASSWORD`. Use `DDL_AUTO=update` on first deploy (switch to `validate` after the schema is created).

**3. Start everything with one command**
```bash
docker-compose up -d
```

This will:
- Download all required images (Node.js 24, Java 25, PostgreSQL 16, Nginx)
- Build the backend and frontend
- Create the database
- Start all services

**3. Wait for services to start (about 1-2 minutes)**

Check the status:
```bash
docker-compose ps
```

You should see:
```
NAME                STATUS                 PORTS
hamradio-backend    Up (healthy)           :8080
hamradio-frontend   Up                     :80
hamradio-postgres   Up (healthy)
```

**4. Open your browser**

Go to: **http://localhost**

**5. Create your account**

1. Click "Register here" on the login page
2. Fill in:
   - Username (required)
   - Email (required)
   - Password (required, 8+ characters)
   - Callsign (optional, e.g., W1ABC)
   - Grid Square (optional, e.g., FN31pr)
3. Click "Register"
4. Login with your credentials

**6. Create your first logbook**

1. Click the dropdown at the top that says "Select a Log"
2. Click "Create New Log"
3. Enter:
   - Log Name (e.g., "My General Log" or "2025 Field Day")
   - Description (optional)
4. Click "Create"

**7. Customize your experience**

Toggle between light and dark modes using the theme button (/) in the top-right navbar. Your preference is saved automatically!

**8. Start logging QSOs!**

You're ready to log contacts. The QSO entry form will guide you through:
- Station selection (create one first)
- Callsign lookup (integrated with QRZ.com if configured)
- Frequency, Mode, RST reports
- Date/Time (UTC)
- Optional: Contest data, location info

---

## Option 2: Local Development

**Perfect for: Developers who want to modify the code**

### Prerequisites

Download and install these:

1. **Java 25** - https://adoptium.net/
   - Verify: `java -version` (should show 25.x.x)

2. **Node.js 24** - https://nodejs.org/
   - Verify: `node --version` (should show v24.x.x)

3. **Maven 3.9+** - https://maven.apache.org/
   - Verify: `mvn --version`

4. **PostgreSQL 16** (optional) - https://www.postgresql.org/download/
   - Or use H2/SQLite for testing

### Setup Steps

**1. Clone the repository**
```bash
git clone https://github.com/campbell-r-e/Hamradiologbook.git
cd Hamradiologbook
```

**2. Start the Backend**

```bash
# Navigate to backend
cd backend

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The backend will start on **http://localhost:8080**

Check health: http://localhost:8080/actuator/health

**3. Start the Frontend** (in a new terminal)

```bash
# Navigate to frontend
cd frontend/logbook-ui

# Install dependencies
npm install

# Start development server
npm start
```

The frontend will start on **http://localhost:4200**

**4. Open your browser**

Go to: **http://localhost:4200**

---

## Default Admin Account

The admin account is created automatically on first startup using the credentials you set in `.env`:

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=YourSecurePassword
ADMIN_EMAIL=admin@hamradio.local
```

**Set a strong password in `.env` before first startup.**

---

## Quick Configuration

### Docker Setup

All configuration is done via the `.env` file in the project root:

```env
# Database
POSTGRES_PASSWORD=<strong-password>

# Security - generate with: openssl rand -base64 64
JWT_SECRET=<generated-key>
JWT_EXPIRATION_MS=86400000

# Admin account (created on first startup)
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<strong-password>
ADMIN_EMAIL=admin@hamradio.local

# Schema management
DDL_AUTO=update   # First deploy only, then change to "validate"

# Optional: QRZ.com callsign lookups
QRZ_USERNAME=your-qrz-username
QRZ_PASSWORD=your-qrz-password
```

### Local Development Setup

Create `backend/src/main/resources/application-dev.properties`:

```properties
# Database (H2 for development)
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# Admin account
admin.username=admin
admin.password=admin
admin.email=admin@hamradio.local

# JWT
jwt.secret=DevSecretKeyMinimum256BitsForHS512
jwt.expiration-ms=86400000

# QRZ API (optional)
qrz.username=your-qrz-username
qrz.password=your-qrz-password
```

---

## Accessing Different Parts

| Component | Docker URL | Local Dev URL | Purpose |
|-----------|-----------|---------------|---------|
| **Frontend** | http://localhost | http://localhost:4200 | Main user interface |
| **Backend API** | http://localhost:8080 | http://localhost:8080 | REST API |
| **Health Check** | http://localhost:8080/actuator/health | http://localhost:8080/actuator/health | System status |

The PostgreSQL database is internal to the Docker network and not accessible from the host directly. Use `docker exec -it hamradio-postgres psql -U hamradio hamradio_logbook` for direct access.

---

## Common Commands

### Docker Commands

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend

# Restart a service
docker-compose restart frontend

# Rebuild after code changes
docker-compose build
docker-compose up -d

# Clean everything (including database!)
docker-compose down -v
```

### Local Development Commands

```bash
# Backend
cd backend
mvn clean install          # Build
mvn spring-boot:run       # Run
mvn test                  # Test

# Frontend
cd frontend/logbook-ui
npm install               # Install dependencies
npm start                 # Development server
npm run build             # Production build
npm test                  # Run tests
```

---

## Quick Troubleshooting

### "Cannot connect to backend"

**Docker:**
```bash
# Check if backend is healthy
docker-compose ps

# View backend logs
docker-compose logs backend

# Restart backend
docker-compose restart backend
```

**Local Dev:**
```bash
# Check if backend is running
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

### "Port already in use"

**Docker:**
```bash
# Stop conflicting services
docker-compose down

# Or change ports in docker-compose.yml:
ports:
  - "8081:8080"  # Backend on 8081 instead of 8080
  - "81:80"      # Frontend on 81 instead of 80
```

**Local Dev:**
```bash
# Frontend: Change port in package.json or use:
ng serve --port 4201

# Backend: Change port in application.properties:
server.port=8081
```

### "Login not working"

1. Check browser console (F12) for errors
2. Verify backend is running: http://localhost:8080/actuator/health
3. Check CORS settings if using different ports
4. Try registering a new account first

### "Database connection failed"

**Docker:**
```bash
# Check if database is healthy
docker-compose ps

# Should show hamradio-postgres as "healthy"

# If not, restart database
docker-compose restart postgres
```

**Local Dev:**
- Make sure PostgreSQL is running
- Check database credentials in application.properties
- Or use H2 in-memory database for testing

### "Frontend shows blank page"

1. Check browser console (F12) for errors
2. Verify build completed successfully
3. Hard refresh: Ctrl+Shift+R (Windows/Linux) or Cmd+Shift+R (Mac)
4. Clear browser cache

---

## What to Do Next

After getting the system running:

1. **Create a Station**
   - Go to "Station Management" section
   - Add your station callsign and equipment

2. **Configure a Contest** (optional)
   - Select from pre-configured contests (CQ WW, Field Day, etc.)
   - Or operate without a contest

3. **Start Logging**
   - Use the QSO Entry form
   - Fields auto-populate from rig control (if configured)
   - Callsign lookup from QRZ.com (if configured)

4. **Set Up Rig Control** (optional)
   - See [RIG_CONTROL_GUIDE.md](RIG_CONTROL_GUIDE.md) for complete instructions
   - Requires Hamlib-compatible radio
   - Provides automatic frequency/mode detection

5. **Invite Other Operators** (for multi-op)
   - Go to "Invitations" section
   - Send invitations to team members
   - Assign roles: CREATOR, STATION, or VIEWER

6. **Export Your Log**
   - Use the Export Panel
   - Choose ADIF or Cabrillo format
   - Submit to contest sponsors

---

## Getting Help

- **Documentation**: See the `/docs` folder for detailed guides
- **Issues**: https://github.com/campbell-r-e/Hamradiologbook/issues
- **Setup Guide**: [SETUP.md](SETUP.md) - Detailed configuration
- **Developer Guide**: [docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md)
- **User Guide**: [docs/USER_GUIDE.md](docs/USER_GUIDE.md)
- **API Reference**: [docs/API_REFERENCE.md](docs/API_REFERENCE.md)

---

## System Architecture Overview

```
┌─────────────────────────────────────────────┐
│          Frontend (Angular 21)              │
│     http://localhost (or :4200)             │
│  - Login/Register                           │
│  - QSO Entry                                │
│  - Map Visualization                        │
│  - Export Tools                             │
└─────────────────┬───────────────────────────┘
                  │ HTTP/WebSocket
┌─────────────────▼───────────────────────────┐
│       Backend (Spring Boot 4.0.0)           │
│        http://localhost:8080                │
│  - REST API                                 │
│  - JWT Authentication                       │
│  - Contest Validation                       │
│  - WebSocket for real-time                  │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│      Database (PostgreSQL 16)               │
│     (internal Docker network only)          │
│  - Users, Logs, QSOs                        │
│  - Stations, Contests                       │
└─────────────────────────────────────────────┘
```

---

## Success Checklist

 Docker installed and running (for Docker setup)
 Java 25, Node 24, Maven installed (for local dev)
 All containers showing "healthy" status
 Frontend loads at http://localhost
 Backend health check shows "UP"
 Can register a new account
 Can login successfully
 Can create a logbook
 Can create a station
 Can log a QSO

---

**73 and happy logging!** 

Built with  for the Amateur Radio Community
