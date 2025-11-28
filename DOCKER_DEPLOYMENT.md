# Docker Deployment Guide

Complete Docker containerization for the Ham Radio Logbook System with support for both development and field deployment scenarios.

## Quick Start

### Development (PostgreSQL)
```bash
docker-compose up -d
```
Access: http://localhost

### Field Deployment (SQLite)
```bash
docker-compose -f docker-compose.field.yml up -d
```
Access: http://localhost

## Architecture

```
┌─────────────────────────────────────────────────┐
│              Docker Compose Network              │
│                                                  │
│  ┌────────────┐  ┌─────────────┐  ┌──────────┐ │
│  │ PostgreSQL │  │   Backend   │  │ Frontend │ │
│  │  Port 5432 │◄─┤  Port 8080  │◄─┤  Port 80 │ │
│  └────────────┘  └─────────────┘  └──────────┘ │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │  Rig Control (optional)                  │  │
│  │  Port 8081, with USB device passthrough  │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

## Services

### 1. Backend (Spring Boot)
- **Image**: Custom multi-stage build (Java 21)
- **Port**: 8080
- **Database**: PostgreSQL (dev) or SQLite (field)
- **Features**:
  - REST API
  - WebSocket support
  - Contest validation
  - ADIF/Cabrillo export
  - Spring Boot Actuator (health, metrics, monitoring)

### 2. Frontend (Angular + Nginx)
- **Image**: Custom multi-stage build
- **Port**: 80
- **Features**:
  - Serves static Angular app
  - Proxies `/api/*` to backend
  - WebSocket passthrough
  - Gzip compression

### 3. PostgreSQL (dev only)
- **Image**: postgres:16-alpine
- **Port**: 5432
- **Credentials**: hamradio/changeme
- **Volume**: Persistent storage

### 4. Rig Control (optional)
- **Image**: Custom with Hamlib (Java 21 + libhamlib4)
- **Ports**: 8081 (service), 4532 (rigctld)
- **Requires**: USB device passthrough
- **Base**: Ubuntu Jammy with universe repository for Hamlib packages

## Configuration

### Environment Variables

Create `.env` file in project root:

```env
# QRZ API Credentials (optional)
QRZ_USERNAME=your_username
QRZ_PASSWORD=your_password

# PostgreSQL (development)
POSTGRES_PASSWORD=changeme

# Station Configuration (rig control)
STATION_ID=1
STATION_NAME=HF-Station
```

### Database Selection

**Development (PostgreSQL)**:
- Scalable, multi-client support
- Better for long-term storage
- Use: `docker-compose.yml`

**Field (SQLite)**:
- Single-file database
- No separate DB container
- Portable, offline-capable
- Use: `docker-compose.field.yml`

## Deployment Scenarios

### Scenario 1: Local Development

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

### Scenario 2: Field Day Setup

**Server Machine** (connected via Ethernet to router):
```bash
# Start backend + frontend with SQLite
docker-compose -f docker-compose.field.yml up -d

# Data persists in ./data/logbook.db
```

**Client Machines** (connected via Wi-Fi):

Run rig control service natively (better USB performance):
```bash
cd rig-control-service
java -jar target/rig-control-service-1.0.0-SNAPSHOT.jar \
  --station.id=1 \
  --backend.api.url=http://192.168.1.100:8080 \
  --rigctld.host=localhost
```

OR run in Docker with USB passthrough:
```bash
# Edit docker-compose.yml, uncomment rig-control section
# Adjust /dev/ttyUSB0 to your radio's device
docker-compose up -d rig-control
```

### Scenario 3: Home Station (Permanent)

```bash
# Use PostgreSQL for permanent installation
docker-compose up -d

# Enable autostart
docker update --restart=always hamradio-backend
docker update --restart=always hamradio-frontend
docker update --restart=always hamradio-postgres
```

## Rig Control with Docker

### Find USB Device
```bash
# Linux/Mac
ls -l /dev/tty* | grep USB

# Output: /dev/ttyUSB0 or /dev/cu.usbserial-*
```

### Configure USB Passthrough

Edit `docker-compose.yml`:
```yaml
rig-control:
  devices:
    - "/dev/ttyUSB0:/dev/ttyUSB0"  # Adjust to your device
  command: >
    sh -c "
      rigctld -m 1035 -r /dev/ttyUSB0 -s 38400 -t 4532 &
      java -jar app.jar
    "
```

### Common Rig Models
- Yaesu FT-991A: `-m 1035`
- Icom IC-7300: `-m 3073`
- Kenwood TS-590SG: `-m 2014`
- Elecraft K3: `-m 2029`

Find yours: `docker run --rm eclipse-temurin:21-jre-jammy sh -c "apt-get update && apt-get install -y software-properties-common && add-apt-repository universe && apt-get update && apt-get install -y hamlib-utils && rigctl --list"`

## Management Commands

### Build
```bash
# Build all images
docker-compose build

# Build specific service
docker-compose build backend

# No cache rebuild
docker-compose build --no-cache
```

### Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend

# Last 100 lines
docker-compose logs --tail=100 backend
```

### Database Backup

**PostgreSQL:**
```bash
docker exec hamradio-postgres pg_dump -U hamradio hamradio_logbook > backup.sql
```

**SQLite:**
```bash
cp ./data/logbook.db ./backups/logbook_$(date +%Y%m%d).db
```

### Health Checks
```bash
# Backend - Main health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","groups":["liveness","readiness"]}

# Liveness probe (is app running?)
curl http://localhost:8080/actuator/health/liveness

# Readiness probe (can app handle traffic?)
curl http://localhost:8080/actuator/health/readiness

# Detailed health (when authenticated)
curl -H "Authorization: Bearer <token>" http://localhost:8080/actuator/health

# Frontend
curl http://localhost

# Rig Control
curl http://localhost:8081/api/rig/test
```

## Networking

### Access from Other Devices

Find server IP:
```bash
hostname -I  # Linux
ipconfig getifaddr en0  # Mac
```

Access from clients:
```
http://192.168.1.100      (Frontend)
http://192.168.1.100:8080 (Backend API)
```

### Port Mapping

Default ports:
- `80` → Frontend
- `8080` → Backend
- `5432` → PostgreSQL
- `8081` → Rig Control Service
- `4532` → rigctld

Change in `docker-compose.yml`:
```yaml
ports:
  - "8888:80"  # Frontend now on port 8888
```

## Troubleshooting

### Permission Denied (USB)
```bash
# Add user to dialout group
sudo usermod -a -G dialout $USER

# Or run container as privileged
privileged: true
```

### Database Connection Failed
```bash
# Check PostgreSQL is ready
docker-compose logs postgres

# Verify health
docker inspect hamradio-postgres | grep -A 10 Health
```

### Frontend Can't Reach Backend
Check nginx proxy configuration in `frontend/logbook-ui/nginx.conf`

### Rig Control Can't Connect
```bash
# Test rigctld manually
docker exec -it hamradio-rig-control-1 rigctl -m 1035 -r /dev/ttyUSB0 f
```

## Production Considerations

### Security
```env
# Change default password
POSTGRES_PASSWORD=strong_random_password

# Use secrets management
docker secret create postgres_password ./postgres_password.txt
```

### Resource Limits
```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
```

### SSL/TLS
Use a reverse proxy (Traefik, Nginx) in front:
```yaml
services:
  traefik:
    image: traefik:v2.10
    ports:
      - "443:443"
```

## Multi-Station Field Deployment

**Server** (one machine):
```bash
docker-compose -f docker-compose.field.yml up -d
```

**Clients** (5-6 laptops):

Each runs rig control natively:
```bash
# Station 1
java -jar rig-control-service.jar --station.id=1 --server.port=8081

# Station 2
java -jar rig-control-service.jar --station.id=2 --server.port=8082

# Station 3
java -jar rig-control-service.jar --station.id=3 --server.port=8083
```

All access frontend: `http://server-ip`

## Performance

Expected resource usage:
- Backend: ~300MB RAM, <5% CPU
- Frontend: ~50MB RAM
- PostgreSQL: ~100MB RAM
- Rig Control: ~150MB RAM per instance

Total for single station: **~600MB RAM**

## Updates

```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose down
docker-compose build
docker-compose up -d
```

## Cleanup

```bash
# Stop and remove containers
docker-compose down

# Remove volumes (deletes database!)
docker-compose down -v

# Remove images
docker rmi hamradiologbook-backend
docker rmi hamradiologbook-frontend
docker rmi hamradiologbook-rig-control
```
