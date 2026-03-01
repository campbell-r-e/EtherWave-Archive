# Docker Deployment - EtherWave Archive

## Quick Summary

Runs two containers via Docker Compose:

- Frontend (Angular + Nginx) on http://localhost (port 80)
- Backend API (Spring Boot) on http://localhost:8080
- PostgreSQL 16 database (internal only, not exposed to host)

All containers restart automatically unless manually stopped.

---

## Prerequisites

- Docker Engine 24+
- Docker Compose v2 plugin (`docker compose` — note: no hyphen)

---

## First-Time Setup

**1. Clone the repository**
```bash
git clone https://github.com/campbell-r-e/EtherWave-Archive.git
cd EtherWave-Archive
```

**2. Create your `.env` file**
```bash
cp .env.example .env
```

Edit `.env` and set strong values for:
```env
POSTGRES_PASSWORD=<strong-password>
JWT_SECRET=<output-of: openssl rand -base64 64>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<strong-password>
DDL_AUTO=update
```

**3. Start services**
```bash
docker compose up -d
```

**4. Check status**
```bash
docker compose ps
# All services should show "Up" or "Up (healthy)"
```

**5. Open browser**
- Application: http://localhost
- Backend API: http://localhost:8080
- Health Check: http://localhost:8080/actuator/health

---

## Access Points

| Service | URL |
|---------|-----|
| Application | http://localhost |
| API | http://localhost:8080 |
| Health Check | http://localhost:8080/actuator/health |

PostgreSQL runs on the internal Docker network only and is not accessible from the host.

---

## LAN / Network Access

The application is accessible from any device on the local network:

```
http://<host-ip>
```

Replace `<host-ip>` with the IP address of the machine running Docker. No additional configuration is required.

---

## Field Deployment (SQLite / Offline)

For portable operations, Field Day, or environments without PostgreSQL:

```bash
docker compose -f docker-compose.field.yml up -d
```

The SQLite database file is stored at `./data/logbook.db` on the host — easy to back up or transfer.

---

## Quick Commands

```bash
# Start services
docker compose up -d

# Stop services
docker compose down

# View logs
docker compose logs -f

# View specific service logs
docker compose logs -f backend

# Rebuild after code changes
docker compose build && docker compose up -d

# Remove containers and database volume (destructive)
docker compose down -v

# Check status
docker compose ps
```

---

## Database

**PostgreSQL 16** is used for production.

- Database: `hamradio_logbook`
- User: `hamradio`
- Password: set via `POSTGRES_PASSWORD` in `.env`
- Persistent volume: `postgres_data`

The database port is not exposed to the host. If you need direct access for maintenance:
```bash
docker exec -it hamradio-postgres psql -U hamradio hamradio_logbook
```

---

## DDL Auto Setting

`DDL_AUTO=update` (the default) lets Hibernate manage the schema automatically — it creates or evolves tables as needed without data loss. This is safe for both fresh installs and upgrades.

Only switch to `DDL_AUTO=validate` if you want strict enforcement that the schema exactly matches the entities (useful in locked-down production environments where you manage schema migrations manually).

---

## Container Security

The backend runs as a non-root user (`appuser`) inside the container. The `docker-entrypoint.sh` script handles privilege dropping automatically:

- **Production (named volumes)**: container starts directly as `appuser`
- **Field (bind-mounted `./data`)**: container starts as root, fixes directory ownership, then drops to `appuser` before the JVM starts

---

## Deployed Features

- Multi-user logbook with role-based access
- Multi-station contest logging with auto-tagging
- Real-time leaderboards and scoring
- GOTA support with separate scoring
- Color-coded station badges
- WebSocket real-time updates
- ADIF 3.1.4 and Cabrillo export

---

## What's Different from Local Development

| Feature | Local Dev | Docker |
|---------|-----------|--------|
| Database | SQLite | PostgreSQL 16 |
| Port 80 | Angular dev server | Nginx production build |
| Port 8080 | Maven | Containerized JAR |
| Data persistence | Local file | Docker volume |
| Multi-user | Limited | Full support |
| Auto-restart | No | Yes (unless-stopped) |

---

## License

Licensed under the Permissive Public License version 1.11. See [License.md](License.md) for full terms.
