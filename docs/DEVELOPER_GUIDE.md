# Ham Radio Logbook - Developer Guide

Complete technical guide for developers working on the Ham Radio Contest Logbook system.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Development Environment Setup](#development-environment-setup)
3. [Backend Architecture](#backend-architecture)
4. [Frontend Architecture](#frontend-architecture)
5. [Authentication & Authorization](#authentication--authorization)
6. [Multi-Tenant System](#multi-tenant-system)
7. [Contest Validation System](#contest-validation-system)
8. [Rig Control Integration](#rig-control-integration)
9. [WebSocket Communication](#websocket-communication)
10. [Database Design](#database-design)
11. [API Design](#api-design)
12. [Testing](#testing)
13. [Deployment](#deployment)
14. [Extending the System](#extending-the-system)

---

## Architecture Overview

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Browser                            │
│  ┌────────────────────────────────────────────────────┐     │
│  │              Angular 17 SPA                         │     │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐           │     │
│  │  │Components│ │ Services │ │  Guards  │           │     │
│  │  └──────────┘ └──────────┘ └──────────┘           │     │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐           │     │
│  │  │  Models  │ │Interceptor│ │  State   │           │     │
│  │  └──────────┘ └──────────┘ └──────────┘           │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                ↕ HTTP/REST                ↕ WebSocket
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                       │
│  ┌────────────────────────────────────────────────────┐     │
│  │           Web Layer (Controllers)                   │     │
│  │  @RestController │ @CrossOrigin │ Exception Handlers│     │
│  └────────────────────────────────────────────────────┘     │
│  ┌────────────────────────────────────────────────────┐     │
│  │          Security Layer (Spring Security)           │     │
│  │  JWT Filter │ Auth Entry Point │ User Details Service│   │
│  └────────────────────────────────────────────────────┘     │
│  ┌────────────────────────────────────────────────────┐     │
│  │          Service Layer (Business Logic)             │     │
│  │  LogService │ QSOService │ ValidationService │ etc. │     │
│  └────────────────────────────────────────────────────┘     │
│  ┌────────────────────────────────────────────────────┐     │
│  │       Repository Layer (Spring Data JPA)            │     │
│  │  Repositories │ Custom Queries │ Specifications     │     │
│  └────────────────────────────────────────────────────┘     │
│  ┌────────────────────────────────────────────────────┐     │
│  │          Domain Layer (Entities)                    │     │
│  │  @Entity classes │ Relationships │ Validation       │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                          ↕ JDBC/JPA
┌─────────────────────────────────────────────────────────────┐
│              Database (SQLite / PostgreSQL)                  │
│  Users │ Logs │ LogParticipants │ Invitations              │
│  QSOs │ Stations │ Operators │ Contests                     │
└─────────────────────────────────────────────────────────────┘
```

### Design Patterns

- **MVC Pattern**: Controller → Service → Repository architecture
- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: Separation of domain and transfer objects
- **Strategy Pattern**: Pluggable contest validators
- **Observer Pattern**: WebSocket for real-time updates
- **Singleton Pattern**: Services injected via dependency injection
- **Builder Pattern**: Complex object construction (QSO entities)

### Technology Choices

#### Why Spring Boot?
- Mature ecosystem for enterprise Java applications
- Excellent Spring Security integration for authentication
- Spring Data JPA reduces boilerplate database code
- Built-in WebSocket support for real-time features
- Spring Boot Actuator provides production-ready monitoring and health checks
- Large community and extensive documentation

#### Why Java 21?
- Latest LTS (Long-Term Support) release with extended support until 2029
- Virtual threads for improved concurrency (Project Loom)
- Pattern matching and record patterns for cleaner code
- Better performance and garbage collection improvements
- Modern language features while maintaining backward compatibility

#### Why Angular 21?
- **Latest Framework**: Angular 21.0.1 with cutting-edge features
- **Standalone Components**: Simpler architecture without NgModules
- **TypeScript 5.9**: Latest type safety with strict mode enabled
- **New Control Flow**: Modern `@if`, `@for`, `@switch` syntax (better performance)
- **Signals**: Fine-grained reactivity for better change detection
- **RxJS 7.8**: Reactive programming for real-time updates and state management
- **Enhanced Router**: Improved functional guards and lazy loading
- **Smaller Bundles**: Better tree-shaking with standalone components
- **Strong CLI Tooling**: Angular CLI for scaffolding and optimization

#### Why SQLite/PostgreSQL?
- SQLite: Zero-configuration, perfect for field deployment
- PostgreSQL: Scalable for production multi-user deployments
- Same JPA code works with both (database abstraction)

---

## Development Environment Setup

### Prerequisites

```bash
# Check versions
java --version    # Should be 21+ (LTS)
node --version    # Should be 18+
npm --version     # Should be 9+
git --version     # Should be 2.x
docker --version  # For rig control (optional)
```

**Important**: Java 21 is required. The project uses:
- Java 21 LTS (latest long-term support version)
- Spring Boot 3.2.0 (requires Java 17+, optimized for Java 21)
- Lombok 1.18.34 (Java 21 compatible)
- Hibernate 6.3 with Java 21 support

### Backend Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/campbell-r-e/Hamradiologbook.git
   cd Hamradiologbook/backend
   ```

2. **Configure Environment**
   ```bash
   cp .env.example .env
   # Edit .env with your settings
   ```

3. **Build Project**
   ```bash
   ./mvnw clean install
   ```

4. **Run Tests**
   ```bash
   ./mvnw test
   ```

5. **Start Backend**
   ```bash
   ./mvnw spring-boot:run
   ```

Backend will start on `http://localhost:8080`

### Frontend Setup

1. **Navigate to Frontend**
   ```bash
   cd ../frontend/logbook-ui
   ```

2. **Install Dependencies**
   ```bash
   npm install
   ```

3. **Run Development Server**
   ```bash
   ng serve
   ```

Frontend will start on `http://localhost:4200`

### Database Setup

**SQLite (Development)**
- Automatic: Database file created on first run
- Location: `backend/hamradio.db`
- No additional setup required

**PostgreSQL (Production)**
```bash
# Install PostgreSQL
sudo apt-get install postgresql

# Create database
sudo -u postgres createdb hamradio_logbook

# Update application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/hamradio_logbook
spring.datasource.username=postgres
spring.datasource.password=yourpassword
```

### Rig Control Setup (Optional)

```bash
cd ../rig-control-service
./mvnw clean install
docker build -t rig-control .
docker run -d --name rigctld \
  --device=/dev/ttyUSB0 \
  -p 4532:4532 \
  rig-control
```

---

## Backend Architecture

### Package Structure

```
com.hamradio.logbook/
├── config/                    # Configuration classes
│   ├── SecurityConfig.java    # Spring Security setup
│   ├── WebSocketConfig.java   # WebSocket configuration
│   └── CorsConfig.java        # CORS settings
│
├── controller/                # REST controllers
│   ├── AuthController.java    # Login/register endpoints
│   ├── LogController.java     # Log CRUD operations
│   ├── QSOController.java     # QSO operations
│   ├── InvitationController.java
│   ├── StationController.java
│   ├── ContestController.java
│   └── ExportController.java
│
├── dto/                       # Data Transfer Objects
│   ├── auth/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   └── AuthResponse.java
│   ├── log/
│   │   ├── LogRequest.java
│   │   ├── LogResponse.java
│   │   ├── InvitationRequest.java
│   │   └── InvitationResponse.java
│   ├── QSORequest.java
│   └── QSOResponse.java
│
├── entity/                    # JPA Entities
│   ├── User.java
│   ├── Log.java
│   ├── LogParticipant.java
│   ├── Invitation.java
│   ├── QSO.java
│   ├── Station.java
│   ├── Operator.java
│   └── Contest.java
│
├── repository/                # Spring Data repositories
│   ├── UserRepository.java
│   ├── LogRepository.java
│   ├── LogParticipantRepository.java
│   ├── InvitationRepository.java
│   ├── QSORepository.java
│   └── ContestRepository.java
│
├── service/                   # Business logic
│   ├── AuthService.java
│   ├── LogService.java
│   ├── InvitationService.java
│   ├── QSOService.java
│   ├── QSOValidationService.java
│   ├── ContestService.java
│   └── AdminInitializationService.java
│
├── actuator/                  # Spring Boot Actuator endpoints
│   ├── /health               # Health check endpoint
│   ├── /health/liveness      # Liveness probe
│   ├── /health/readiness     # Readiness probe
│   ├── /info                 # Application information
│   └── /metrics              # Application metrics
│
├── util/                      # Utility classes
│   ├── security/
│   │   ├── JwtUtil.java
│   │   └── JwtAuthenticationFilter.java
│   └── DateTimeUtil.java
│
└── validation/                # Contest validators
    ├── ContestValidator.java      # Interface
    ├── CqwwValidator.java
    ├── ArrlSweepstakesValidator.java
    ├── PotaValidator.java
    └── FieldDayValidator.java
```

### Core Services Explained

#### LogService

**Purpose**: Manages log lifecycle, permissions, and participants

**Key Methods**:
```java
public List<LogResponse> getLogsForUser(String username);
public LogResponse createLog(LogRequest request, String username);
public LogResponse updateLog(Long logId, LogRequest request, String username);
public void deleteLog(Long logId, String username);
public boolean canEdit(Log log, User user);
public boolean hasAccess(Log log, User user);
```

**Permission Logic**:
```java
public boolean canEdit(Log log, User user) {
    if (!log.isEditable()) return false;
    if (isCreator(log, user)) return true;

    return logParticipantRepository.findByLogAndUser(log, user)
        .map(p -> p.getActive() && p.canEdit())
        .orElse(false);
}
```

#### QSOService

**Purpose**: QSO CRUD operations with validation and permission checks

**Key Methods**:
```java
public QSOResponse createQSO(QSORequest request, Long logId, String username);
public QSOResponse updateQSO(Long id, QSORequest request, String username);
public void deleteQSO(Long id, String username);
public Page<QSOResponse> getAllQSOs(Long logId, int page, int size, String username);
```

**Permission Flow**:
```java
@Transactional
public QSOResponse createQSO(QSORequest request, Long logId, String username) {
    User user = getUserByUsername(username);
    Log log = getLogByIdOrThrow(logId);

    // Permission check
    if (!logService.canEdit(log, user)) {
        throw new SecurityException("Cannot edit this log");
    }

    // Freeze check
    if (!log.isEditable()) {
        throw new IllegalStateException("Log is frozen");
    }

    // Build and save QSO
    QSO qso = buildQSO(request, log);
    return toResponse(qsoRepository.save(qso));
}
```

#### InvitationService

**Purpose**: Manages invitation lifecycle

**Workflow**:
1. Creator sends invitation → Creates Invitation entity with PENDING status
2. Invitee accepts → Updates status to ACCEPTED, creates LogParticipant
3. Invitee declines → Updates status to DECLINED
4. Creator cancels → Updates status to CANCELLED

**Key Methods**:
```java
public InvitationResponse createInvitation(InvitationRequest request, String username);
public InvitationResponse acceptInvitation(Long invitationId, String username);
public InvitationResponse declineInvitation(Long invitationId, String username);
public InvitationResponse cancelInvitation(Long invitationId, String username);
```

---

## Monitoring and Observability

### Spring Boot Actuator

The system includes Spring Boot Actuator for production monitoring and health checks.

**Configured Endpoints**:

```properties
# application.properties
management.endpoints.web.exposure.include=health,info,metrics,env,loggers
management.endpoint.health.show-details=when-authorized
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
```

**Health Check Endpoints**:

```bash
# Main health endpoint
curl http://localhost:8080/actuator/health
# Response: {"status":"UP","groups":["liveness","readiness"]}

# Liveness probe - Is the application running?
curl http://localhost:8080/actuator/health/liveness
# Response: {"status":"UP"}

# Readiness probe - Can the application handle traffic?
curl http://localhost:8080/actuator/health/readiness
# Response: {"status":"UP"}

# Detailed health (requires authentication)
curl -H "Authorization: Bearer <token>" http://localhost:8080/actuator/health
# Response includes database status, disk space, etc.
```

**Metrics Endpoint**:

```bash
# Get all available metrics
curl http://localhost:8080/actuator/metrics

# Specific metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/http.server.requests
```

**Docker Health Checks**:

The backend Dockerfile includes health check integration:
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1
```

**Kubernetes Probes**:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

---

## Frontend Architecture

### Angular 21 Modern Features

#### Control Flow Syntax (New in Angular 17+)

Angular 21 uses the new control flow syntax for better performance and readability:

**Conditionals** - `@if` instead of `*ngIf`:
```html
<!-- Old way (*ngIf) -->
<div *ngIf="isLoggedIn">Welcome!</div>
<div *ngIf="error">{{ error }}</div>

<!-- New way (@if) -->
@if (isLoggedIn) {
  <div>Welcome!</div>
}
@if (error) {
  <div>{{ error }}</div>
}
```

**Loops** - `@for` instead of `*ngFor`:
```html
<!-- Old way (*ngFor) -->
<div *ngFor="let qso of qsos">{{ qso.callsign }}</div>

<!-- New way (@for) -->
@for (qso of qsos; track qso.id) {
  <div>{{ qso.callsign }}</div>
}
```

**Switch** - `@switch` instead of `*ngSwitch`:
```html
<!-- New way (@switch) -->
@switch (log.type) {
  @case ('PERSONAL') {
    <span>Personal Log</span>
  }
  @case ('SHARED') {
    <span>Shared Log</span>
  }
  @default {
    <span>Unknown</span>
  }
}
```

#### Standalone Components

All components use the standalone API (no NgModule needed):

```typescript
@Component({
  selector: 'app-login',
  standalone: true,  // No module required!
  imports: [ReactiveFormsModule, RouterModule, CommonModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  // Component logic
}
```

#### Functional Route Guards

Modern functional guards instead of class-based:

```typescript
// Old way (class-based)
export class AuthGuard implements CanActivate {
  canActivate(): boolean { /* ... */ }
}

// New way (functional) - used in this project
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};
```

**Note**: This project currently uses class-based guards for compatibility, but can be migrated to functional guards.

### Project Structure

```
src/app/
├── components/
│   ├── auth/
│   │   ├── login/
│   │   │   ├── login.component.ts
│   │   │   ├── login.component.html
│   │   │   └── login.component.css
│   │   └── register/
│   │
│   ├── dashboard/
│   │   ├── dashboard.component.ts
│   │   ├── dashboard.component.html
│   │   └── dashboard.component.css
│   │
│   ├── log/
│   │   ├── log-selector/
│   │   └── invitations/
│   │
│   ├── qso-entry/
│   ├── qso-list/
│   ├── rig-status/
│   ├── map-visualization/
│   ├── contest-selection/
│   ├── station-management/
│   └── export-panel/
│
├── models/
│   ├── auth/
│   │   ├── user.model.ts
│   │   ├── login-request.model.ts
│   │   └── auth-response.model.ts
│   ├── log.model.ts
│   ├── qso.model.ts
│   └── contest.model.ts
│
├── services/
│   ├── auth/
│   │   ├── auth.service.ts
│   │   ├── auth.guard.ts
│   │   └── jwt.interceptor.ts
│   ├── log/
│   │   └── log.service.ts
│   ├── qso.service.ts
│   ├── rig.service.ts
│   └── websocket.service.ts
│
├── guards/
│   ├── auth.guard.ts
│   └── role.guard.ts
│
├── app.config.ts
├── app.routes.ts
└── app.component.ts
```

### State Management

Using **BehaviorSubject** pattern for reactive state:

```typescript
@Injectable({providedIn: 'root'})
export class LogService {
    // State holders
    private currentLogSubject = new BehaviorSubject<Log | null>(null);
    private logsSubject = new BehaviorSubject<Log[]>([]);

    // Public observables
    public currentLog$ = this.currentLogSubject.asObservable();
    public logs$ = this.logsSubject.asObservable();

    // State setters
    setCurrentLog(log: Log | null): void {
        this.currentLogSubject.next(log);
        if (log) {
            localStorage.setItem('currentLogId', log.id.toString());
        }
    }

    // Components subscribe to state
    ngOnInit() {
        this.logService.currentLog$.subscribe(log => {
            this.currentLog = log;
        });
    }
}
```

### HTTP Interceptor Pattern

**JWT Automatic Injection**:

```typescript
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const token = this.authService.getToken();

        if (token) {
            req = req.clone({
                setHeaders: {
                    Authorization: `Bearer ${token}`
                }
            });
        }

        return next.handle(req).pipe(
            catchError((error: HttpErrorResponse) => {
                if (error.status === 401) {
                    this.authService.logout();
                    this.router.navigate(['/login']);
                }
                return throwError(() => error);
            })
        );
    }
}
```

### Route Guards

**Authentication Guard**:

```typescript
@Injectable({providedIn: 'root'})
export class AuthGuard implements CanActivate {
    canActivate(
        route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): boolean {
        const currentUser = this.authService.currentUserValue;

        if (currentUser) {
            return true;
        }

        // Redirect to login with return URL
        this.router.navigate(['/login'], {
            queryParams: { returnUrl: state.url }
        });
        return false;
    }
}
```

---

## Authentication & Authorization

### JWT Authentication Flow

```
1. User Login
   ↓
2. Backend validates credentials
   ↓
3. Backend generates JWT token
   JWT Payload: {
     "sub": "username",
     "iat": issuedAt,
     "exp": expiresAt,
     "roles": ["ROLE_USER"]
   }
   ↓
4. Frontend stores token in localStorage
   ↓
5. Frontend includes token in all requests:
   Authorization: Bearer <token>
   ↓
6. Backend validates token on each request
   ↓
7. Backend extracts username from token
   ↓
8. Backend performs authorization checks
```

### Backend Implementation

**JwtUtil**:
```java
@Component
public class JwtUtil {
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

**Security Filter Chain**:
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/ws/**").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/**").authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

### Permission System

**Three-Level Authorization**:

1. **User-Level**: Authenticated vs. Anonymous
2. **Role-Level**: ROLE_USER vs. ROLE_ADMIN
3. **Resource-Level**: Log permissions (CREATOR, STATION, VIEWER)

**Permission Checking Example**:
```java
// In QSOService
public QSOResponse createQSO(QSORequest request, Long logId, String username) {
    // Level 1: Must be authenticated (enforced by filter chain)

    // Level 2: Must be a regular user (implicit - all auth users)

    // Level 3: Resource-level permission check
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    Log log = logRepository.findById(logId)
        .orElseThrow(() -> new IllegalArgumentException("Log not found"));

    if (!logService.canEdit(log, user)) {
        throw new SecurityException("User cannot edit this log");
    }

    // Permission granted - proceed with operation
    ...
}
```

---

## Multi-Tenant System

### Architecture

**Single Database, Multi-Tenant Pattern**:
- One database for all users
- Data isolation via `log_id` foreign key
- All queries filtered by log context
- Cross-log data leakage prevented

### Entity Relationships

```
User ──┬── Log (creator)
       │
       └── LogParticipant ──── Log
                   │
                   └── QSO ──── Log
```

### Database Isolation

**Every data-bearing table has `log_id`**:

```sql
-- Example: QSOs table
CREATE TABLE qsos (
    id BIGINT PRIMARY KEY,
    log_id BIGINT NOT NULL,  -- Multi-tenant key
    callsign VARCHAR(20),
    frequency_khz DOUBLE,
    ...
    FOREIGN KEY (log_id) REFERENCES logs(id),
    INDEX idx_qso_log (log_id)  -- Performance
);
```

### Query Filtering

**All repository methods filter by log**:

```java
// QSORepository
@Query("SELECT q FROM QSO q WHERE q.log.id = :logId ORDER BY q.createdAt DESC")
Page<QSO> findByLogId(@Param("logId") Long logId, Pageable pageable);

@Query("SELECT q FROM QSO q WHERE q.log.id = :logId AND q.qsoDate BETWEEN :startDate AND :endDate")
List<QSO> findByLogIdAndDateRange(
    @Param("logId") Long logId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);
```

### Access Control

**LogService.hasAccess()**:
```java
public boolean hasAccess(Log log, User user) {
    // Public logs accessible to all
    if (log.getIsPublic()) {
        return true;
    }

    // Creator always has access
    if (log.getCreator().getId().equals(user.getId())) {
        return true;
    }

    // Check if user is active participant
    return logParticipantRepository.findByLogAndUser(log, user)
        .map(LogParticipant::getActive)
        .orElse(false);
}
```

---

## Contest Validation System

### Plugin Architecture

**Interface**:
```java
public interface ContestValidator {
    String getContestCode();
    ValidationResult validate(QSO qso, List<QSO> previousQSOs);
    int calculatePoints(QSO qso);
    boolean isDuplicate(QSO qso, List<QSO> previousQSOs);
}
```

**Implementation Example (CQ WW)**:
```java
@Component
public class CqwwValidator implements ContestValidator {
    @Override
    public String getContestCode() {
        return "CQWW";
    }

    @Override
    public ValidationResult validate(QSO qso, List<QSO> previousQSOs) {
        ValidationResult result = new ValidationResult();

        // Check required fields
        if (qso.getCqZone() == null) {
            result.addError("CQ Zone is required for CQ WW");
        }

        // Check band is allowed
        if (!isValidBand(qso.getBand())) {
            result.addError("Invalid band for CQ WW: " + qso.getBand());
        }

        // Check for duplicates
        if (isDuplicate(qso, previousQSOs)) {
            result.addWarning("Duplicate QSO on this band/mode");
        }

        return result;
    }

    @Override
    public int calculatePoints(QSO qso) {
        // CQ WW DX scoring logic
        if (qso.getCountry().equals("USA")) {
            // Same country contacts
            return 0;  // No points for same country in DX contest
        } else if (isInSameContinent(qso)) {
            return 1;  // 1 point for same continent
        } else {
            return 3;  // 3 points for different continent
        }
    }
}
```

### Validation Service

**QSOValidationService**:
```java
@Service
public class QSOValidationService {
    private final List<ContestValidator> validators;

    @Autowired
    public QSOValidationService(List<ContestValidator> validators) {
        this.validators = validators;
    }

    public ValidationResult validateQSO(QSO qso) {
        if (qso.getContest() == null) {
            return generalValidation(qso);
        }

        // Find contest-specific validator
        ContestValidator validator = validators.stream()
            .filter(v -> v.getContestCode().equals(qso.getContest().getContestCode()))
            .findFirst()
            .orElse(null);

        if (validator == null) {
            return generalValidation(qso);
        }

        // Get previous QSOs for dupe checking
        List<QSO> previousQSOs = qsoRepository.findByLogIdOrderByCreatedAt(
            qso.getLog().getId()
        );

        return validator.validate(qso, previousQSOs);
    }
}
```

### Adding New Contest Validator

1. **Create Validator Class**:
   ```java
   @Component
   public class MyContestValidator implements ContestValidator {
       // Implement interface methods
   }
   ```

2. **Spring Auto-Discovery**: Automatically detected via `@Component`

3. **Contest Definition**: Add JSON config in `resources/contest-definitions/`
   ```json
   {
     "contestCode": "MYCONTEST",
     "name": "My Contest",
     "requiredFields": ["exchange"],
     "validBands": ["160m", "80m", "40m", "20m", "15m", "10m"],
     "validModes": ["SSB", "CW", "RTTY"]
   }
   ```

4. **Done**: Validator available in contest selection

---

## Rig Control Integration

### Architecture

```
Radio ──USB──┐
             ↓
┌──────────────────────────┐
│  rigctld (Hamlib)        │  ← Docker container per user
│  - Listens on TCP 4532   │
│  - Polls radio           │
│  - Responds to commands  │
└──────────────────────────┘
             ↓ TCP Socket
┌──────────────────────────┐
│  Rig Control Service     │
│  - Connects to rigctld   │
│  - Polls frequency/mode  │
│  - Publishes via WS      │
└──────────────────────────┘
             ↓ WebSocket
┌──────────────────────────┐
│  Backend (Spring Boot)   │
│  - Receives rig updates  │
│  - Broadcasts to clients │
└──────────────────────────┘
             ↓ WebSocket
┌──────────────────────────┐
│  Frontend (Angular)      │
│  - Displays rig status   │
│  - Auto-fills QSO form   │
└──────────────────────────┘
```

### Rig Control Service

**Main Polling Loop**:
```java
@Service
public class RigControlService {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    @PostConstruct
    public void init() {
        connectToRigctld();
        startPolling();
    }

    private void connectToRigctld() {
        socket = new Socket(rigHost, rigPort);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Scheduled(fixedRate = 500)  // Poll every 500ms
    public void pollRig() {
        try {
            // Get frequency
            out.println("f");
            String frequency = in.readLine();

            // Get mode
            out.println("m");
            String mode = in.readLine();

            // Broadcast via WebSocket
            RigStatus status = new RigStatus(frequency, mode);
            messagingTemplate.convertAndSend("/topic/rig", status);

        } catch (IOException e) {
            handleDisconnect();
        }
    }
}
```

### Frontend Rig Service

```typescript
@Injectable({providedIn: 'root'})
export class RigService {
    private rigStatusSubject = new BehaviorSubject<RigStatus | null>(null);
    public rigStatus$ = this.rigStatusSubject.asObservable();

    constructor(private websocketService: WebSocketService) {
        this.subscribeToRigUpdates();
    }

    private subscribeToRigUpdates(): void {
        this.websocketService.subscribe('/topic/rig').subscribe(
            (status: RigStatus) => {
                this.rigStatusSubject.next(status);
            }
        );
    }

    getCurrentFrequency(): number | null {
        return this.rigStatusSubject.value?.frequency || null;
    }

    getCurrentMode(): string | null {
        return this.rigStatusSubject.value?.mode || null;
    }
}
```

---

## WebSocket Communication

### Backend Configuration

**WebSocketConfig**:
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");  // Prefix for subscriptions
        config.setApplicationDestinationPrefixes("/app");  // Prefix for messages
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:4200")
                .withSockJS();
    }
}
```

**Broadcasting QSO Updates**:
```java
@Service
public class QSOService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public QSOResponse createQSO(QSORequest request, Long logId, String username) {
        QSO qso = buildAndSaveQSO(request, logId);
        QSOResponse response = toResponse(qso);

        // Broadcast to all clients watching this log
        messagingTemplate.convertAndSend(
            "/topic/qsos/" + logId,
            response
        );

        return response;
    }
}
```

### Frontend WebSocket Service

```typescript
@Injectable({providedIn: 'root'})
export class WebSocketService {
    private stompClient: any;
    private connected = new BehaviorSubject<boolean>(false);

    constructor() {
        this.connect();
    }

    private connect(): void {
        const socket = new SockJS('http://localhost:8080/ws');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, () => {
            this.connected.next(true);
        });
    }

    subscribe(topic: string): Observable<any> {
        return new Observable(observer => {
            const subscription = this.stompClient.subscribe(topic, (message) => {
                observer.next(JSON.parse(message.body));
            });

            return () => subscription.unsubscribe();
        });
    }
}
```

**Component Usage**:
```typescript
export class QsoListComponent implements OnInit {
    ngOnInit(): void {
        const currentLog = this.logService.getCurrentLog();

        // Subscribe to QSO updates for current log
        this.websocketService
            .subscribe(`/topic/qsos/${currentLog.id}`)
            .subscribe((newQSO: QSOResponse) => {
                this.qsos.unshift(newQSO);  // Add to list
            });
    }
}
```

---

## Database Design

See [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) for complete entity relationship diagrams and schema definitions.

**Key Design Decisions**:

1. **Multi-Tenant with log_id**: All data tables reference a log
2. **Soft Deletes**: `active` flags instead of hard deletes
3. **Audit Timestamps**: `createdAt` and `updatedAt` on all entities
4. **Indexed Foreign Keys**: Performance for multi-tenant queries
5. **Enumerated Types**: Java enums for type safety, stored as strings in DB

---

## API Design

See [API_REFERENCE.md](API_REFERENCE.md) for complete endpoint documentation.

### RESTful Design Principles

1. **Resource-Based URLs**: `/api/logs`, `/api/qsos`, `/api/invitations`
2. **HTTP Verbs**: GET (read), POST (create), PUT (update), DELETE (delete)
3. **Status Codes**:
   - 200: Success
   - 201: Created
   - 204: No Content (successful delete)
   - 400: Bad Request (validation error)
   - 401: Unauthorized (not logged in)
   - 403: Forbidden (insufficient permissions)
   - 404: Not Found
   - 409: Conflict (duplicate, concurrent edit)
   - 500: Internal Server Error
4. **JSON Responses**: All endpoints return/accept JSON
5. **Pagination**: Use query params `?page=0&size=20`

### Endpoint Patterns

**CRUD Pattern**:
```
GET    /api/logs           - List all logs for user
POST   /api/logs           - Create new log
GET    /api/logs/{id}      - Get specific log
PUT    /api/logs/{id}      - Update log
DELETE /api/logs/{id}      - Delete log
```

**Nested Resources**:
```
GET    /api/logs/{id}/participants     - Get log participants
DELETE /api/logs/{id}/participants/{participantId} - Remove participant
POST   /api/logs/{id}/freeze           - Freeze log
POST   /api/logs/{id}/unfreeze         - Unfreeze log
```

**Query Parameters**:
```
GET /api/qsos?logId=123&page=0&size=20
GET /api/qsos/range?logId=123&startDate=2024-01-01&endDate=2024-01-31
GET /api/qsos/states?logId=123
```

---

## Testing

### Backend Testing

**Unit Tests** (Service Layer):
```java
@SpringBootTest
class LogServiceTest {
    @Autowired
    private LogService logService;

    @MockBean
    private LogRepository logRepository;

    @Test
    void testCreateLog() {
        LogRequest request = new LogRequest();
        request.setName("Test Log");
        request.setType(LogType.PERSONAL);

        LogResponse response = logService.createLog(request, "testuser");

        assertNotNull(response);
        assertEquals("Test Log", response.getName());
        assertEquals(LogType.PERSONAL, response.getType());
    }

    @Test
    void testCanEditAsCreator() {
        Log log = createTestLog();
        User creator = log.getCreator();

        assertTrue(logService.canEdit(log, creator));
    }

    @Test
    void testCannotEditFrozenLog() {
        Log log = createTestLog();
        log.setEditable(false);
        User creator = log.getCreator();

        assertFalse(logService.canEdit(log, creator));
    }
}
```

**Integration Tests** (Controller Layer):
```java
@SpringBootTest
@AutoConfigureMockMvc
class LogControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "testuser")
    void testGetMyLogs() throws Exception {
        mockMvc.perform(get("/api/logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateLog() throws Exception {
        String requestJson = """
            {
                "name": "Test Log",
                "type": "PERSONAL",
                "isPublic": false
            }
            """;

        mockMvc.perform(post("/api/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Log"));
    }
}
```

### Frontend Testing

**Unit Tests** (Jasmine/Karma):
```typescript
describe('LogService', () => {
    let service: LogService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [LogService]
        });

        service = TestBed.inject(LogService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should retrieve logs', () => {
        const mockLogs: Log[] = [
            { id: 1, name: 'Test Log', type: LogType.PERSONAL, ... }
        ];

        service.getMyLogs().subscribe(logs => {
            expect(logs.length).toBe(1);
            expect(logs[0].name).toBe('Test Log');
        });

        const req = httpMock.expectOne(`${service.baseUrl}`);
        expect(req.request.method).toBe('GET');
        req.flush(mockLogs);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
```

**E2E Tests** (Playwright/Cypress):
```typescript
describe('Log Management', () => {
    it('should create a new log', () => {
        cy.visit('/');
        cy.login('testuser', 'password');

        cy.get('[data-testid="log-dropdown"]').click();
        cy.get('[data-testid="create-log"]').click();

        cy.get('[data-testid="log-name"]').type('My Test Log');
        cy.get('[data-testid="log-type"]').select('PERSONAL');
        cy.get('[data-testid="create-button"]').click();

        cy.get('[data-testid="log-dropdown"]')
            .should('contain', 'My Test Log');
    });
});
```

---

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for complete production deployment guide.

**Quick Overview**:

1. **Docker Compose** (Development/Small Deployment):
   ```yaml
   version: '3.8'
   services:
     backend:
       build: ./backend
       ports:
         - "8080:8080"
       environment:
         - SPRING_PROFILES_ACTIVE=production

     frontend:
       build: ./frontend
       ports:
         - "80:80"

     db:
       image: postgres:15
       volumes:
         - postgres_data:/var/lib/postgresql/data
   ```

2. **Kubernetes** (Large Scale):
   - Backend deployment + service
   - Frontend deployment + ingress
   - PostgreSQL StatefulSet
   - Persistent volume claims

---

## Extending the System

### Adding a New Contest Validator

1. Create validator class implementing `ContestValidator`
2. Add `@Component` annotation for Spring auto-discovery
3. Implement required methods (validate, calculatePoints, isDuplicate)
4. Add contest definition JSON
5. Test with sample QSOs

### Adding a New API Endpoint

1. Add method to service layer
2. Create controller method with `@GetMapping/@PostMapping`
3. Define request/response DTOs
4. Add permission checks
5. Write unit and integration tests
6. Update API documentation

### Adding a New Frontend Component

1. Generate component: `ng generate component my-component`
2. Create standalone component with imports
3. Add to dashboard or create route
4. Implement service integration
5. Add styling with Bootstrap
6. Write unit tests

### Adding a New Database Entity

1. Create JPA entity class with `@Entity`
2. Define fields, relationships, indexes
3. Create repository interface
4. Create DTOs for API layer
5. Add service methods
6. Generate migration (Liquibase/Flyway)
7. Update documentation

---

## Code Style and Conventions

### Java (Backend)

- **Package naming**: Lowercase, descriptive (e.g., `com.hamradio.logbook.service`)
- **Class naming**: PascalCase (e.g., `LogService`, `QSOController`)
- **Method naming**: camelCase, verb-based (e.g., `createLog`, `hasAccess`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_PAGE_SIZE`)
- **Use Lombok**: `@Data`, `@Builder`, `@RequiredArgsConstructor`
- **Exception handling**: Specific exceptions, meaningful messages
- **Logging**: Use SLF4J with appropriate levels

### TypeScript (Frontend)

- **File naming**: kebab-case (e.g., `log-selector.component.ts`)
- **Class naming**: PascalCase (e.g., `LogSelectorComponent`)
- **Method naming**: camelCase (e.g., `selectLog`, `createInvitation`)
- **Interfaces**: PascalCase, descriptive (e.g., `Log`, `QSORequest`)
- **Observables**: Append `$` suffix (e.g., `currentLog$`)
- **Use async pipe**: Prefer over manual subscription management
- **Type safety**: Explicit types, avoid `any`

### Git Commit Messages

```
feat: Add support for POTA validation
fix: Correct duplicate detection for multi-band QSOs
docs: Update API documentation for invitation endpoints
refactor: Extract permission checking to LogService
test: Add integration tests for QSO controller
chore: Update dependencies to latest versions
```

---

## Performance Optimization

### Backend

1. **Database Indexing**: All foreign keys, frequently queried columns
2. **Query Optimization**: Use pagination, avoid N+1 queries
3. **Caching**: Consider Spring Cache for contest definitions
4. **Connection Pooling**: HikariCP configuration
5. **Async Operations**: Use `@Async` for non-blocking operations

### Frontend

1. **Lazy Loading**: Load components on demand
2. **OnPush Change Detection**: For performance-critical components
3. **Virtual Scrolling**: For long QSO lists
4. **Debouncing**: On search inputs and rig polling
5. **Bundle Optimization**: Tree-shaking, minification

---

## Security Best Practices

1. **Input Validation**: Validate all user input on backend
2. **SQL Injection Prevention**: Use parameterized queries (JPA does this)
3. **XSS Prevention**: Angular sanitizes by default
4. **CSRF Protection**: Stateless JWT architecture prevents CSRF
5. **Password Storage**: BCrypt with salt
6. **HTTPS**: Always use TLS in production
7. **Rate Limiting**: Implement on authentication endpoints
8. **Secrets Management**: Use environment variables, never commit secrets

---

## Resources

- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Angular Docs**: https://angular.io/docs
- **Hamlib**: https://hamlib.github.io/
- **ADIF Specification**: https://www.adif.org/
- **JWT**: https://jwt.io/
- **REST API Design**: https://restfulapi.net/

---

For additional help, see:
- [User Guide](USER_GUIDE.md)
- [API Reference](API_REFERENCE.md)
- [Database Schema](DATABASE_SCHEMA.md)
- [Deployment Guide](DEPLOYMENT.md)
