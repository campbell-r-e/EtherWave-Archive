#  Test Strategy - Ham Radio Contest Logbook System

## Executive Summary

This document outlines the comprehensive testing strategy for the Ham Radio Contest Logbook System, a multi-user web application for logging amateur radio contacts with real-time rig control, contest scoring, and ADIF/Cabrillo export capabilities.

**Current Test Coverage**: ~2% (1 basic frontend component test)
**Target Test Coverage**: 80%+ across all tiers
**Testing Approach**: Pyramid strategy with emphasis on unit tests, supported by integration and E2E tests

---

## 1. Testing Pyramid Strategy

```
                    ┌─────────────────┐
                    │   E2E Tests     │  10% - Full user workflows
                    │   (Playwright)  │
                    └─────────────────┘
                  ┌─────────────────────┐
                  │ Integration Tests   │  20% - API, WebSocket, DB
                  │ (Spring Boot Test)  │
                  └─────────────────────┘
              ┌───────────────────────────┐
              │     Unit Tests            │  70% - Components, Services
              │ (JUnit 5 + Jasmine)       │       Controllers, Validators
              └───────────────────────────┘
```

---

## 2. Testing Frameworks & Tools

### Backend Testing Stack
| Tool | Version | Purpose |
|------|---------|---------|
| **JUnit 5** | 5.11.0+ | Test framework |
| **Spring Boot Test** | 4.0.0 | Integration testing |
| **Mockito** | 5.x | Mocking framework |
| **AssertJ** | 3.x | Fluent assertions |
| **Testcontainers** | 1.19+ | Database testing (PostgreSQL) |
| **MockMvc** | 6.x | REST API testing |
| **WireMock** | 3.x | External API mocking |
| **H2** | In-memory | Fast unit tests |

### Frontend Testing Stack
| Tool | Version | Purpose |
|------|---------|---------|
| **Jasmine** | 5.12.1 | Test framework |
| **Karma** | 6.4.0 | Test runner |
| **Karma Coverage** | 2.2.0 | Code coverage |
| **@testing-library/angular** | Latest | Component testing utilities |

### E2E Testing Stack
| Tool | Version | Purpose |
|------|---------|---------|
| **Playwright** | Latest | E2E automation |
| **Axe-core** | Latest | Accessibility testing |

### CI/CD
| Tool | Purpose |
|------|---------|
| **GitHub Actions** | CI/CD pipeline |
| **SonarQube** | Code quality & coverage |
| **Allure** | Test reporting |

---

## 3. Backend Testing Strategy

### 3.1 Unit Tests (70% of backend tests)

#### **Controllers (11 controllers)**
**Test Pattern**: MockMvc + Service Mocking

**Coverage Areas**:
-  HTTP endpoint validation (GET, POST, PUT, DELETE)
-  Request/Response serialization
-  Input validation (@Valid, @RequestBody)
-  HTTP status codes (200, 201, 400, 401, 403, 404, 500)
-  Error handling (@ExceptionHandler)
-  Security (@PreAuthorize, JWT validation)
-  Pagination, sorting, filtering
-  CORS headers

**Example Test Structure**:
```java
@WebMvcTest(QSOController.class)
class QSOControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean QSOService qsoService;
    @MockBean JwtUtil jwtUtil;

    @Test
    void createQSO_validInput_returns201() { }
    @Test
    void createQSO_invalidCallsign_returns400() { }
    @Test
    void createQSO_unauthorized_returns401() { }
}
```

**Controllers to Test**:
1. `AuthController` - Registration, Login, Token refresh
2. `QSOController` - CRUD operations, filtering, pagination
3. `LogController` - Multi-user log management, freeze/unfreeze
4. `StationController` - Station CRUD
5. `ContestController` - Contest management
6. `ExportController` - ADIF/Cabrillo export
7. `ImportController` - ADIF import with file upload
8. `CallsignController` - Callsign validation & QRZ lookup
9. `InvitationController` - Log sharing & permissions
10. `OperatorController` - Operator management
11. `TelemetryController` - Rig telemetry WebSocket

#### **Services (11 services)**
**Test Pattern**: Mockito + Repository Mocking

**Coverage Areas**:
-  Business logic correctness
-  Data transformation
-  Error handling & exceptions
-  Transaction boundaries
-  External API integration (mocked)
-  Validation rules
-  Security checks

