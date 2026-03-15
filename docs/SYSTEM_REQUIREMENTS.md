# System Requirements - EtherWave Archive Ham Radio Logbook

**Last Updated:** December 2025
**Application Version:** 1.0.0

---

## Quick Reference

| Deployment Type | CPU | RAM | Storage | Docker |
|----------------|-----|-----|---------|--------|
| **Production (Minimum)** | 2 cores | 2 GB | 5 GB | Required |
| **Production (Recommended)** | 4 cores | 4-8 GB | 10 GB | Required |
| **Development** | 4-6 cores | 8-16 GB | 20 GB | Required |
| **Field Deployment** | 2 cores | 1.5-2 GB | 3 GB | Required |

---

## Technology Stack

### Backend
- **Java:** 25 (Eclipse Temurin)
- **Spring Boot:** 4.0.0
- **Spring Framework:** 7.0.0
- **Hibernate:** 7.1.8
- **Maven:** 3.9+

### Frontend
- **Angular:** 21.2.4
- **Node.js:** 25.x
- **TypeScript:** 5.9
- **Bootstrap:** 5.3

### Database
- **PostgreSQL:** 16-alpine (production)
- **SQLite:** 3.x (field deployment)

### Deployment
- **Docker Engine:** 20.10+ (minimum), 24.0+ (recommended)
- **Docker Compose:** 2.0+ (minimum), 2.20+ (recommended)

---

## Production Deployment (Docker)

### Minimum Requirements

**Hardware:**
- **CPU:** 2 cores (x64 architecture)
  - Intel Core i3 or AMD Ryzen 3 equivalent
  - ARM64 supported (Apple Silicon, Raspberry Pi 4+)
- **RAM:** 2 GB minimum
  - PostgreSQL: ~100 MB
  - Spring Boot Backend: ~600 MB
  - Nginx Frontend: ~20 MB
  - System overhead: ~300 MB
  - Headroom: ~1 GB
- **Storage:** 5 GB free disk space
  - Docker images: ~2 GB
  - PostgreSQL data: ~500 MB (grows with QSO data)
  - Logs and temporary files: ~500 MB
  - Container volumes: ~2 GB

**Software:**
- Docker Engine 20.10+ or Docker Desktop 4.0+
- Docker Compose 2.0+
- **Operating Systems:**
  - Linux (Ubuntu 20.04+, Debian 11+, RHEL 8+, Fedora 35+)
  - macOS 11 Big Sur or later
  - Windows 10/11 with WSL2 enabled

**Network:**
- Internet connection for initial setup (Docker image pulls)
- 1 Mbps download, 512 Kbps upload minimum
- Ports 80 and 8080 available

---

### Recommended Requirements

**Hardware:**
- **CPU:** 4 cores (x64 architecture)
  - Intel Core i5/i7 or AMD Ryzen 5/7
  - Turbo boost/precision boost enabled
- **RAM:** 4 GB minimum, 8 GB recommended
  - Supports 10-20 concurrent users
  - Better WebSocket performance
  - Database query caching
  - Room for rig control services
- **Storage:** 10 GB free disk space (SSD recommended)
  - Faster database operations
  - Improved container startup times
  - Room for database growth
  - Map tile caching

**Software:**
- Docker Engine 24.0+ or Docker Desktop 4.20+
- Docker Compose 2.20+
- Modern web browser for access

**Network:**
- 5 Mbps download, 1 Mbps upload (for real-time features)
- Low latency (<100ms) for WebSocket connections
- Static IP or dynamic DNS (for remote access)

---

## Development Environment

### Requirements

**Hardware:**
- **CPU:** 4 cores minimum, 6+ cores recommended
  - Build processes are CPU-intensive
  - Parallel compilation benefits from more cores
- **RAM:** 8 GB minimum, 16 GB recommended
  - Node.js Angular build: ~2 GB
  - Maven Spring Boot build: ~2 GB
  - Docker containers: ~2 GB
  - IDE (VS Code/IntelliJ): ~1-2 GB
  - Operating system: ~2 GB
  - Browser with DevTools: ~1 GB
