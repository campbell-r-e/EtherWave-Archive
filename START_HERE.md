# 🚀 START HERE - GEKHoosier QSO Suite Quick Reference

## ✅ Configuration Complete!

All initial configuration files have been created. Your GEKHoosier QSO Suite is ready to run with an Indiana-themed professional interface featuring dark mode support!

---

## 🎯 Choose Your Setup Method

### Option 1: Docker (Easiest - 2 Minutes)

**Production Mode (PostgreSQL):**
```bash
docker-compose up -d
```

**Field Mode (SQLite - for portable/offline):**
```bash
docker-compose -f docker-compose.field.yml up -d
```

**Access:** http://localhost

---

### Option 2: Local Development

**Set Java 25 (required):**
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
```

**Terminal 1 - Start Backend:**
```bash
cd backend
mvn spring-boot:run
```

**Terminal 2 - Start Frontend:**
```bash
cd frontend/logbook-ui
npm start
```

**Access:** http://localhost:4200

---

## ⚙️ Configuration Files Created

| File | Purpose |
|------|---------|
| `backend/.env` | Backend configuration (database, JWT, admin user) |
| `frontend/logbook-ui/src/environments/environment.prod.ts` | Production frontend config |
| `.env.example` | Template for environment variables |
| `.java-version` | Java version specification (25) |
| `setup.sh` | Automated setup script |

---

## 🔧 Important Configuration

### 1. Backend Environment (`backend/.env`)

**Current Settings:**
- **Database:** SQLite (development mode)
- **Admin User:** admin / admin123
- **JWT Secret:** Development key (change for production!)

**To customize:**
```bash
nano backend/.env
```

**Switch to PostgreSQL:**
Uncomment PostgreSQL lines and comment out SQLite lines in `backend/.env`

### 2. Admin Credentials

**⚠️ CHANGE BEFORE PRODUCTION!**

Edit `backend/.env`:
```env
ADMIN_USERNAME=your_username
ADMIN_PASSWORD=YourSecurePassword123!
ADMIN_EMAIL=your@email.com
```

### 3. QRZ API (Optional but Recommended)

For callsign lookups, add to `backend/.env`:
```env
QRZ_USERNAME=your_qrz_username
QRZ_PASSWORD=your_qrz_password
```

Get credentials at: https://www.qrz.com/

---

## 🏃 Quick Start Commands

**Start everything (Docker):**
```bash
docker-compose up -d
```

**Check status:**
```bash
docker-compose ps
```

**View logs:**
```bash
docker-compose logs -f backend
```

**Stop everything:**
```bash
docker-compose down
```

**Backend health check:**
```bash
curl http://localhost:8080/actuator/health
```

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| **QUICKSTART.md** | Complete beginner's guide (5 minutes) |
| **SETUP.md** | Comprehensive setup instructions |
| **BRANDING.md** | GEKHoosier branding guidelines and theme system |
| **CONFIGURATION.md** | Detailed configuration reference |
| **README.md** | Full system documentation |
| **RIG_CONTROL_GUIDE.md** | Rig control setup |
| **docs/USER_GUIDE.md** | How to use the system |

---

## ✨ First Time Usage

1. **Start the application** (choose method above)

2. **Open in browser:**
   - Docker: http://localhost
   - Local dev: http://localhost:4200

3. **Create your account:**
   - Click "Register here"
   - Fill in your details
   - Or login with admin credentials

4. **Create a logbook:**
   - Click "Create New Log"
   - Enter log details
   - Choose contest type (optional)

5. **Customize your experience:**
   - Toggle dark/light mode using the theme button (☀️/🌙) in the navbar
   - Theme preference is saved automatically

6. **Start logging QSOs!**

---

## 🔍 Verify Installation

```bash
# Check Java version
java -version
# Should show: openjdk version "25.0.1" or higher

# Check Node version
node --version
# Should show: v24.x.x or higher

# Check Docker
docker --version
# Should show: Docker version 20.x or higher

# Check backend health (after starting)
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

---

## 🐛 Troubleshooting

### Java Version Issues

```bash
# Set Java 25
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home

# Add to ~/.zshrc to make permanent
echo 'export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home' >> ~/.zshrc
```

### Backend Won't Start

1. Check `backend/.env` exists
2. Verify Java 17+ is set
3. Check database configuration

### Docker Issues

```bash
# Rebuild containers
docker-compose down
docker-compose build
docker-compose up -d

# View logs
docker-compose logs -f
```

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill process (replace PID)
kill -9 <PID>
```

---

## 🎓 Learning Resources

- **Spring Boot:** https://spring.io/projects/spring-boot
- **Angular:** https://angular.io/
- **Hamlib:** https://hamlib.github.io/
- **ADIF Specification:** https://adif.org/

---

## 📞 Support

- **Issues:** Create issue on GitHub
- **Documentation:** See `docs/` folder
- **Rig Control:** See `RIG_CONTROL_GUIDE.md`

---

## ✅ Next Steps

1. **Configure backend/.env** with your preferences
2. **Choose deployment method** (Docker or Local)
3. **Start the application**
4. **Create your account**
5. **Start logging contacts!**

---

**GEKHoosier QSO Suite - 73 and happy logging from Indiana and beyond! 📻**
