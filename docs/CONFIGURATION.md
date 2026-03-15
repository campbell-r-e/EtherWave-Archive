# EtherWave Archive - Configuration Guide

This guide explains all configuration options for the EtherWave Archive, including theme customization and branding settings.

## Table of Contents

- [Quick Start](#quick-start)
- [Java Configuration](#java-configuration)
- [Backend Configuration](#backend-configuration)
- [Frontend Configuration](#frontend-configuration)
- [Database Configuration](#database-configuration)
- [Security Configuration](#security-configuration)
- [QRZ API Configuration](#qrz-api-configuration)
- [Docker Configuration](#docker-configuration)

---

## Quick Start

Run the automated setup script:

```bash
./setup.sh
```

This will:
-  Check all prerequisites (Java, Node.js, Maven, Docker)
-  Create configuration files
-  Install dependencies
-  Provide next steps

---

## Java Configuration

### Required Version

The project requires **Java 25**. This is the minimum version required for Spring Boot 4.0.3.

### Setting Java Version

A `.java-version` file is created automatically to specify Java 25:

```
25
```

### Available Java Versions on Your System

Check installed Java versions:

```bash
/usr/libexec/java_home -V
```

### Setting JAVA_HOME (if needed)

For Java 25:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
```

For Java 17:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

Add to your `~/.zshrc` or `~/.bash_profile` to make permanent.

---

## Backend Configuration

### Configuration File Location

`backend/.env`

### Configuration Options

#### Database Configuration

**SQLite (Development/Field Deployment)**
```env
SPRING_DATASOURCE_URL=jdbc:sqlite:logbook.db
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.community.dialect.SQLiteDialect
```

**PostgreSQL (Production/Multi-User)**
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/hamradio_logbook
SPRING_DATASOURCE_USERNAME=hamradio
SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}   # Set in .env
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
```

#### JWT Authentication

```env
# Generate secure key: openssl rand -base64 64
JWT_SECRET=YourSecureSecretKeyHere-MustBe256BitsMinimum
JWT_EXPIRATION_MS=86400000  # 24 hours in milliseconds
```

#### Admin User (Auto-created on first startup)

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=changeme123
# ADMIN_EMAIL removed — email not required
```

**IMPORTANT:** Change these defaults before deploying to production!

#### QRZ API (Optional)

```env
QRZ_USERNAME=your_qrz_username
QRZ_PASSWORD=your_qrz_password
```

Get credentials at: https://www.qrz.com/

#### Actuator Endpoints

```env
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics
```

### Application Properties

Located at: `backend/src/main/resources/application.properties`

Key settings:
- Server port: `8080`
- CORS origins: `http://localhost:4200`
- WebSocket origins: `http://localhost:4200`
- Health check endpoints enabled
- Auto-schema update enabled

---

## Frontend Configuration

### Development Environment

File: `frontend/logbook-ui/src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

### Production Environment

File: `frontend/logbook-ui/src/environments/environment.prod.ts`

```typescript
export const environment = {
  production: true,
  apiUrl: '/api'   // Relative URL - routed through nginx to backend
};
```

### Angular Configuration

File: `frontend/logbook-ui/angular.json`

The project is configured with:
- Angular 21.2.4
- TypeScript 5.9
- Bootstrap 5.3
- Development server on port 4200

### Theme Configuration (EtherWave Archive Branding)

The application features a professional professional interface with dark/light mode support.

**Theme Service**: `frontend/logbook-ui/src/app/services/theme/theme.service.ts`

**Theme Management:**
- Automatic theme detection from system preferences
- Manual toggle via navbar button (/)
- localStorage persistence (`gekhoosier-theme` key)
- Real-time theme switching without page reload

**Color System**: `frontend/logbook-ui/src/styles.css`

Primary brand colors:
- Gold: `#003F87`
- Bronze: `#C41E3A`
- QSO Green: `#4CAF50`
- Highlight Yellow: `#F5C542`

Station colors:
- Station 1: Blue `#1E88E5`
- Station 2: Red `#E53935`
- GOTA: Green `#43A047`
- Viewer: Gray `#9E9E9E`

**Station Color Configuration**: `frontend/logbook-ui/src/app/config/station-colors.ts`

TypeScript utilities for consistent station color usage across components.

**Branding Assets**: `frontend/logbook-ui/src/assets/branding/`
- `logo.png` - Primary logo (light mode) - 1.8MB
- `logo-dark.png` - Dark mode variant - 1.8MB
- `icon.png` - Icon-only version - 1.8MB

**For more details**, see [BRANDING.md](BRANDING.md) for complete branding guidelines.

---

## Database Configuration

### SQLite (Recommended for Development)

**Advantages:**
-  No separate database server needed
-  Single file database (easy backup)
-  Perfect for field operations
-  Zero configuration

**Configuration:**
```env
SPRING_DATASOURCE_URL=jdbc:sqlite:logbook.db
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.community.dialect.SQLiteDialect
```

**Database file location:** `backend/logbook.db`

### PostgreSQL (Recommended for Production)

**Advantages:**
-  Better performance for multi-user
-  Advanced features
-  Production-grade reliability

**Configuration (Docker deployment):**
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/hamradio_logbook
SPRING_DATASOURCE_USERNAME=hamradio
SPRING_DATASOURCE_PASSWORD=<set in .env as POSTGRES_PASSWORD>
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
```

**Setup PostgreSQL (standalone):**

Using Docker:
```bash
docker run -d \
  --name postgres-hamradio \
  -e POSTGRES_DB=hamradio_logbook \
  -e POSTGRES_USER=hamradio \
  -e POSTGRES_PASSWORD=<strong-password> \
  -p 5432:5432 \
  postgres:16-alpine
```

Using docker-compose:
```bash
docker compose up -d postgres
```

---

## Security Configuration

### JWT Token Configuration

**Generate a secure secret key:**

```bash
openssl rand -base64 64
```

Copy the output to your `.env` file:
```env
JWT_SECRET=<paste-generated-key-here>
```

**Token expiration:**
```env
JWT_EXPIRATION_MS=86400000  # 24 hours
```

### Admin User

The admin user is created automatically on first startup. Configure in `.env`:

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=SecurePassword123!
# ADMIN_EMAIL removed — email not required
```

**IMPORTANT:**
- Change default credentials before production deployment
- Use strong passwords (min 8 characters, mixed case, numbers, symbols)
- Keep credentials secure

### CORS Configuration

Configured in `application.properties`:

```properties
spring.web.cors.allowed-origins=http://localhost:4200
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
```

For production, update to your domain:
```properties
spring.web.cors.allowed-origins=https://yourhamradio.com
```

---

## QRZ API Configuration

QRZ.com provides callsign lookup services (optional but recommended).

### Sign Up

1. Create account at https://www.qrz.com/
2. Subscribe to XML Data service (if not already subscribed)
3. Get your credentials

### Configure

Add to `backend/.env`:
```env
QRZ_USERNAME=your_qrz_username
QRZ_PASSWORD=your_qrz_password
```

### Test

After starting the backend, callsign lookups will automatically use QRZ API when available.

---

## Docker Configuration

### Production Deployment

File: `docker-compose.yml`

Uses PostgreSQL database for production multi-user setup.

**Setup:**
```bash
cp .env.example .env   # Edit .env with your passwords and JWT_SECRET
docker compose up -d
```

All secrets are read from `.env`. See `.env.example` for available variables.

### Field Deployment

File: `docker-compose.field.yml`

Uses SQLite for portable/offline operations.

**Start:**
```bash
docker compose -f docker-compose.field.yml up -d
```

**Advantages:**
- No PostgreSQL dependency
- Single SQLite database file
- Smaller resource footprint
- Perfect for Field Day

### Rig Control (Optional)

Uncomment the rig-control section in `docker-compose.yml` and configure:

```yaml
rig-control:
  environment:
    STATION_ID: 1
    STATION_NAME: "Station-1"
    RIGCTLD_HOST: localhost
    RIGCTLD_PORT: 4532
  devices:
    - "/dev/ttyUSB0:/dev/ttyUSB0"  # Adjust for your radio
```

Find your USB device:
```bash
ls -l /dev/tty* | grep USB
```

See `RIG_CONTROL_GUIDE.md` for detailed rig control setup.

---

## Running the Application

### Option 1: Docker (Recommended)

**Production (PostgreSQL):**
```bash
docker compose up -d
```

**Field (SQLite):**
```bash
docker compose -f docker-compose.field.yml up -d
```

**Access:**
- Frontend: http://localhost
- Backend: http://localhost:8080
- Health: http://localhost:8080/actuator/health

### Option 2: Local Development

**Terminal 1 - Backend:**
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
cd backend
mvn spring-boot:run
```

**Terminal 2 - Frontend:**
```bash
cd frontend/logbook-ui
npm install  # First time only
npm start
```

**Access:**
- Frontend: http://localhost:4200
- Backend: http://localhost:8080

### Option 3: Production Build

**Backend:**
```bash
cd backend
mvn clean package
java -jar target/ham-radio-logbook-*.jar
```

**Frontend:**
```bash
cd frontend/logbook-ui
npm run build
# Deploy dist/ folder to web server
```

---

## Verification

### Check Backend Health

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Check Frontend

Open browser: http://localhost:4200 (dev) or http://localhost (Docker)

### Check Database Connection

Backend logs should show:
```
Hibernate: create table if not exists users ...
Admin user created: admin
```

### First Login

1. Open frontend in browser
2. Click "Register here" to create your account
3. Or login with admin credentials (if configured)
4. Create a logbook
5. Start logging QSOs!

---

## Troubleshooting

### Java Version Issues

```bash
# Check current Java version
java -version

# Set correct Java version
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
```

### Backend Won't Start

Check `backend/.env` file exists and has correct configuration.

### Database Connection Errors

**SQLite:** Ensure write permissions in backend directory
**PostgreSQL:** Ensure PostgreSQL is running and credentials are correct

### Frontend Can't Connect to Backend

Check CORS configuration in `application.properties` matches frontend URL.

### Docker Issues

```bash
# Check running containers
docker compose ps

# View logs
docker compose logs backend
docker compose logs frontend

# Restart services
docker compose restart
```

---

## Next Steps

-  Configuration complete? See [QUICKSTART.md](QUICKSTART.md)
-  Need detailed setup? See [SETUP.md](SETUP.md)
-  Want rig control? See [RIG_CONTROL_GUIDE.md](RIG_CONTROL_GUIDE.md)
-  Ready to use? See [docs/USER_GUIDE.md](USER_GUIDE.md)

---

**73 and happy logging!**