**Example Test Structure**:
```java
@ExtendWith(MockitoExtension.class)
class QSOServiceTest {
    @Mock QSORepository qsoRepository;
    @Mock LogRepository logRepository;
    @Mock QSOValidationService validationService;
    @InjectMocks QSOService qsoService;

    @Test
    void createQSO_validData_savesSuccessfully() { }
    @Test
    void createQSO_duplicateQSO_throwsException() { }
}
```

**Services to Test**:
1. `QSOService` - QSO CRUD, duplicate detection
2. `LogService` - Multi-tenant isolation, freeze logic
3. `AuthService` - JWT generation, password hashing
4. `AdifImportService` - ADIF parsing, data mapping
5. `AdifExportService` - ADIF generation
6. `CabrilloExportService` - Cabrillo format generation
7. `CallsignValidationService` - FCC/QRZ API integration
8. `QSOValidationService` - Contest-agnostic validation
9. `InvitationService` - Permission management
10. `DataInitializationService` - Seed data
11. `CustomUserDetailsService` - Spring Security integration

#### **Validators (5 validators)**
**Test Pattern**: Direct instantiation + edge cases

**Coverage Areas**:
-  Contest-specific rules
-  Exchange format validation
-  Required field checking
-  Edge cases & boundary values

**Example Test Structure**:
```java
class FieldDayValidatorTest {
    FieldDayValidator validator = new FieldDayValidator();

    @Test
    void validate_validClass_passes() { }
    @Test
    void validate_invalidSection_fails() { }
}
```