- **Storage:** 20 GB free disk space (SSD highly recommended)
  - Source code repository: ~100 MB
  - Frontend node_modules: ~500 MB
  - Backend Maven dependencies: ~200 MB
  - Docker images and volumes: ~5 GB
  - Build artifacts: ~500 MB
  - Development tools and IDEs: ~10 GB
  - Workspace and logs: ~3 GB

**Software - Required:**
- **Java Development Kit (JDK):** 25
  - Eclipse Temurin 25 (recommended)
  - Oracle JDK 25
  - OpenJDK 25
- **Node.js:** 25.x
- **npm:** 11.x (included with Node.js)
- **Maven:** 3.9+
- **Git:** 2.30+
- **Docker Engine:** 24.0+ or Docker Desktop 4.20+
- **Docker Compose:** 2.20+

**Software - Recommended:**
- **IDE:**
  - IntelliJ IDEA (Ultimate or Community)
  - Visual Studio Code with extensions:
    - Angular Language Service
    - Java Extension Pack
    - Docker
    - GitLens
    - Prettier
- **Terminal:** Modern shell (bash, zsh, PowerShell 7+)
- **Database Client:**
  - pgAdmin 4
  - DBeaver
  - DataGrip
- **API Testing:**
  - Postman
  - HTTPie
  - curl

**Operating Systems:**
- Linux (Ubuntu 22.04+, Fedora 38+)
- macOS 12 Monterey or later (Apple Silicon fully supported)
- Windows 10/11 with WSL2 and Windows Terminal

---

## Field Deployment (Portable Operations)

### For Field Day, POTA, SOTA, Mobile Operations

**Hardware:**
- **CPU:** 2 cores minimum
  - Intel Celeron or AMD A-series sufficient
  - Raspberry Pi 4 (4GB RAM model) supported
- **RAM:** 1.5 GB minimum, 2 GB recommended
  - SQLite backend uses less memory than PostgreSQL
  - Spring Boot: ~600 MB
  - Nginx: ~20 MB
  - System: ~500 MB
  - Buffer: ~400 MB
- **Storage:** 3 GB free disk space
  - Docker images: ~2 GB
  - SQLite database: ~100 MB (grows with QSO data)
  - Logs and cache: ~100 MB
  - Minimum SD card: 8 GB (for Raspberry Pi)

**Power Requirements:**
- Typical power consumption: 5-15 watts
- Battery runtime estimates (with 100Wh battery):
  - Laptop: 6-8 hours
  - Raspberry Pi 4: 15-20 hours
  - Mini PC: 8-12 hours

**Software:**
- Docker Engine or Docker Desktop
- Use `docker-compose.field.yml` configuration
- Offline operation supported (after initial setup)

**Deployment Command:**
```bash
docker compose -f docker-compose.field.yml up -d
```

---

## Browser Requirements (Client/User Side)

### Supported Browsers

| Browser | Minimum Version | Recommended Version |
|---------|----------------|---------------------|
| Chrome | 90 | Latest |
| Edge | 90 | Latest |
| Firefox | 88 | Latest |
| Safari | 14 | Latest |
| Opera | 76 | Latest |

### Required Browser Features
- JavaScript enabled (ES2020+ support)
- LocalStorage enabled (for theme persistence)
- WebSocket support (for real-time updates)
- Service Workers (optional, for PWA features)
- Modern CSS support:
  - CSS Grid Layout
  - Flexbox
  - CSS Custom Properties (variables)
  - Media Queries

### Display Requirements
- **Minimum Resolution:** 1280x720 (tablet-optimized layout)
- **Recommended Resolution:** 1920x1080 or higher
- **Mobile Devices:** 375x667 minimum (iPhone SE)
- **Optimal Experience:** 1920x1080 or 2560x1440

