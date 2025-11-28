# Ham Radio Logbook - Setup Guide

Complete setup instructions for developers and system administrators.

## Table of Contents

- [System Requirements](#system-requirements)
- [Quick Start with Docker](#quick-start-with-docker)
- [Local Development Setup](#local-development-setup)
- [Production Deployment](#production-deployment)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

## System Requirements

### For Docker Deployment (Recommended)
- Docker Engine 20.10+
- Docker Compose 2.0+
- 2GB RAM minimum
- 10GB disk space

### For Local Development
- **Java Development Kit (JDK) 21** - Latest LTS version
  - Download: https://adoptium.net/
  - Verify: `java -version` should show version 21
- **Node.js 18+** and npm 9+
  - Download: https://nodejs.org/
  - Verify: `node --version` and `npm --version`
- **Maven 3.9+**
  - Download: https://maven.apache.org/
  - Verify: `mvn --version`
- **Git** for version control
- **PostgreSQL 16** (optional, for production database)
- **Docker** (optional, for rig control service)

## Quick Start with Docker

### Production Setup (PostgreSQL)

1. **Clone the repository**
   ```bash
   git clone https://github.com/campbell-r-e/Hamradiologbook.git
   cd Hamradiologbook
   ```

2. **Configure environment** (optional)
   ```bash
   # Edit docker-compose.yml to set:
   # - ADMIN_USERNAME
   # - ADMIN_PASSWORD
   # - JWT_SECRET (use a strong random string)
   ```

3. **Start services**
   ```bash
   docker-compose up -d
   ```

4. **Check status**
   ```bash
   docker-compose ps

   # Should show:
   # hamradio-backend    (healthy)   :8080
   # hamradio-frontend                :80
   # hamradio-postgres   (healthy)   :5432
   ```

5. **Access the application**
   - Frontend: http://localhost
   - Backend API: http://localhost:8080
   - Health Check: http://localhost:8080/actuator/health

6. **View logs**
   ```bash
   # All services
   docker-compose logs -f

   # Specific service
   docker-compose logs -f backend
   ```

### Field Deployment (SQLite - Portable)

Perfect for portable operations, Field Day, or offline use:

```bash
# Start field deployment
docker-compose -f docker-compose.field.yml up -d

# Check status
docker-compose -f docker-compose.field.yml ps

# Stop
docker-compose -f docker-compose.field.yml down
```

**Advantages of Field Deployment:**
- No PostgreSQL dependency
- Single file database (easy backup)
- Smaller resource footprint
- Portable between systems
- Works offline

## Local Development Setup

### Backend Setup

1. **Install Java 21**
   ```bash
   # macOS (using Homebrew)
   brew install openjdk@21

   # Ubuntu/Debian
   sudo apt install openjdk-21-jdk

   # Verify installation
   java -version  # Should show version 21
   ```

2. **Set JAVA_HOME**
   ```bash
   # macOS
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)

   # Linux
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

   # Add to ~/.bashrc or ~/.zshrc for persistence
   ```

3. **Clone and build backend**
   ```bash
   cd Hamradiologbook/backend

   # Download dependencies and build
   mvn clean install

   # Run the application
   mvn spring-boot:run
   ```

4. **Verify backend is running**
   ```bash
   curl http://localhost:8080/actuator/health
   # Should return: {"status":"UP"}
   ```

### Frontend Setup

1. **Install Node.js and npm**
   ```bash
   # macOS
   brew install node@18

   # Ubuntu/Debian
   curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
   sudo apt-get install -y nodejs

   # Verify
   node --version  # Should be 18+
   npm --version   # Should be 9+
   ```

2. **Install Angular CLI**
   ```bash
   npm install -g @angular/cli@17

   # Verify
   ng version
   ```

3. **Build and run frontend**
   ```bash
   cd Hamradiologbook/frontend/logbook-ui

   # Install dependencies
   npm install

   # Start development server
   ng serve

   # Or for production build
   npm run build
   ```

4. **Access frontend**
   - Development: http://localhost:4200
   - Production build: Serve `dist/logbook-ui` with nginx or other web server

### Database Setup

#### SQLite (Default for Development)

No setup required! The database file `logbook.db` will be created automatically in the backend directory on first run.

#### PostgreSQL (For Production-like Development)

1. **Install PostgreSQL**
   ```bash
   # macOS
   brew install postgresql@16
   brew services start postgresql@16

   # Ubuntu/Debian
   sudo apt install postgresql-16
   sudo systemctl start postgresql
   ```

2. **Create database and user**
   ```bash
   sudo -u postgres psql
   ```

   ```sql
   CREATE DATABASE hamradio_logbook;
   CREATE USER hamradio WITH PASSWORD 'hamradio';
   GRANT ALL PRIVILEGES ON DATABASE hamradio_logbook TO hamradio;
   \q
   ```

3. **Update application.properties**
   ```properties
   # backend/src/main/resources/application.properties
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hamradio_logbook
   SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
   ```

## Production Deployment

### Docker Production Deployment

1. **Prepare server**
   ```bash
   # Install Docker
   curl -fsSL https://get.docker.com -o get-docker.sh
   sudo sh get-docker.sh

   # Install Docker Compose
   sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
   sudo chmod +x /usr/local/bin/docker-compose
   ```

2. **Configure production settings**
   ```bash
   # Edit docker-compose.yml
   vim docker-compose.yml
   ```

   Update these critical settings:
   - `JWT_SECRET` - Use a strong random 256-bit key
   - `ADMIN_PASSWORD` - Set a secure admin password
   - `POSTGRES_PASSWORD` - Set a secure database password
   - `SPRING_DATASOURCE_URL` - Update with production database details

3. **Build and deploy**
   ```bash
   # Build images
   docker-compose build --no-cache

   # Start in detached mode
   docker-compose up -d

   # Check health
   docker-compose ps
   curl http://localhost:8080/actuator/health
   ```

4. **Setup reverse proxy (optional but recommended)**
   ```nginx
   # /etc/nginx/sites-available/hamradio-logbook
   server {
       listen 80;
       server_name logbook.example.com;

       location / {
           proxy_pass http://localhost:80;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }

       location /api {
           proxy_pass http://localhost:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }

       location /ws {
           proxy_pass http://localhost:8080;
           proxy_http_version 1.1;
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection "upgrade";
       }
   }
   ```

5. **Enable SSL with Let's Encrypt**
   ```bash
   sudo apt install certbot python3-certbot-nginx
   sudo certbot --nginx -d logbook.example.com
   ```

### Manual Production Deployment

1. **Build backend**
   ```bash
   cd backend
   export JAVA_HOME=/path/to/java21
   mvn clean package -DskipTests
   ```

2. **Build frontend**
   ```bash
   cd frontend/logbook-ui
   npm install
   npm run build
   ```

3. **Deploy backend**
   ```bash
   # Copy JAR to server
   scp backend/target/logbook-backend-1.0.0-SNAPSHOT.jar user@server:/opt/hamradio-logbook/

   # Create systemd service
   sudo vim /etc/systemd/system/hamradio-backend.service
   ```

   ```ini
   [Unit]
   Description=Ham Radio Logbook Backend
   After=network.target

   [Service]
   Type=simple
   User=hamradio
   WorkingDirectory=/opt/hamradio-logbook
   Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk"
   Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hamradio_logbook"
   Environment="JWT_SECRET=YourSecretKey"
   ExecStart=/usr/lib/jvm/java-21-openjdk/bin/java -jar logbook-backend-1.0.0-SNAPSHOT.jar
   Restart=always

   [Install]
   WantedBy=multi-user.target
   ```

   ```bash
   sudo systemctl enable hamradio-backend
   sudo systemctl start hamradio-backend
   ```

4. **Deploy frontend**
   ```bash
   # Copy built files to nginx
   sudo cp -r frontend/logbook-ui/dist/logbook-ui/* /var/www/hamradio-logbook/

   # Configure nginx (see reverse proxy config above)
   ```

## Configuration

### Backend Configuration

**application.properties** location: `backend/src/main/resources/application.properties`

Key settings:

```properties
# Server
server.port=8080

# Database (choose one)
# PostgreSQL (Production)
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/hamradio_logbook
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect

# SQLite (Development/Field)
# SPRING_DATASOURCE_URL=jdbc:sqlite:logbook.db
# SPRING_JPA_DATABASE_PLATFORM=org.hibernate.community.dialect.SQLiteDialect

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# JWT
app.jwt.secret=${JWT_SECRET:ChangeThisToASecure256BitKeyForProduction}
app.jwt.expiration-ms=86400000

# Admin User (created on startup if not exists)
app.admin.username=${ADMIN_USERNAME:admin}
app.admin.password=${ADMIN_PASSWORD:admin}
app.admin.email=${ADMIN_EMAIL:admin@hamradio.local}

# QRZ API (optional)
qrz.api.username=${QRZ_USERNAME:}
qrz.api.password=${QRZ_PASSWORD:}

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,env,loggers
management.endpoint.health.show-details=when-authorized
management.endpoint.health.probes.enabled=true
```

### Frontend Configuration

**Environment files**: `frontend/logbook-ui/src/environments/`

```typescript
// environment.ts (development)
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  wsUrl: 'ws://localhost:8080/ws'
};

// environment.prod.ts (production)
export const environment = {
  production: true,
  apiUrl: '/api',  // Relative URL when behind reverse proxy
  wsUrl: '/ws'
};
```

### Docker Compose Configuration

**docker-compose.yml** - Production with PostgreSQL

```yaml
services:
  postgres:
    environment:
      POSTGRES_USER: hamradio
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-hamradio}  # Change in production
      POSTGRES_DB: hamradio_logbook

  backend:
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/hamradio_logbook
      SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect
      JWT_SECRET: ${JWT_SECRET:-YourSecretKeyHere}  # MUST change in production
      ADMIN_USERNAME: ${ADMIN_USERNAME:-admin}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD:-admin}  # Change in production
```

**docker-compose.field.yml** - Field deployment with SQLite

No database service needed. SQLite database stored in Docker volume.

## Troubleshooting

### Common Issues

#### 1. Backend fails to start with "java.lang.UnsupportedClassVersionError"

**Problem**: Wrong Java version

**Solution**:
```bash
# Check Java version
java -version  # Must be 21 or higher

# Set correct JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk     # Linux
```

#### 2. Frontend build fails with npm errors

**Problem**: Node modules not installed or version mismatch

**Solution**:
```bash
cd frontend/logbook-ui
rm -rf node_modules package-lock.json
npm install
```

#### 3. CORS errors in browser console

**Problem**: Frontend trying to access backend on different origin

**Solution**: This should be fixed now with `allowedOriginPatterns`. If still occurring, check:
- Backend logs for CORS-related errors
- Frontend environment configuration has correct `apiUrl`

#### 4. Database connection refused

**PostgreSQL**:
```bash
# Check if PostgreSQL is running
docker-compose ps postgres  # Docker
sudo systemctl status postgresql  # Linux
brew services list  # macOS

# Check connection
psql -U hamradio -d hamradio_logbook -h localhost
```

**SQLite**:
```bash
# Check file permissions
ls -l logbook.db

# Verify database
sqlite3 logbook.db ".tables"
```

#### 5. Docker container keeps restarting

**Solution**:
```bash
# Check logs
docker-compose logs backend

# Common causes:
# - Database not ready (add healthcheck wait)
# - Missing environment variables
# - Port already in use
```

#### 6. Health check fails

**Check**:
```bash
curl http://localhost:8080/actuator/health

# Should return:
# {"status":"UP","groups":["liveness","readiness"]}

# If not, check:
docker-compose logs backend
# Look for startup errors
```

### Getting Help

- Check logs: `docker-compose logs -f`
- Verify configuration: Review environment variables
- Test connectivity: Use `curl` to test endpoints
- GitHub Issues: https://github.com/campbell-r-e/Hamradiologbook/issues

## Next Steps

- **[User Guide](docs/USER_GUIDE.md)** - Learn how to use the system
- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Contribute to development
- **[API Reference](docs/API_REFERENCE.md)** - Integrate with the API

---

**73 and good logging!**