**Validators to Test**:
1. `FieldDayValidator` - Class (1A-40F), Section (71 ARRL sections)
2. `WinterFieldDayValidator` - Class, Section
3. `POTAValidator` - Park reference format (K-####)
4. `SOTAValidator` - Summit reference format (W#/#-###)
5. `ContestValidatorRegistry` - Validator lookup

#### **Repositories (10 repositories)**
**Test Pattern**: @DataJpaTest + Testcontainers (PostgreSQL)

**Coverage Areas**:
-  Custom queries (@Query)
-  Entity relationships
-  Pagination & sorting
-  Filtering & search
-  Multi-tenant isolation (log_id filtering)

**Example Test Structure**:
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class QSORepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired QSORepository qsoRepository;

    @Test
    void findByLogId_returnsOnlyLogQSOs() { }
}
```

**Repositories to Test**:
1. `QSORepository` - Log filtering, date range queries
2. `LogRepository` - User permissions
3. `StationRepository` - Callsign lookup
4. `ContestRepository` - Active contests
5. `UserRepository` - Authentication
6. `OperatorRepository` - Callsign uniqueness
7. `InvitationRepository` - Pending invitations
8. `LogParticipantRepository` - Role-based queries
9. `CallsignCacheRepository` - Cache expiry
10. `RigTelemetryRepository` - Latest telemetry

#### **Security Components**
**Test Pattern**: Integration tests with MockMvc

**Coverage Areas**:
-  JWT generation & validation
-  Token expiration
-  Authentication filter chain
-  Authorization rules (@PreAuthorize)
-  Password encryption (BCrypt)
-  CORS configuration

**Security to Test**:
1. `JwtUtil` - Token lifecycle
2. `JwtAuthenticationFilter` - Request filtering
3. `CustomUserDetailsService` - User loading
4. `SecurityConfig` - Endpoint protection
5. `AuthService` - Login/Register flows

---

### 3.2 Integration Tests (20% of backend tests)

#### **API Integration Tests**
**Test Pattern**: @SpringBootTest + MockMvc + Real DB (Testcontainers)

**Test Scenarios**:
1. **End-to-end QSO workflow**:
   - Register user → Login → Create log → Add QSO → Export ADIF
2. **Multi-user collaboration**:
   - User A creates log → Invites User B → User B accepts → Both log QSOs
3. **Log freeze workflow**:
   - Add QSOs → Freeze log → Attempt edit (should fail) → Unfreeze
4. **Contest scoring**:
   - Add Field Day QSOs → Calculate score → Verify multipliers

#### **Database Integration Tests**
**Test Pattern**: Testcontainers PostgreSQL + SQLite

**Test Scenarios**:
1. **PostgreSQL mode** (production):
   - Full schema migration (Flyway/Liquibase)
   - Complex queries
   - Transaction rollback
2. **SQLite mode** (field deployment):
   - Verify SQLite compatibility
   - Test offline operation
   - Data portability

#### **External API Integration Tests**
**Test Pattern**: WireMock for external services

**APIs to Mock**:
1. **QRZ.com API** - Callsign lookup
   - Mock XML responses
   - Handle rate limiting
   - Test error scenarios (invalid callsign, API down)
2. **Hamlib/rigctld** - Rig control
   - Mock rigctld responses
   - Test frequency/mode updates
   - Handle connection failures

---

### 3.3 WebSocket Tests

**Test Pattern**: Spring WebSocket Test + StompJS

**Coverage Areas**:
-  Connection establishment
-  Authentication (JWT in handshake)
-  Message publishing
-  Subscription handling
-  Broadcast to multiple clients
-  Disconnection handling

**WebSocket Endpoints to Test**:
1. `/ws/rig-updates` - Rig frequency/mode updates
2. `/topic/rig-status` - Status broadcasts

---

## 4. Frontend Testing Strategy

### 4.1 Component Unit Tests (60% of frontend tests)

#### **Component Testing Pattern**
**Framework**: Jasmine + TestBed

**Coverage Areas**:
-  Component initialization
-  Input/Output bindings
-  Template rendering
-  Event handling
-  Form validation
-  Lifecycle hooks (ngOnInit, ngOnDestroy)
-  Conditional rendering (@if, @for)

**Example Test Structure**:
```typescript
describe('QsoEntryComponent', () => {
  let component: QsoEntryComponent;
  let fixture: ComponentFixture<QsoEntryComponent>;
  let mockApiService: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    mockApiService = jasmine.createSpyObj('ApiService', ['createQSO', 'getStations']);
    TestBed.configureTestingModule({
      imports: [QsoEntryComponent],
      providers: [{ provide: ApiService, useValue: mockApiService }]
    });
    fixture = TestBed.createComponent(QsoEntryComponent);
    component = fixture.componentInstance;
  });

  it('should auto-populate UTC time on init', () => { });
  it('should validate callsign format', () => { });
});
```

**Components to Test** (13 components):
1. **Auth Components**:
   - `LoginComponent` - Form validation, JWT storage, error handling
   - `RegisterComponent` - Password strength, callsign validation
2. **Dashboard**:
   - `DashboardComponent` - Layout, log selection state
3. **QSO Management**:
   - `QsoEntryComponent` - Form validation, UTC time, frequency-to-band calc
   - `QsoListComponent` - Pagination, filtering, sorting
4. **Log Management**:
   - `LogSelectorComponent` - Log switching, creation modal
   - `InvitationsComponent` - Accept/decline invitations
5. **Station & Contest**:
   - `StationManagementComponent` - CRUD operations
   - `ContestSelectionComponent` - Contest-specific fields
6. **Rig Control**:
   - `RigStatusComponent` - WebSocket updates, frequency display
7. **Import/Export**:
   - `ImportPanelComponent` - File upload, progress display
   - `ExportPanelComponent` - Format selection, download
8. **Visualization**:
   - `MapVisualizationComponent` - Leaflet map rendering

### 4.2 Service Unit Tests (30% of frontend tests)

#### **Service Testing Pattern**
**Framework**: Jasmine + HttpClientTestingModule

**Coverage Areas**:
-  HTTP requests (GET, POST, PUT, DELETE)
-  Request headers (JWT token)
-  Query parameters
-  Error handling (HTTP 4xx, 5xx)
-  Response transformation
-  Observable chains

**Example Test Structure**:
```typescript
describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should fetch QSOs with log filter', () => {
    service.getQSOs(123).subscribe();
    const req = httpMock.expectOne('/api/qsos?logId=123');
    expect(req.request.method).toBe('GET');
  });
});
```

**Services to Test** (4 core services):
1. `ApiService` - All HTTP endpoints (~30 methods)
2. `AuthService` - JWT storage, login/logout, token refresh
3. `LogService` - Current log state, invitations
4. `WebSocketService` - STOMP connection, subscriptions

### 4.3 Guard & Interceptor Tests

**Guards to Test**:
1. `AuthGuard` - Redirect to login if unauthenticated

**Interceptors to Test**:
1. `JwtInterceptor` - Add Authorization header

---

## 5. End-to-End (E2E) Testing Strategy

### 5.1 E2E Framework: Playwright

**Why Playwright**:
-  Multi-browser (Chromium, Firefox, WebKit)
-  Auto-wait for elements
-  Screenshot/video recording
-  Network interception
-  Accessibility testing integration

### 5.2 E2E Test Scenarios (10% of total tests)

#### **Critical User Journeys**

**Journey 1: New User Registration → First QSO**
```gherkin
Given a new user visits the site
When they register with valid credentials
And log in successfully
And create a new log "Field Day 2025"
And select a station
And enter a QSO (W1AW, 14.250 MHz, SSB)
Then the QSO appears in the QSO list
And the log shows 1 QSO
```

**Journey 2: Multi-User Collaboration**
```gherkin
Given User A creates a log "Contest Log"
When User A invites User B (email)
And User B accepts the invitation
And both users log QSOs simultaneously
Then both see real-time updates via WebSocket
And the log shows QSOs from both operators
```

**Journey 3: Rig Control Integration**
```gherkin
Given a user has rig control enabled
When the rig frequency changes to 7.200 MHz
Then the QSO entry form auto-populates 7200 kHz
And the band auto-selects to "40m"
And the mode updates to "CW"
```

**Journey 4: Contest Workflow**
```gherkin
Given a user selects "Field Day" contest
When they enter 10 QSOs with class/section
And freeze the log
Then attempting to edit a QSO fails
And export to Cabrillo succeeds
And the Cabrillo file contains correct format
```

**Journey 5: ADIF Import/Export**
```gherkin
Given a user has an ADIF file with 50 QSOs
When they upload the file
Then all 50 QSOs import successfully
And they can export the log as ADIF
And the exported file matches the original
```

**Journey 6: Offline Field Deployment (SQLite)**
```gherkin
Given the system is running in SQLite mode
When the user logs 100 QSOs offline
Then all QSOs persist locally
And the user can export ADIF without internet
```

### 5.3 Accessibility Testing (Axe-core)

**WCAG 2.1 AA Compliance**:
-  Keyboard navigation (Tab, Enter, Esc)
-  Screen reader compatibility (aria-labels)
-  Color contrast ratios
-  Focus indicators
-  Form labels and error messages

---

## 6. Rig Control Service Testing

### 6.1 Unit Tests

**Components to Test**:
1. `HamlibService` - Hamlib command execution
2. `RigController` - REST endpoints
3. `TelemetryService` - Telemetry storage

### 6.2 Integration Tests

**Scenarios**:
1. Mock rigctld server responses
2. Test WebSocket broadcasting
3. Handle rig disconnection/reconnection

---

## 7. Test Data & Fixtures

### 7.1 Test Data Builders

**Backend Builders**:
```java
public class QSOBuilder {
    public static QSO.QSOBuilder aValidQSO() {
        return QSO.builder()
            .callsign("W1AW")
            .frequencyKhz(14250L)
            .mode("SSB")
            .qsoDate(LocalDate.now())
            .timeOn(LocalTime.now())
            .rstSent("59")
            .rstRcvd("59")
            .band("20m");
    }
}
```

**Frontend Mocks**:
```typescript
export const mockQSO: QSO = {
  id: 1,
  callsign: 'W1AW',
  frequencyKhz: 14250,
  mode: 'SSB',
  qsoDate: '2025-01-15',
  timeOn: '14:30:00',
  rstSent: '59',
  rstRcvd: '59',
  band: '20m'
};
```

### 7.2 Realistic Ham Radio Data

**Callsigns**: W1AW, K1ABC, N2XYZ, KD6ABC, etc.
**Frequencies**: 1.8-54 MHz (HF), 144-148 MHz (VHF), 420-450 MHz (UHF)
**Modes**: SSB, CW, FT8, FT4, RTTY, PSK31
**Bands**: 160m, 80m, 40m, 20m, 15m, 10m, 6m, 2m, 70cm
**Grid Squares**: FN31, EM12, DM43 (Maidenhead locator system)
**Contests**: ARRL Field Day, CQ WW DX, POTA, SOTA

---

## 8. Test Coverage Goals

### 8.1 Coverage Targets

| Component | Target | Critical Path |
|-----------|--------|---------------|
| Backend Controllers | 80% | 100% |
| Backend Services | 85% | 100% |
| Backend Repositories | 70% | 90% |
| Backend Validators | 90% | 100% |
| Frontend Components | 75% | 90% |
| Frontend Services | 85% | 100% |
| E2E Critical Paths | 100% | 100% |

### 8.2 Coverage Reporting

**Tools**:
- Backend: JaCoCo (integrated with SonarQube)
- Frontend: Istanbul (via karma-coverage)

**Reports**:
- HTML reports in `backend/target/site/jacoco/`
- Frontend coverage in `frontend/logbook-ui/coverage/`

---

## 9. Continuous Integration

### 9.1 GitHub Actions Pipeline

**Stages**:
1. **Build** - Compile backend + frontend
2. **Unit Tests** - Run all unit tests (parallel)
3. **Integration Tests** - Run DB tests (Testcontainers)
4. **E2E Tests** - Run Playwright (headless)
5. **Coverage** - Upload to SonarQube
6. **Quality Gate** - Block merge if <80% coverage

**Triggers**:
- On every pull request
- On merge to main
- Nightly full regression

### 9.2 Test Execution Time Goals

| Test Suite | Target Time |
|------------|-------------|
| Backend Unit Tests | <2 minutes |
| Frontend Unit Tests | <1 minute |
| Integration Tests | <5 minutes |
| E2E Tests | <10 minutes |
| **Total CI Pipeline** | **<20 minutes** |

---

## 10. Test Maintenance Strategy

### 10.1 Test Ownership

- **Backend Tests**: Backend developers own controller/service tests
- **Frontend Tests**: Frontend developers own component/service tests
- **E2E Tests**: QA team owns critical path tests
- **Flaky Tests**: Delete or fix within 48 hours

### 10.2 Test Review Process

- All new features require tests (enforced via CI)
- Code reviews must check test quality
- Monthly test health review (flaky test report)

---

## 11. Testing Anti-Patterns to Avoid

 **Don't**:
- Test framework internals (e.g., testing Spring's `@Autowired`)
- Test getters/setters only
- Use `Thread.sleep()` in tests (use proper waits)
- Share mutable state between tests
- Ignore flaky tests
- Test implementation details instead of behavior

 **Do**:
- Test public API contracts
- Use meaningful test names (`should...when...`)
- Keep tests independent and isolated
- Use test data builders
- Mock external dependencies
- Test error cases and edge cases

---

## 12. Risk Assessment

### High-Risk Areas (Require Extra Testing)

1. **Authentication/Authorization** - Security vulnerabilities
2. **Multi-User Log Access** - Data leakage between users
3. **Contest Validators** - Incorrect scoring/disqualification
4. **ADIF Import** - Data corruption from malformed files
5. **WebSocket Real-Time Updates** - Race conditions
6. **Rig Control** - Hardware integration failures

### Testing Priorities

**P0 (Must Have)**:
- Authentication flows
- QSO CRUD operations
- Multi-tenant isolation
- ADIF export

**P1 (Should Have)**:
- Contest validation
- Rig control integration
- WebSocket updates

**P2 (Nice to Have)**:
- Advanced filtering
- Map visualization
- Telemetry history

---

## 13. Test Metrics & KPIs

### 13.1 Quality Metrics

- **Code Coverage**: 80%+ overall
- **Test Pass Rate**: 100% (zero tolerance for failing tests in main)
- **Test Execution Time**: <20 minutes full suite
- **Flaky Test Rate**: <1%

### 13.2 Defect Metrics

- **Defect Escape Rate**: <5% (defects found in production)
- **Mean Time to Detect (MTTD)**: <24 hours
- **Mean Time to Repair (MTTR)**: <48 hours

---

## 14. Deliverables Timeline

| Week | Deliverable |
|------|-------------|
| Week 1 | Test infrastructure + Backend unit tests (50%) |
| Week 2 | Backend unit tests (100%) + Integration tests |
| Week 3 | Frontend unit tests (100%) |
| Week 4 | E2E tests + CI/CD pipeline |
| Week 5 | Test utilities + Documentation |
| Week 6 | Full regression + Coverage report |

---

## 15. Success Criteria

 **Test suite is successful when**:
1. 80%+ code coverage across all tiers
2. All critical user journeys covered by E2E tests
3. CI pipeline runs in <20 minutes
4. Zero flaky tests in main branch
5. All tests pass on pull requests
6. SonarQube quality gate passes
7. Security vulnerabilities tested (OWASP Top 10)
8. Accessibility WCAG 2.1 AA compliance verified

---

## 16. Resources & Training

### 16.1 Training Materials

- JUnit 5 best practices guide
- Spring Boot testing documentation
- Jasmine/Karma tutorial
- Playwright workshop

### 16.2 Tools & Environments

- Local test DB (Testcontainers)
- Shared test environment (staging)
- Mock external APIs (WireMock server)

---

## Conclusion

This test strategy provides comprehensive coverage of the Ham Radio Contest Logbook System across all tiers: unit, integration, and end-to-end. By following this plan, we will achieve:

- **High confidence** in code quality
- **Fast feedback** loops via CI
- **Reduced defects** in production
- **Better maintainability** through well-tested code
- **Regulatory compliance** (accessibility, security)

**Next Steps**: Begin implementation with backend unit tests (controllers and services), then frontend components, followed by E2E critical paths.

---

**Document Version**: 1.0
**Last Updated**: 2025-01-29
**Owner**: QA Engineering Team
**Review Cycle**: Quarterly