### Accessibility
- Screen reader support (NVDA, JAWS, VoiceOver)
- Keyboard navigation fully supported
- High contrast mode compatible
- Reduced motion preferences respected
- WCAG 2.1 AA compliant

---

## Network Requirements

### Bandwidth

**Minimum:**
- Download: 1 Mbps
- Upload: 512 Kbps
- Use case: Single user, basic operations

**Recommended:**
- Download: 5 Mbps
- Upload: 1 Mbps
- Use case: Multi-user, real-time updates, map tiles

**Optimal:**
- Download: 10+ Mbps
- Upload: 2+ Mbps
- Use case: Contest operations, multiple concurrent users

### Latency
- **Acceptable:** <300ms
- **Good:** <100ms
- **Optimal:** <50ms
- Critical for WebSocket real-time updates and rig control

### Ports (Docker Deployment)

| Port | Service | Access | Required |
|------|---------|--------|----------|
| 80 | Frontend (HTTP) | External | Yes |
| 8080 | Backend API | External | Yes |
| 5432 | PostgreSQL | Internal only | Yes (production) |
| 4532 | rigctld (Hamlib) | Internal/External | No (optional) |
| 8081 | Rig Control Service | Internal/External | No (optional) |

**Firewall Configuration:**
- Allow incoming on ports 80, 8080
- Allow outgoing on ports 80, 443 (for QRZ API, map tiles)
- Optional: Allow 4532, 8081 for rig control

### External Service Dependencies (Optional)

| Service | Purpose | Fallback |
|---------|---------|----------|
| QRZ.com API | Callsign lookup | Manual entry |
| OpenStreetMap | Map tiles | Cached tiles |
| Leaflet CDN | Map library | Local bundle |

**Offline Operation:**
- Core logging functions work offline
- Map tiles cached after first load
- QRZ lookups unavailable (manual entry required)
- Real-time sync unavailable

---

## Rig Control Requirements (Optional)

### If Using Hamlib Integration

**Hardware:**
- USB port (USB 2.0 or higher)
- Compatible amateur radio transceiver
- USB-to-serial cable or built-in USB interface

**Supported Radios:**
- Yaesu (FT-991, FT-891, FT-DX10, etc.)
- Icom (IC-7300, IC-9700, IC-705, etc.)
- Kenwood (TS-590, TS-890, TS-2000, etc.)
- Elecraft (K3, K4, KX2, KX3, etc.)
- See Hamlib documentation for full compatibility list

**Software:**
- Hamlib 4.0+ (included in rig-control-service Docker image)
- USB device access permissions (udev rules on Linux)
- Serial port configuration (baud rate, data bits, etc.)

**Additional System Resources:**
- RAM: +200 MB per rig control instance
- CPU: +0.5 core per active rig
- USB bandwidth: Minimal (<1 Mbps)

**USB Device Mapping (Linux):**
```bash
# Find USB device
ls -l /dev/ttyUSB* | grep USB

# Grant permissions
sudo chmod 666 /dev/ttyUSB0

# Or add user to dialout group
sudo usermod -aG dialout $USER
```

---

## Scalability and Performance

### Multi-Station Contest Operations

| Concurrent Users | CPU Cores | RAM | Storage | Database Connections |
|-----------------|-----------|-----|---------|---------------------|
| 1-5 | 2-4 | 4 GB | 10 GB | 10 |
| 5-10 | 4-6 | 8 GB | 20 GB | 25 |
| 10-20 | 6-8 | 16 GB | 30 GB | 50 |
| 20-50 | 8-12 | 32 GB | 50 GB | 100 |
| 50+ | 12+ | 64 GB+ | 100 GB+ | 200+ |

### Database Growth Estimates

| QSO Count | Database Size | Recommended RAM | Recommended Storage |
|-----------|---------------|-----------------|-------------------|
| 1,000 | ~1 MB | 2 GB | 5 GB |
| 10,000 | ~10 MB | 4 GB | 10 GB |
| 100,000 | ~100 MB | 8 GB | 20 GB |
| 1,000,000 | ~1 GB | 16 GB | 50 GB |
| 10,000,000 | ~10 GB | 32 GB | 100 GB |

