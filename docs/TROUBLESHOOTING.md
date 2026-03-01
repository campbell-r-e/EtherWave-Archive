# Ham Radio Logbook - Comprehensive Troubleshooting Guide

Complete troubleshooting guide for developers and users of the Ham Radio Contest Logbook system.

## Table of Contents

1. [Quick Diagnostic Steps](#quick-diagnostic-steps)
2. [Build & Compilation Errors](#build--compilation-errors)
3. [Runtime Errors](#runtime-errors)
4. [Database Issues](#database-issues)
5. [Docker & Deployment Problems](#docker--deployment-problems)
6. [Rig Control Issues](#rig-control-issues)
7. [WebSocket & Real-Time Updates](#websocket--real-time-updates)
8. [Authentication & JWT Problems](#authentication--jwt-problems)
9. [Frontend Issues](#frontend-issues)
10. [Performance Problems](#performance-problems)
11. [Network & Connectivity](#network--connectivity)
12. [Diagnostic Tools & Commands](#diagnostic-tools--commands)

---

## Quick Diagnostic Steps

When encountering any issue, follow these steps first:

### 1. Verify System Requirements

```bash
# Check Java version
java -version
# Should show: openjdk version "25.0.1" or higher

# Check Maven version
mvn -version
# Should show: Apache Maven 3.9.x or higher

# Check Node version
node --version
# Should show: v24.x.x or higher

# Check Docker version (if using)
docker --version
# Should show: Docker version 24.x.x or higher
```

### 2. Check Service Status

```bash
# Backend health check
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","groups":["liveness","readiness"]}

# Frontend check
curl http://localhost:4200

# Docker services check
docker compose ps
```

### 3. Check Logs

```bash
# Backend logs (local development)
cd backend
mvn spring-boot:run
# Watch for errors in console output

# Docker logs
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f rig-control

# Tail specific number of lines
docker compose logs --tail=100 backend
```

### 4. Common Quick Fixes

**Clear and rebuild:**
```bash
# Backend
cd backend
mvn clean install

# Frontend
cd frontend/logbook-ui
rm -rf node_modules package-lock.json
npm install

# Docker
docker compose down
docker compose build --no-cache
docker compose up -d
```

---

## Build & Compilation Errors

### Error: "invalid target release: 25"

**Symptom:**
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile
(default-compile) on project logbook-backend: Fatal error compiling: invalid target release: 25
```

**Cause:** Maven is using an older Java version (not Java 25)

**Solution:**

1. **Verify Java 25 is installed:**
   ```bash
   java -version
   # Should show: openjdk version "25.0.1"
   ```

2. **Set JAVA_HOME explicitly:**
   ```bash
   # macOS (Homebrew Temurin 25)
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home

   # Linux
   export JAVA_HOME=/opt/jdk-25

   # Windows (PowerShell)
   $env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25"
   ```

3. **Verify Maven is using Java 25:**
   ```bash
   mvn -version
   # Should show: Java version: 25.0.1
   ```

4. **Rebuild:**
   ```bash
   mvn clean compile
   ```

**Alternative:** Use Maven wrapper which respects JAVA_HOME:
```bash
./mvnw clean compile
```

---

### Error: Lombok Annotations Not Working

**Symptom:**
```
[ERROR] cannot find symbol
  symbol:   variable log
  location: class com.hamradio.logbook.service.LogService

[ERROR] cannot find symbol
  symbol:   method getUsername()
  location: variable user of type com.hamradio.logbook.entity.User
```

**Cause:** Lombok annotation processor not configured for Java 25

**Solution:**

1. **Verify Lombok version in pom.xml:**
   ```xml
   <dependency>
       <groupId>org.projectlombok</groupId>
       <artifactId>lombok</artifactId>
       <version>1.18.38</version>
       <optional>true</optional>
   </dependency>
   ```

2. **Ensure Maven compiler plugin has annotation processor path:**
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <configuration>
           <annotationProcessorPaths>
               <path>
                   <groupId>org.projectlombok</groupId>
                   <artifactId>lombok</artifactId>
                   <version>1.18.38</version>
               </path>
           </annotationProcessorPaths>
           <compilerArgs>
               <arg>-parameters</arg>
           </compilerArgs>
       </configuration>
   </plugin>
   ```

3. **Clean and rebuild:**
   ```bash
   mvn clean compile -X
   # -X flag shows debug output to verify Lombok processing
   ```

4. **For IDE users:**

   **IntelliJ IDEA:**
   - Install Lombok plugin: Settings → Plugins → Search "Lombok" → Install
   - Enable annotation processing: Settings → Build → Compiler → Annotation Processors → Enable annotation processing
   - Invalidate caches: File → Invalidate Caches / Restart

   **Eclipse:**
   - Download lombok.jar from https://projectlombok.org/download
   - Run: `java -jar lombok.jar`
   - Select Eclipse installation and click Install
   - Restart Eclipse

---

### Error: "Package tools.jackson does not exist"

**Symptom:**
```
[ERROR] package tools.jackson.databind does not exist
[ERROR] cannot find symbol
  symbol:   class ObjectMapper
  location: package tools.jackson.databind
```

**Cause:** This error indicates Spring Boot 4.0.3 is trying to use Jackson 3.0 (tools.jackson) but dependencies are incorrect

**Solution:**

1. **Verify Spring Boot version:**
   ```xml
   <parent>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-parent</artifactId>
       <version>4.0.0</version>
   </parent>
   ```

2. **Don't manually specify Jackson version** - Spring Boot manages it:
   ```xml
   <!-- CORRECT - Let Spring Boot manage version -->
   <dependency>
       <groupId>com.fasterxml.jackson.core</groupId>
       <artifactId>jackson-databind</artifactId>
   </dependency>

   <!-- INCORRECT - Don't specify version manually -->
   <dependency>
       <groupId>com.fasterxml.jackson.core</groupId>
       <artifactId>jackson-databind</artifactId>
       <version>3.0.0</version> <!-- REMOVE THIS -->
   </dependency>
   ```

3. **Clean Maven repository cache:**
   ```bash
   rm -rf ~/.m2/repository/com/fasterxml/jackson
   mvn clean install
   ```

---

### Error: "Spring Security DaoAuthenticationProvider constructor error"

**Symptom:**
```
[ERROR] constructor DaoAuthenticationProvider in class
org.springframework.security.authentication.dao.DaoAuthenticationProvider cannot be applied to given types;
  required: org.springframework.security.core.userdetails.UserDetailsService
  found:    no arguments
```

**Cause:** Spring Security 7 changed DaoAuthenticationProvider API

**Solution:**

Update SecurityConfig.java:

```java
// OLD (Spring Security 6)
@Bean
public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(customUserDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
}

// NEW (Spring Security 7) - Constructor injection required
@Bean
public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider =
        new DaoAuthenticationProvider(customUserDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
}
```

---

### Error: "npm ERR! code ERESOLVE" (Frontend)

**Symptom:**
```
npm ERR! code ERESOLVE
npm ERR! ERESOLVE could not resolve
npm ERR! Found: @angular/core@21.0.1
npm ERR! Could not resolve dependency
```

**Cause:** Dependency version conflicts or outdated package-lock.json

**Solution:**

1. **Clear npm cache and reinstall:**
   ```bash
   cd frontend/logbook-ui

   # Remove existing installations
   rm -rf node_modules package-lock.json

   # Clear npm cache
   npm cache clean --force

   # Reinstall with legacy peer deps
   npm install --legacy-peer-deps
   ```

2. **If that doesn't work, use force:**
   ```bash
   npm install --force
   ```

3. **Verify Node version:**
   ```bash
   node --version
   # Should be v24.x.x or higher

   npm --version
   # Should be 10.x.x or higher
   ```

4. **Update Angular CLI globally:**
   ```bash
   npm install -g @angular/cli@21
   ```

---

## Runtime Errors

### Error: "Application failed to start" - Port Already in Use

**Symptom:**
```
***************************
APPLICATION FAILED TO START
***************************

Description:
Web server failed to start. Port 8080 was already in use.

Action:
Identify and stop the process that's listening on port 8080 or configure this
application to listen on another port.
```

**Solution:**

1. **Find process using port 8080:**
   ```bash
   # macOS/Linux
   lsof -i :8080

   # Or use netstat
   netstat -an | grep 8080

   # Windows (PowerShell)
   netstat -ano | findstr :8080
   ```

2. **Kill the process:**
   ```bash
   # macOS/Linux
   kill -9 <PID>

   # Or kill all Java processes
   pkill java

   # Windows (PowerShell)
   Stop-Process -Id <PID> -Force
   ```

3. **Or change port in application.properties:**
   ```properties
   server.port=8081
   ```

---

### Error: "Failed to configure a DataSource"

**Symptom:**
```
***************************
APPLICATION FAILED TO START
***************************

Description:
Failed to configure a DataSource: 'url' attribute is not specified and no embedded
datasource could be configured.

Reason: Failed to determine a suitable driver class
```

**Cause:** Database configuration missing or incorrect

**Solution:**

1. **For SQLite (Development):**

   Create/update `application.properties`:
   ```properties
   # SQLite configuration
   spring.datasource.url=jdbc:sqlite:hamradio.db
   spring.datasource.driver-class-name=org.sqlite.JDBC
   spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect

   # Hibernate settings
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=false
   ```

2. **For PostgreSQL (Production):**

   ```properties
   # PostgreSQL configuration
   spring.datasource.url=jdbc:postgresql://localhost:5432/hamradio_logbook
   spring.datasource.username=postgres
   spring.datasource.password=yourpassword
   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

   # Hibernate settings
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=false
   ```

3. **Verify database driver in pom.xml:**

   For SQLite:
   ```xml
   <dependency>
       <groupId>org.xerial</groupId>
       <artifactId>sqlite-jdbc</artifactId>
       <version>3.47.2.0</version>
   </dependency>
   <dependency>
       <groupId>org.hibernate.orm</groupId>
       <artifactId>hibernate-community-dialects</artifactId>
   </dependency>
   ```

   For PostgreSQL:
   ```xml
   <dependency>
       <groupId>org.postgresql</groupId>
       <artifactId>postgresql</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```

---

### Error: "JWT token is invalid or expired"

**Symptom:**
Frontend shows "Unauthorized" or user is logged out unexpectedly

**Cause:** JWT token expired or JWT_SECRET mismatch

**Solution:**

1. **Check JWT configuration in `.env` or application.properties:**
   ```properties
   # JWT secret must be at least 256 bits for HS512
   app.jwt.secret=YourSecretKeyMinimum256BitsForHS512AlgorithmMakeSureLongEnough

   # Expiration in milliseconds (86400000 = 24 hours)
   app.jwt.expiration-ms=86400000
   ```

2. **Verify secret length:**
   ```bash
   # Secret should be at least 32 characters for HS512
   echo -n "YourSecretKeyHere" | wc -c
   # Should output at least 32
   ```

3. **Clear browser localStorage and login again:**
   ```javascript
   // In browser console
   localStorage.clear();
   sessionStorage.clear();
   location.reload();
   ```

4. **Check JWT token validity in browser:**
   ```javascript
   // In browser console
   const token = localStorage.getItem('token');
   console.log('Token:', token);

   // Decode JWT (use jwt.io to decode and inspect)
   const payload = JSON.parse(atob(token.split('.')[1]));
   console.log('Expires:', new Date(payload.exp * 1000));
   console.log('Issued:', new Date(payload.iat * 1000));
   ```

---

## Database Issues

### Issue: SQLite Database Locked

**Symptom:**
```
org.sqlite.SQLiteException: [SQLITE_BUSY]  The database file is locked (database is locked)
```

**Cause:** Another process has the SQLite database file open, or previous connection wasn't closed properly

**Solution:**

1. **Close all connections to database:**
   ```bash
   # Kill all Java processes
   pkill java

   # Wait a moment, then restart
   mvn spring-boot:run
   ```

2. **Check for orphaned lock file:**
   ```bash
   # Look for .db-journal or .db-wal files
   ls -la hamradio.db*

   # Remove lock files (only if application is stopped!)
   rm hamradio.db-journal
   rm hamradio.db-wal
   ```

3. **For development, consider using PostgreSQL instead:**
   SQLite doesn't support concurrent writes well. For multi-user testing, use PostgreSQL.

---

### Issue: PostgreSQL Connection Refused

**Symptom:**
```
org.postgresql.util.PSQLException: Connection to localhost:5432 refused.
Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
```

**Solution:**

1. **Verify PostgreSQL is running:**
   ```bash
   # macOS
   brew services list | grep postgresql

   # Linux
   sudo systemctl status postgresql

   # Start if not running
   brew services start postgresql@16
   # or
   sudo systemctl start postgresql
   ```

2. **Test connection manually:**
   ```bash
   psql -h localhost -p 5432 -U postgres
   ```

3. **Check PostgreSQL is listening on correct port:**
   ```bash
   sudo lsof -i :5432
   # or
   sudo netstat -an | grep 5432
   ```

4. **Verify postgresql.conf allows TCP connections:**
   ```bash
   # Find config file
   psql -U postgres -c "SHOW config_file"

   # Edit config
   sudo nano /path/to/postgresql.conf

   # Ensure this line is uncommented:
   listen_addresses = 'localhost'  # or '*' for all interfaces

   # Restart PostgreSQL
   sudo systemctl restart postgresql
   ```

5. **Check pg_hba.conf allows password authentication:**
   ```bash
   sudo nano /path/to/pg_hba.conf

   # Add this line if not present:
   host    all             all             127.0.0.1/32            md5

   # Reload PostgreSQL
   sudo systemctl reload postgresql
   ```

---

### Issue: Database Schema Migration Errors

**Symptom:**
```
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException:
Unable to execute schema management to JDBC target [alter table users add column...]
```

**Cause:** Schema mismatch between code and database, or conflicting migration

**Solution:**

1. **For development, reset database:**

   **SQLite:**
   ```bash
   # Backup current database
   cp hamradio.db hamradio.db.backup

   # Delete database to recreate
   rm hamradio.db

   # Restart application - schema will be recreated
   mvn spring-boot:run
   ```

   **PostgreSQL:**
   ```bash
   # Drop and recreate database
   psql -U postgres -c "DROP DATABASE hamradio_logbook;"
   psql -U postgres -c "CREATE DATABASE hamradio_logbook;"

   # Restart application
   mvn spring-boot:run
   ```

2. **For production, use Flyway/Liquibase migrations:**

   Check migration version:
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;
   ```

3. **Manual schema fix (last resort):**
   ```bash
   # Connect to database
   sqlite3 hamradio.db  # or: psql -U postgres hamradio_logbook

   # Check schema
   .schema users

   # Manually alter table if needed
   ALTER TABLE users ADD COLUMN new_column VARCHAR(255);
   ```

---

## Docker & Deployment Problems

### Issue: "docker compose up" Fails to Start Services

**Symptom:**
```
ERROR: for backend  Cannot start service backend: driver failed programming external
connectivity on endpoint hamradio-backend: Error starting userland proxy: listen tcp4
0.0.0.0:8080: bind: address already in use
```

**Solution:**

1. **Find and stop conflicting service:**
   ```bash
   # Find process on port 8080
   lsof -i :8080

   # Kill it
   kill -9 <PID>

   # Or stop all Docker containers
   docker compose down
   docker stop $(docker ps -aq)
   ```

2. **Clean Docker resources:**
   ```bash
   # Remove stopped containers
   docker compose down

   # Remove all containers, networks, volumes
   docker compose down -v

   # Rebuild without cache
   docker compose build --no-cache

   # Start fresh
   docker compose up -d
   ```

---

### Issue: Docker Container Exits Immediately

**Symptom:**
```
backend exited with code 1
```

**Solution:**

1. **Check container logs:**
   ```bash
   # View logs
   docker compose logs backend

   # Follow logs in real-time
   docker compose logs -f backend

   # Last 100 lines
   docker compose logs --tail=100 backend
   ```

2. **Common causes in logs:**

   **Missing environment variable:**
   ```
   Error: JWT_SECRET environment variable not set
   ```
   Solution: Set in docker-compose.yml or .env file

   **Database connection failed:**
   ```
   Connection refused: postgres:5432
   ```
   Solution: Ensure postgres service starts before backend (use depends_on)

   **Port conflict:**
   ```
   Port 8080 already in use
   ```
   Solution: Change port mapping in docker-compose.yml

3. **Run container interactively to debug:**
   ```bash
   docker compose run backend /bin/bash

   # Inside container, check:
   env | grep JWT_SECRET
   ping postgres
   java -version
   ```

---

### Issue: Docker Build Fails - "unable to prepare context"

**Symptom:**
```
ERROR: failed to solve: failed to read dockerfile: failed to prepare context:
unable to evaluate symlinks in Dockerfile path: lstat /path/to/Dockerfile: no such file or directory
```

**Solution:**

1. **Verify Dockerfile exists:**
   ```bash
   ls -la backend/Dockerfile
   ls -la frontend/Dockerfile
   ls -la rig-control-service/Dockerfile
   ```

2. **Check docker-compose.yml build context:**
   ```yaml
   services:
     backend:
       build:
         context: ./backend  # Should point to directory containing Dockerfile
         dockerfile: Dockerfile
   ```

3. **Clean Docker build cache:**
   ```bash
   docker system prune -a
   docker compose build --no-cache
   ```

---

## Rig Control Issues

### Issue: rigctld Not Connecting to Radio

**Symptom:**
Rig status shows "Disconnected" or frequency/mode not updating

**Solution:**

1. **Verify radio is connected:**
   ```bash
   # macOS
   ls -la /dev/tty.* | grep -i usb

   # Linux
   ls -la /dev/ttyUSB* /dev/ttyACM*

   # Should show something like:
   # /dev/ttyUSB0 or /dev/tty.usbserial-xxxx
   ```

2. **Test rigctld manually:**
   ```bash
   # Start rigctld with your radio model
   rigctld -m 122 -r /dev/ttyUSB0 -s 9600 -vvvvv

   # Model 122 = Yaesu FT-817
   # Find your model: rigctl --list | grep "Yaesu"

   # In another terminal, test connection:
   telnet localhost 4532
   f  # Get frequency
   m  # Get mode
   ```

3. **Check permissions:**
   ```bash
   # Add user to dialout group (Linux)
   sudo usermod -a -G dialout $USER

   # Log out and back in for changes to take effect

   # Or change permissions temporarily
   sudo chmod 666 /dev/ttyUSB0
   ```

4. **Docker USB passthrough:**

   Update docker-compose.yml:
   ```yaml
   rig-control:
     devices:
       - "/dev/ttyUSB0:/dev/ttyUSB0"  # Linux
       # or
       - "/dev/tty.usbserial-AB123:/dev/ttyUSB0"  # macOS
     privileged: true  # May be needed for USB access
   ```

---

### Issue: Rig Control Service Can't Connect to rigctld

**Symptom:**
```
java.net.ConnectException: Connection refused (Connection refused)
```

**Solution:**

1. **Verify rigctld is running:**
   ```bash
   # Check if rigctld process is running
   ps aux | grep rigctld

   # Check if port 4532 is listening
   lsof -i :4532
   # or
   netstat -an | grep 4532
   ```

2. **Check rigctld host/port configuration:**

   In rig-control-service application.properties:
   ```properties
   rig.host=localhost  # or container name in Docker
   rig.port=4532
   ```

   For Docker networking:
   ```yaml
   services:
     rig-control:
       environment:
         - RIG_HOST=rigctld  # Service name
         - RIG_PORT=4532
   ```

3. **Test connection manually:**
   ```bash
   # From rig-control container
   docker compose exec rig-control bash

   # Test rigctld connection
   telnet rigctld 4532
   # or
   nc -vz rigctld 4532
   ```

---

## WebSocket & Real-Time Updates

### Issue: WebSocket Connection Fails

**Symptom:**
Frontend shows "WebSocket connection failed" or QSO updates don't appear in real-time

**Solution:**

1. **Check WebSocket endpoint:**

   Browser console should show:
   ```
   WebSocket connection to 'ws://localhost:8080/ws' failed
   ```

2. **Verify CORS configuration:**

   In WebSocketConfig.java:
   ```java
   @Override
   public void registerStompEndpoints(StompEndpointRegistry registry) {
       registry.addEndpoint("/ws")
               .setAllowedOrigins("http://localhost:4200")  // Frontend URL
               .withSockJS();
   }
   ```

3. **Test WebSocket manually:**

   Use browser console:
   ```javascript
   const socket = new SockJS('http://localhost:8080/ws');
   const stompClient = Stomp.over(socket);

   stompClient.connect({}, (frame) => {
       console.log('Connected:', frame);

       // Subscribe to test topic
       stompClient.subscribe('/topic/qsos/1', (message) => {
           console.log('Message received:', message.body);
       });
   });
   ```

4. **Check firewall/proxy:**

   WebSockets may be blocked by firewalls or proxies. Test direct connection:
   ```bash
   # Test if port is accessible
   telnet localhost 8080
   ```

---

### Issue: QSO Updates Not Broadcasting

**Symptom:**
New QSOs appear in database but don't show in real-time on other clients

**Solution:**

1. **Verify messagingTemplate is autowired:**

   In QSOService.java:
   ```java
   @Autowired
   private SimpMessagingTemplate messagingTemplate;

   public QSOResponse createQSO(...) {
       QSO qso = qsoRepository.save(buildQSO(request, log));
       QSOResponse response = toResponse(qso);

       // Broadcast to WebSocket topic
       messagingTemplate.convertAndSend(
           "/topic/qsos/" + logId,
           response
       );

       return response;
   }
   ```

2. **Check subscribers are listening to correct topic:**

   Frontend should subscribe with log ID:
   ```typescript
   this.websocketService
       .subscribe(`/topic/qsos/${this.currentLog.id}`)
       .subscribe((qso: QSOResponse) => {
           console.log('New QSO received:', qso);
           this.qsos.unshift(qso);
       });
   ```

3. **Enable WebSocket debug logging:**

   In application.properties:
   ```properties
   logging.level.org.springframework.messaging=DEBUG
   logging.level.org.springframework.web.socket=DEBUG
   ```

---

## Authentication & JWT Problems

### Issue: Login Returns 401 Unauthorized

**Symptom:**
```json
{
  "code": "UNAUTHORIZED",
  "message": "Bad credentials"
}
```

**Solution:**

1. **Verify credentials are correct:**
   ```bash
   # Check if user exists in database
   sqlite3 hamradio.db "SELECT username, email FROM users;"
   # or
   psql -U postgres hamradio_logbook -c "SELECT username, email FROM users;"
   ```

2. **Check password encoding:**

   Passwords should be BCrypt hashed. Test password:
   ```java
   // In Java code or test
   BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
   boolean matches = encoder.matches("plainPassword", "hashedPassword");
   System.out.println("Password matches: " + matches);
   ```

3. **Reset user password (development):**
   ```java
   // Create quick test controller method
   @GetMapping("/test/reset-password")
   public String resetPassword() {
       User user = userRepository.findByUsername("admin").orElseThrow();
       user.setPassword(passwordEncoder.encode("newpassword"));
       userRepository.save(user);
       return "Password reset";
   }
   ```

4. **Check AuthService login logic:**
   ```java
   public AuthResponse login(LoginRequest request) {
       // Should authenticate before generating token
       Authentication authentication = authenticationManager.authenticate(
           new UsernamePasswordAuthenticationToken(
               request.getUsername(),
               request.getPassword()
           )
       );
       // ... generate JWT
   }
   ```

---

### Issue: CORS Errors on API Calls

**Symptom:**
```
Access to XMLHttpRequest at 'http://localhost:8080/api/auth/login' from origin
'http://localhost:4200' has been blocked by CORS policy: Response to preflight
request doesn't pass access control check
```

**Solution:**

1. **Update CORS configuration:**

   In SecurityConfig.java or CorsConfig.java:
   ```java
   @Bean
   public CorsConfigurationSource corsConfigurationSource() {
       CorsConfiguration configuration = new CorsConfiguration();
       configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
       configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
       configuration.setAllowedHeaders(Arrays.asList("*"));
       configuration.setAllowCredentials(true);

       UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
       source.registerCorsConfiguration("/**", configuration);
       return source;
   }
   ```

2. **For Spring Security, ensure CORS is enabled:**
   ```java
   @Bean
   public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
       http
           .cors(cors -> cors.configurationSource(corsConfigurationSource()))
           .csrf(csrf -> csrf.disable())
           // ... rest of configuration
       return http.build();
   }
   ```

3. **Add @CrossOrigin on controllers (alternative):**
   ```java
   @RestController
   @RequestMapping("/api/auth")
   @CrossOrigin(origins = "http://localhost:4200")
   public class AuthController {
       // ...
   }
   ```

---

## Frontend Issues

### Issue: "Cannot find module '@angular/core'"

**Symptom:**
```
Error: Cannot find module '@angular/core'
```

**Solution:**

```bash
cd frontend/logbook-ui

# Clean install
rm -rf node_modules package-lock.json
npm cache clean --force
npm install

# If still failing, use legacy peer deps
npm install --legacy-peer-deps
```

---

### Issue: Page Shows Blank White Screen

**Symptom:**
Frontend loads but shows blank page with no errors in console

**Solution:**

1. **Check browser console for errors:**
   Press F12 → Console tab

2. **Check routing configuration:**

   In app.routes.ts:
   ```typescript
   export const routes: Routes = [
       { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
       { path: 'login', component: LoginComponent },
       { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
       { path: '**', redirectTo: '/dashboard' }
   ];
   ```

3. **Verify app bootstrapping:**

   In main.ts:
   ```typescript
   bootstrapApplication(AppComponent, appConfig)
       .catch(err => console.error('Bootstrap error:', err));
   ```

4. **Check for TypeScript errors:**
   ```bash
   cd frontend/logbook-ui
   npm run build
   # Look for compilation errors
   ```

---

### Issue: HTTP Interceptor Not Adding Authorization Header

**Symptom:**
API calls return 401 even though user is logged in

**Solution:**

1. **Verify interceptor is provided:**

   In app.config.ts:
   ```typescript
   export const appConfig: ApplicationConfig = {
       providers: [
           provideRouter(routes),
           provideHttpClient(
               withInterceptors([jwtInterceptor])  // Make sure this is included
           ),
           // ...
       ]
   };
   ```

2. **Check interceptor logic:**

   In jwt.interceptor.ts:
   ```typescript
   export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
       const token = localStorage.getItem('token');

       if (token) {
           req = req.clone({
               setHeaders: {
                   Authorization: `Bearer ${token}`
               }
           });
       }

       return next(req);
   };
   ```

3. **Test manually in browser console:**
   ```javascript
   const token = localStorage.getItem('token');
   console.log('Token:', token);

   fetch('http://localhost:8080/api/logs', {
       headers: {
           'Authorization': `Bearer ${token}`
       }
   })
   .then(r => r.json())
   .then(console.log);
   ```

---

## Performance Problems

### Issue: Slow Query Performance

**Symptom:**
Pages load slowly, especially QSO list with many entries

**Solution:**

1. **Enable query logging to identify slow queries:**

   In application.properties:
   ```properties
   # Show SQL queries
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.format_sql=true

   # Log slow queries (queries taking more than 1 second)
   spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=1000
   ```

2. **Add database indexes:**

   For QSO queries, ensure these indexes exist:
   ```sql
   CREATE INDEX idx_qso_log_date ON qsos(log_id, qso_date);
   CREATE INDEX idx_qso_log_callsign ON qsos(log_id, callsign);
   CREATE INDEX idx_qso_log_band_mode ON qsos(log_id, band, mode);
   ```

3. **Use pagination:**

   In QSORepository:
   ```java
   Page<QSO> findByLogId(Long logId, Pageable pageable);
   ```

   In controller:
   ```java
   @GetMapping("/qsos")
   public Page<QSOResponse> getQSOs(
       @RequestParam Long logId,
       @RequestParam(defaultValue = "0") int page,
       @RequestParam(defaultValue = "20") int size
   ) {
       Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
       return qsoService.getAllQSOs(logId, pageable);
   }
   ```

4. **Optimize queries - avoid N+1:**

   Use JOIN FETCH:
   ```java
   @Query("SELECT q FROM QSO q LEFT JOIN FETCH q.station LEFT JOIN FETCH q.operator WHERE q.log.id = :logId")
   List<QSO> findByLogIdWithDetails(@Param("logId") Long logId);
   ```

---

### Issue: Frontend Bundle Too Large

**Symptom:**
Initial page load is slow due to large bundle size

**Solution:**

1. **Enable production build optimizations:**
   ```bash
   cd frontend/logbook-ui
   ng build --configuration production

   # Check bundle sizes
   ls -lh dist/logbook-ui/browser/*.js
   ```

2. **Analyze bundle:**
   ```bash
   npm install --save-dev webpack-bundle-analyzer
   ng build --configuration production --stats-json
   npx webpack-bundle-analyzer dist/logbook-ui/browser/stats.json
   ```

3. **Lazy load routes:**

   In app.routes.ts:
   ```typescript
   export const routes: Routes = [
       {
           path: 'admin',
           loadComponent: () => import('./admin/admin.component')
               .then(m => m.AdminComponent)
       }
   ];
   ```

4. **Remove unused dependencies:**
   ```bash
   npm install -g depcheck
   depcheck
   # Remove unused packages shown
   ```

---

## Network & Connectivity

### Issue: Cannot Access Backend from Frontend in Docker

**Symptom:**
Frontend container cannot reach backend API

**Solution:**

1. **Use service names in Docker network:**

   In frontend environment.ts:
   ```typescript
   export const environment = {
       production: true,
       apiUrl: 'http://backend:8080/api'  // Use service name, not localhost
   };
   ```

2. **Or use environment variable:**

   In docker-compose.yml:
   ```yaml
   frontend:
       environment:
           - API_URL=http://backend:8080
   ```

3. **Ensure services are on same network:**

   In docker-compose.yml:
   ```yaml
   services:
       backend:
           networks:
               - hamradio-network
       frontend:
           networks:
               - hamradio-network

   networks:
       hamradio-network:
           driver: bridge
   ```

---

## Diagnostic Tools & Commands

### Backend Diagnostics

```bash
# Check Spring Boot Actuator health
curl http://localhost:8080/actuator/health

# Get detailed health (requires authentication)
curl -H "Authorization: Bearer <token>" http://localhost:8080/actuator/health

# Check all available metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Application info
curl http://localhost:8080/actuator/info

# Thread dump (debugging deadlocks)
curl http://localhost:8080/actuator/threaddump

# Heap dump (memory analysis)
curl http://localhost:8080/actuator/heapdump > heapdump.hprof
```

### Database Diagnostics

```bash
# SQLite
sqlite3 hamradio.db

# Check all tables
.tables

# Describe table structure
.schema users

# Query data
SELECT * FROM users;

# Check database file size
ls -lh hamradio.db

# Integrity check
PRAGMA integrity_check;
```

```bash
# PostgreSQL
psql -U postgres hamradio_logbook

# List tables
\dt

# Describe table
\d users

# Query data
SELECT * FROM users;

# Database size
SELECT pg_size_pretty(pg_database_size('hamradio_logbook'));

# Active connections
SELECT count(*) FROM pg_stat_activity WHERE datname = 'hamradio_logbook';

# Slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

### Docker Diagnostics

```bash
# Service status
docker compose ps

# Service logs
docker compose logs backend
docker compose logs -f --tail=100 backend

# Execute command in running container
docker compose exec backend bash

# Inspect container
docker inspect hamradio-backend

# Container resource usage
docker stats

# Network inspection
docker network ls
docker network inspect hamradiologbook_default

# Volume inspection
docker volume ls
docker volume inspect hamradiologbook_postgres_data

# Clean up unused resources
docker system prune
docker volume prune
```

### Network Diagnostics

```bash
# Test backend API
curl -v http://localhost:8080/actuator/health

# Test with authentication
TOKEN="your-jwt-token-here"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/logs

# Test WebSocket (using wscat)
npm install -g wscat
wscat -c ws://localhost:8080/ws

# Check port listening
lsof -i :8080
netstat -an | grep 8080

# DNS resolution
nslookup localhost
ping localhost

# Traceroute
traceroute localhost
```

---

## Getting Additional Help

If you're still experiencing issues after trying these solutions:

1. **Check Application Logs:**
   - Backend: Console output or application.log
   - Frontend: Browser Developer Console (F12)
   - Docker: `docker compose logs -f`

2. **Enable Debug Logging:**
   ```properties
   logging.level.root=INFO
   logging.level.com.hamradio=DEBUG
   logging.level.org.springframework=DEBUG
   logging.level.org.hibernate.SQL=DEBUG
   ```

3. **Collect Diagnostic Information:**
   - Java version: `java -version`
   - Maven version: `mvn -version`
   - Node version: `node --version`
   - Docker version: `docker --version`
   - Operating System: `uname -a`
   - Error logs and stack traces

4. **Check GitHub Issues:**
   https://github.com/campbell-r-e/Hamradiologbook/issues

5. **Create New Issue:**
   Include:
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details
   - Relevant logs/error messages
   - Screenshots if applicable

---

## Related Documentation

- [User Guide](USER_GUIDE.md) - How to use the system
- [Developer Guide](DEVELOPER_GUIDE.md) - Development setup and architecture
- [API Reference](API_REFERENCE.md) - API documentation
- [Database Schema](DATABASE_SCHEMA.md) - Database structure
- [UPGRADE_GUIDE.md](../UPGRADE_GUIDE.md) - Upgrading to Java 25 and Spring Boot 4.0.3
