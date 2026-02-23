# Docker Deployment - EtherWave Archive 

**Date**: 2025-12-05  
**Status**:  **RUNNING**

---

##  Quick Summary

The multi-station contest logging system is now running in Docker with:
-  Frontend (Angular + Nginx) on http://localhost
-  Backend API (Spring Boot) on http://localhost:8080  
-  PostgreSQL database on localhost:5432
-  All multi-station features operational

---

##  Access Points

| Service | URL | Status |
|---------|-----|--------|
| **Application** | http://localhost |  Running |
| **API** | http://localhost:8080 |  Healthy |
| **Health Check** | http://localhost:8080/actuator/health |  UP |
| **Database** | localhost:5432 |  Healthy |

---

##  Quick Commands

```bash
# Start services
docker-compose up -d

# Stop services  
docker-compose down

# View logs
docker-compose logs -f

# Rebuild after changes
docker-compose build && docker-compose up -d

# Check status
docker-compose ps
```

---

##  Database

**PostgreSQL 16** (production-grade)
- Database: `hamradio_logbook`
- User: `hamradio`
- Password: `changeme` ( change in production!)
- Persistent volume: `postgres_data`

---

##  Deployed Features

- Multi-station contest logging
- Auto-tagging with station assignments
- **Station assignment display fix** (shows immediately on login)
- Real-time leaderboards with medals 
- GOTA support with separate scoring
- Color-coded station badges
- Tabbed QSO filtering
- WebSocket real-time updates

---

##  What's New vs Local Development

| Feature | Local Dev | Docker |
|---------|-----------|--------|
| Database | SQLite | PostgreSQL 16 |
| Port 80 | Angular dev server | Nginx production |
| Port 8080 | Maven | Containerized JAR |
| Data | Local file | Docker volume |
| Multi-user | Limited | Full support |

---

**Deployment Complete!**   
Access the application at http://localhost