**Formula:** ~1 KB per QSO entry (including indexes and metadata)

### Performance Benchmarks

**Current Measured Resource Usage (Idle):**
- Frontend (Nginx): 14 MB RAM
- Backend (Spring Boot + Java 25): 500 MB RAM
- PostgreSQL 16: 77 MB RAM
- **Total Runtime:** ~591 MB RAM

**Startup Times (Cold Start):**
- PostgreSQL: 3-5 seconds
- Backend API: 10-15 seconds (includes health check)
- Frontend: 1-2 seconds
- **Total System Ready:** ~20 seconds

**Build Times (Clean Build):**
- Frontend (Angular): ~60 seconds (first build)
- Frontend (Angular): ~12 seconds (incremental)
- Backend (Maven): ~120 seconds (first build)
- Backend (Maven): ~5 seconds (incremental with cached dependencies)

**API Response Times (Typical):**
- QSO Entry: <100ms
- Callsign Lookup: <500ms (QRZ API dependent)
- Log List: <50ms
- Statistics: <200ms
- Map Data: <300ms

---

## Cloud Deployment Options

### Virtual Private Server (VPS)

**Minimum VPS Specifications:**
- 1 vCPU, 2 GB RAM, 20 GB SSD
- Examples: DigitalOcean Basic Droplet, AWS t3.small, Linode Nanode

**Recommended VPS Specifications:**
- 2 vCPU, 4 GB RAM, 50 GB SSD
- Examples: DigitalOcean Standard Droplet, AWS t3.medium, Linode 4GB

### Container Orchestration

**Kubernetes:**
- Minimum: 3-node cluster, 2 cores + 4 GB per node
- Resource requests:
  - Frontend: 100m CPU, 128Mi RAM
  - Backend: 500m CPU, 1Gi RAM
  - PostgreSQL: 250m CPU, 512Mi RAM
- Persistent volumes: 10Gi for PostgreSQL

**Docker Swarm:**
- Minimum: 3-node swarm
- Similar resource requirements to standalone Docker

---

## Security Considerations

### Network Security
- HTTPS/TLS recommended for production (reverse proxy with Certbot)
- JWT token expiration configured
- CORS policies enforced
- Rate limiting recommended for API endpoints

### Data Protection
- Database backups scheduled (automated or manual)
- Volume backups for persistent data
- Secrets management (environment variables, Docker secrets)

### Access Control
- Strong passwords enforced
- Role-based access control (RBAC)
- Admin user configuration via environment variables
- API authentication required

---

## Troubleshooting and Support

### Common Issues

**Out of Memory:**
- Increase Docker memory limit
- Adjust JVM heap size (backend)
- Reduce concurrent connections

**Slow Performance:**
- Check CPU usage (`docker stats`)
- Verify disk I/O (use SSD if possible)
- Review database indexes
- Enable query caching

**Build Failures:**
- Ensure sufficient disk space
- Update Docker images
- Clear build caches

### Monitoring

**Docker Stats:**
```bash
docker stats --no-stream
```

**Container Logs:**
```bash
docker compose logs -f [service-name]
```

**Health Checks:**
```bash
curl http://localhost:8080/actuator/health
```

---

## Future Considerations

### Roadmap Items That May Affect Requirements

- **Clustering Support:** May require load balancer, session persistence
- **Advanced Analytics:** May require additional RAM for data processing
- **Video/Image Upload:** Will increase storage requirements
- **Mobile Apps:** No server-side impact, client-side only
- **Advanced Mapping:** May require map tile server (additional storage)

---

## Contact and Support

For questions about system requirements:
- GitHub Issues: https://github.com/[your-repo]/issues
- Documentation: See [README.md](README.md) and [SETUP.md](SETUP.md)

---

**Document Version:** 1.0.0
**Last Updated:** December 2025
