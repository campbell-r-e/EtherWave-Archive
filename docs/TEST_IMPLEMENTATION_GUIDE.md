#  Test Implementation Guide
## Ham Radio Contest Logbook System

This guide provides complete instructions, templates, and examples for implementing the full test suite.

---

##  Deliverables Summary

###  Completed
1. **Test Strategy Document** - `docs/TEST_STRATEGY.md`
2. **Backend Test Utilities**:
   - `TestDataBuilder.java` - Realistic ham radio test data builders
   - `BaseIntegrationTest.java` - Testcontainers setup
   - `JwtTestUtil.java` - JWT token generation for tests
3. **Backend Test Dependencies** - Added to `pom.xml`:
   - Testcontainers (PostgreSQL)
   - Spring Security Test
   - RestAssured
   - H2 Database
4. **Example Tests**:
   - `QSOServiceTest.java` - Complete service test example

###  Templates Provided Below
- Controller tests (MockMvc)
- Validator tests
- Repository tests (Testcontainers)
- Security tests
- Frontend component tests
- Frontend service tests
- E2E tests (Playwright)
- CI/CD pipeline

---

##  Backend Testing Implementation

### Test Structure
```
backend/src/test/java/com/hamradio/logbook/
├── testutil/
│   ├── TestDataBuilder.java 
│   ├── BaseIntegrationTest.java 
│   └── JwtTestUtil.java 
├── controller/
│   ├── QSOControllerTest.java (template below)
│   ├── AuthControllerTest.java (template below)
│   ├── LogControllerTest.java
│   ├── ExportControllerTest.java
│   └── ImportControllerTest.java
├── service/
│   ├── QSOServiceTest.java 
│   ├── AuthServiceTest.java
│   ├── AdifImportServiceTest.java
│   └── AdifExportServiceTest.java
├── repository/
│   └── QSORepositoryTest.java (template below)
├── validation/
│   └── FieldDayValidatorTest.java (template below)
└── security/
    └── JwtUtilTest.java (template below)
```

---

##  Backend Test Templates

### Template 1: Controller Test (MockMvc Pattern)

**File**: `backend/src/test/java/com/hamradio/logbook/controller/QSOControllerTest.java`

```java
package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.service.QSOService;
import com.hamradio.logbook.testutil.JwtTestUtil;
import com.hamradio.logbook.testutil.TestDataBuilder;
import com.hamradio.logbook.util.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for QSOController REST endpoints
 */
@WebMvcTest(QSOController.class)
@Import(JwtTestUtil.class)
@DisplayName("QSO Controller Tests")
class QSOControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QSOService qsoService;

    @MockBean
    private JwtUtil jwtUtil;

    private QSO testQSO;

    @BeforeEach
    void setUp() {
        testQSO = TestDataBuilder.aValidQSO(null, null)
                .id(1L)
                .build();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/qsos/{id} - Existing QSO - Returns 200 OK")
    void getQSOById_existingQSO_returns200() throws Exception {
        // Arrange
        when(qsoService.getQSOById(1L)).thenReturn(Optional.of(testQSO));

        // Act & Assert
        mockMvc.perform(get("/api/qsos/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.callsign").value("W1AW"))
                .andExpect(jsonPath("$.mode").value("SSB"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/qsos/{id} - Non-existent QSO - Returns 404")
    void getQSOById_nonExistent_returns404() throws Exception {
        // Arrange
        when(qsoService.getQSOById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/qsos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/qsos - Valid QSO - Returns 201 Created")
    void createQSO_validInput_returns201() throws Exception {
        // Arrange
        when(qsoService.createQSO(any(QSO.class), any(Long.class)))
                .thenReturn(testQSO);

        String requestBody = objectMapper.writeValueAsString(testQSO);

        // Act & Assert
        mockMvc.perform(post("/api/qsos")
                        .param("logId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.callsign").value("W1AW"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/qsos - Invalid Callsign - Returns 400")
    void createQSO_invalidCallsign_returns400() throws Exception {
        // Arrange
        QSO invalidQSO = TestDataBuilder.aValidQSO(null, null)
                .callsign("") // Empty callsign
                .build();

        String requestBody = objectMapper.writeValueAsString(invalidQSO);

        // Act & Assert
        mockMvc.perform(post("/api/qsos")
                        .param("logId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/qsos - Unauthenticated - Returns 401")
    void createQSO_unauthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/qsos")
                        .param("logId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/qsos/{id} - Existing QSO - Returns 204")
    void deleteQSO_existingQSO_returns204() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/qsos/1"))
                .andExpect(status().isNoContent());
    }
}
```

**Key Points**:
- Use `@WebMvcTest` for controller layer only
- Mock all dependencies with `@MockBean`
- Use `@WithMockUser` for authenticated tests
- Test all HTTP methods (GET, POST, PUT, DELETE)
- Test all status codes (200, 201, 400, 401, 404)
- Use JSONPath for response validation

---

### Template 2: Repository Test (Testcontainers)

**File**: `backend/src/test/java/com/hamradio/logbook/repository/QSORepositoryTest.java`

```java
package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for QSORepository using real PostgreSQL database (Testcontainers)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("QSO Repository Tests")
class QSORepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private QSORepository qsoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Station testStation;
    private Log testLog;

    @BeforeEach
    void setUp() {
        testUser = entityManager.persist(TestDataBuilder.aValidUser().build());
        testStation = entityManager.persist(TestDataBuilder.aValidStation().build());
        testLog = entityManager.persist(TestDataBuilder.aValidLog(testUser).build());
        entityManager.flush();
    }

    @Test
    @DisplayName("findByLogId - Returns only QSOs from specified log")
    void findByLogId_returnsOnlyLogQSOs() {
        // Arrange
        Log anotherLog = entityManager.persist(TestDataBuilder.aValidLog(testUser)
                .logName("Another Log").build());

        QSO qso1 = entityManager.persist(TestDataBuilder.aValidQSO(testStation, testLog).build());
        QSO qso2 = entityManager.persist(TestDataBuilder.aValidQSO(testStation, testLog).build());
        QSO qso3 = entityManager.persist(TestDataBuilder.aValidQSO(testStation, anotherLog).build());
        entityManager.flush();

        // Act
        List<QSO> result = qsoRepository.findAllByLogId(testLog.getId());

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(QSO::getLog).allMatch(log -> log.getId().equals(testLog.getId()));
    }

    @Test
    @DisplayName("findByDateRange - Returns QSOs within date range")
    void findByDateRange_returnsQSOsInRange() {
        // Arrange
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        QSO qso1 = entityManager.persist(TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2025, 1, 15)).build());
        QSO qso2 = entityManager.persist(TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2024, 12, 31)).build()); // Outside range
        entityManager.flush();

        // Act
        List<QSO> result = qsoRepository.findByDateRange(start, end);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQsoDate()).isBetween(start, end);
    }

    @Test
    @DisplayName("findByCallsign - Returns QSOs for specific callsign")
    void findByCallsign_returnsMatchingQSOs() {
        // Arrange
        entityManager.persist(TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W1AW").build());
        entityManager.persist(TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("K2ABC").build());
        entityManager.flush();

        // Act
        List<QSO> result = qsoRepository.findByCallsign("W1AW");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCallsign()).isEqualTo("W1AW");
    }

    @Test
    @DisplayName("countByLogId - Returns correct count")
    void countByLogId_returnsCorrectCount() {
        // Arrange
        entityManager.persist(TestDataBuilder.aValidQSO(testStation, testLog).build());
        entityManager.persist(TestDataBuilder.aValidQSO(testStation, testLog).build());
        entityManager.persist(TestDataBuilder.aValidQSO(testStation, testLog).build());
        entityManager.flush();

        // Act
        long count = qsoRepository.countByLogId(testLog.getId());

        // Assert
        assertThat(count).isEqualTo(3);
    }
}
```

**Key Points**:
- Use `@DataJpaTest` for repository layer
- Use Testcontainers for real database testing
- Use `TestEntityManager` for setup
- Test custom queries (`@Query`)
- Test entity relationships
- Test filtering and pagination

---

### Template 3: Validator Test

**File**: `backend/src/test/java/com/hamradio/logbook/validation/FieldDayValidatorTest.java`

```java
package com.hamradio.logbook.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Field Day validator
 * Validates class (1A-40F) and ARRL section
 */
@DisplayName("Field Day Validator Tests")
class FieldDayValidatorTest {

    private FieldDayValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new FieldDayValidator();
        objectMapper = new ObjectMapper();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1A", "2A", "3A", "10A", "40F", "1B", "1C", "1D", "1E"})
    @DisplayName("validate - Valid Field Day Classes - Passes")
    void validate_validClasses_passes(String fdClass) {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", fdClass);
        contestData.put("section", "ORG");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(contestData.toString())
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0A", "41A", "1Z", "ABC", ""})
    @DisplayName("validate - Invalid Field Day Classes - Fails")
    void validate_invalidClasses_fails(String fdClass) {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", fdClass);
        contestData.put("section", "ORG");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(contestData.toString())
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("class");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ORG", "SCV", "LAX", "NTX", "STX", "AL", "KY"})
    @DisplayName("validate - Valid ARRL Sections - Passes")
    void validate_validSections_passes(String section) {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", "2A");
        contestData.put("section", section);

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(contestData.toString())
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Missing Class - Fails")
    void validate_missingClass_fails() {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("section", "ORG");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(contestData.toString())
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("class");
    }

    @Test
    @DisplayName("validate - Missing Section - Fails")
    void validate_missingSection_fails() {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", "2A");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(contestData.toString())
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("section");
    }
}
```

**Key Points**:
- Test all validation rules
- Use `@ParameterizedTest` for multiple test cases
- Test both valid and invalid inputs
- Test edge cases and boundary values

---

##  Frontend Testing Implementation

### Frontend Test Structure
```
frontend/logbook-ui/src/app/
├── components/
│   ├── qso-entry/
│   │   └── qso-entry.component.spec.ts (template below)
│   ├── login/
│   │   └── login.component.spec.ts
│   └── export-panel/
│       └── export-panel.component.spec.ts
├── services/
│   ├── api.service.spec.ts (template below)
│   ├── auth.service.spec.ts
│   └── websocket.service.spec.ts
└── guards/
    └── auth.guard.spec.ts
```

---

### Template 4: Frontend Component Test

**File**: `frontend/logbook-ui/src/app/components/qso-entry/qso-entry.component.spec.ts`

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { QsoEntryComponent } from './qso-entry.component';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log/log.service';
import { of, throwError } from 'rxjs';

describe('QsoEntryComponent', () => {
  let component: QsoEntryComponent;
  let fixture: ComponentFixture<QsoEntryComponent>;
  let mockApiService: jasmine.SpyObj<ApiService>;
  let mockLogService: jasmine.SpyObj<LogService>;

  beforeEach(async () => {
    mockApiService = jasmine.createSpyObj('ApiService', [
      'createQSO',
      'getStations',
      'getActiveContests',
      'lookupCallsign'
    ]);
    mockLogService = jasmine.createSpyObj('LogService', [
      'getCurrentLog',
      'currentLog$'
    ]);

    // Setup default mocks
    mockApiService.getStations.and.returnValue(of([{
      id: 1,
      callsign: 'W1ABC',
      stationName: 'Home Station',
      gridSquare: 'FN31pr',
      power: 100,
      antenna: 'Dipole'
    }]));

    mockApiService.getActiveContests.and.returnValue(of([]));
    mockLogService.currentLog$ = of({
      id: 1,
      logName: 'Test Log',
      isFrozen: false
    });

    await TestBed.configureTestingModule({
      imports: [QsoEntryComponent, FormsModule],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        { provide: LogService, useValue: mockLogService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(QsoEntryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should auto-populate UTC date and time on init', () => {
    // Arrange
    const now = new Date();
    const expectedDate = now.toISOString().split('T')[0];

    // Assert
    expect(component.qso.qsoDate).toBe(expectedDate);
    expect(component.qso.timeOn).toMatch(/^\d{2}:\d{2}:\d{2}$/);
  });

  it('should auto-calculate band from frequency', () => {
    // Arrange
    component.qso.frequencyKhz = 14250;

    // Act
    component.onFrequencyChange();

    // Assert
    expect(component.qso.band).toBe('20m');
  });

  it('should validate callsign format', () => {
    // Arrange
    component.qso.callsign = 'W1ABC';

    // Act
    const isValid = component.validateQSO();

    // Assert
    expect(isValid).toBeTrue();
  });

  it('should reject invalid callsign', () => {
    // Arrange
    component.qso.callsign = '';

    // Act
    const isValid = component.validateQSO();

    // Assert
    expect(isValid).toBeFalse();
    expect(component.errorMessage).toContain('Callsign');
  });

  it('should save QSO successfully', (done) => {
    // Arrange
    const mockQSO = { id: 1, callsign: 'W1AW', ...component.qso };
    mockApiService.createQSO.and.returnValue(of(mockQSO));
    mockLogService.getCurrentLog.and.returnValue({ id: 1, logName: 'Test' });

    // Act
    component.saveQSO();

    // Assert
    setTimeout(() => {
      expect(mockApiService.createQSO).toHaveBeenCalled();
      expect(component.successMessage).toContain('success');
      done();
    }, 100);
  });

  it('should handle save error', (done) => {
    // Arrange
    mockApiService.createQSO.and.returnValue(
      throwError(() => new Error('Network error'))
    );
    mockLogService.getCurrentLog.and.returnValue({ id: 1, logName: 'Test' });

    // Act
    component.saveQSO();

    // Assert
    setTimeout(() => {
      expect(component.errorMessage).toBeTruthy();
      done();
    }, 100);
  });

  it('should lookup callsign and populate fields', (done) => {
    // Arrange
    const mockCallsignInfo = {
      callsign: 'W1AW',
      name: 'Hiram Percy Maxim',
      state: 'CT',
      country: 'USA',
      gridSquare: 'FN31pr'
    };
    mockApiService.lookupCallsign.and.returnValue(of(mockCallsignInfo));
    component.qso.callsign = 'W1AW';

    // Act
    component.onCallsignChange();

    // Assert
    setTimeout(() => {
      expect(component.qso.name).toBe('Hiram Percy Maxim');
      expect(component.qso.state).toBe('CT');
      done();
    }, 100);
  });
});
```

**Key Points**:
- Use `ComponentFixture` and `TestBed`
- Mock all services with `jasmine.createSpyObj`
- Test component initialization
- Test user interactions
- Test form validation
- Test async operations

---

### Template 5: Frontend Service Test

**File**: `frontend/logbook-ui/src/app/services/api.service.spec.ts`

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';
import { QSO } from '../models/qso.model';

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

  afterEach(() => {
    httpMock.verify(); // Ensure no outstanding requests
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('QSO Operations', () => {
    it('should fetch QSOs with log filter', () => {
      // Arrange
      const mockResponse = {
        content: [{ id: 1, callsign: 'W1AW' }],
        totalElements: 1
      };

      // Act
      service.getQSOs(123, 0, 20).subscribe(response => {
        // Assert
        expect(response.content).toHaveSize(1);
        expect(response.content[0].callsign).toBe('W1AW');
      });

      // Assert HTTP request
      const req = httpMock.expectOne(
        '/api/qsos?logId=123&page=0&size=20'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should create QSO with correct payload', () => {
      // Arrange
      const newQSO: QSO = {
        callsign: 'K2ABC',
        frequencyKhz: 14250,
        mode: 'SSB',
        qsoDate: '2025-01-15',
        timeOn: '14:30:00',
        rstSent: '59',
        rstRcvd: '59'
      };

      // Act
      service.createQSO(newQSO, 123).subscribe(response => {
        expect(response.id).toBe(1);
      });

      // Assert
      const req = httpMock.expectOne('/api/qsos?logId=123');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newQSO);
      req.flush({ id: 1, ...newQSO });
    });

    it('should handle 404 error', () => {
      // Act
      service.getQSO(999).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          // Assert
          expect(error.status).toBe(404);
        }
      });

      // Assert
      const req = httpMock.expectOne('/api/qsos/999');
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('Export Operations', () => {
    it('should trigger ADIF export by log', () => {
      // Arrange
      spyOn(window, 'open');

      // Act
      service.exportAdifByLog(123);

      // Assert
      expect(window.open).toHaveBeenCalledWith(
        '/api/export/adif/log/123',
        '_blank'
      );
    });
  });
});
```

---

##  E2E Testing with Playwright

### Setup Playwright

```bash
cd frontend/logbook-ui
npm install -D @playwright/test
npx playwright install
```

### Playwright Config

**File**: `frontend/logbook-ui/playwright.config.ts`

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
  ],
  webServer: {
    command: 'npm start',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env.CI,
  },
});
```

### E2E Test Template

**File**: `frontend/logbook-ui/e2e/critical-paths.spec.ts`

```typescript
import { test, expect } from '@playwright/test';

test.describe('Critical User Journeys', () => {
  test('should complete full QSO workflow', async ({ page }) => {
    // Step 1: Register
    await page.goto('/register');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'Password123!');
    await page.fill('input[name="callsign"]', 'W1TEST');
    await page.click('button[type="submit"]');

    // Step 2: Login
    await expect(page).toHaveURL('/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'Password123!');
    await page.click('button[type="submit"]');

    // Step 3: Create Log
    await expect(page).toHaveURL('/dashboard');
    await page.click('button:has-text("Create Log")');
    await page.fill('input[name="logName"]', 'Field Day 2025');
    await page.click('button:has-text("Create")');

    // Step 4: Add QSO
    await page.fill('input[name="callsign"]', 'W1AW');
    await page.fill('input[name="frequencyKhz"]', '14250');
    await page.selectOption('select[name="mode"]', 'SSB');
    await page.click('button:has-text("Save QSO")');

    // Verify QSO appears in list
    await expect(page.locator('text=W1AW')).toBeVisible();
    await expect(page.locator('text=14250')).toBeVisible();
  });

  test('should export ADIF file', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'Password123!');
    await page.click('button[type="submit"]');

    // Navigate to export panel
    await page.click('text=Export');

    // Trigger download
    const downloadPromise = page.waitForEvent('download');
    await page.click('button:has-text("Download ADIF")');
    const download = await downloadPromise;

    // Verify filename
    expect(download.suggestedFilename()).toMatch(/\.adi$/);
  });

  test('should handle rig control updates', async ({ page }) => {
    await page.goto('/dashboard');

    // Mock WebSocket message
    await page.evaluate(() => {
      const event = new MessageEvent('message', {
        data: JSON.stringify({
          frequency: 7200000,
          mode: 'CW'
        })
      });
      window.dispatchEvent(event);
    });

    // Verify frequency updated
    await expect(page.locator('input[name="frequencyKhz"]')).toHaveValue('7200');
    await expect(page.locator('select[name="mode"]')).toHaveValue('CW');
  });
});
```

---

##  CI/CD Pipeline

### GitHub Actions Workflow

**File**: `.github/workflows/test.yml`

```yaml
name: Test Suite

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  backend-tests:
    name: Backend Tests
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Cache Maven packages
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Run Backend Tests
        run: |
          cd backend
          mvn clean test
        env:
          SPRING_PROFILES_ACTIVE: test

      - name: Generate Coverage Report
        run: |
          cd backend
          mvn jacoco:report

      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          files: ./backend/target/site/jacoco/jacoco.xml

  frontend-tests:
    name: Frontend Tests
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/logbook-ui/package-lock.json

      - name: Install Dependencies
        run: |
          cd frontend/logbook-ui
          npm ci

      - name: Run Frontend Tests
        run: |
          cd frontend/logbook-ui
          npm test -- --watch=false --code-coverage

      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          files: ./frontend/logbook-ui/coverage/lcov.info

  e2e-tests:
    name: E2E Tests
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Install Dependencies
        run: |
          cd frontend/logbook-ui
          npm ci
          npx playwright install --with-deps

      - name: Run E2E Tests
        run: |
          cd frontend/logbook-ui
          npx playwright test

      - name: Upload Playwright Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: frontend/logbook-ui/playwright-report/
```

---

##  Test Execution Commands

### Backend Tests
```bash
# Run all tests
cd backend
mvn clean test

# Run specific test class
mvn test -Dtest=QSOServiceTest

# Run with coverage
mvn clean test jacoco:report

# Skip tests during build
mvn clean package -DskipTests
```

### Frontend Tests
```bash
cd frontend/logbook-ui

# Run all tests (watch mode)
npm test

# Run once with coverage
npm test -- --watch=false --code-coverage

# Run specific test file
npm test -- --include='**/qso-entry.component.spec.ts'
```

### E2E Tests
```bash
cd frontend/logbook-ui

# Run all E2E tests
npx playwright test

# Run in headed mode
npx playwright test --headed

# Run specific test
npx playwright test critical-paths

# Debug mode
npx playwright test --debug
```

---

##  Next Steps

### Week 1: Backend Unit Tests
- [ ] Create all service tests (use QSOServiceTest as template)
- [ ] Create all controller tests (use QSOControllerTest as template)
- [ ] Create validator tests
- [ ] Target: 70% coverage

### Week 2: Backend Integration Tests
- [ ] Create repository tests with Testcontainers
- [ ] Create security integration tests
- [ ] Create ADIF import/export tests
- [ ] Target: 80% coverage

### Week 3: Frontend Unit Tests
- [ ] Create all component tests
- [ ] Create all service tests
- [ ] Target: 75% coverage

### Week 4: E2E Tests
- [ ] Set up Playwright
- [ ] Create critical path tests
- [ ] Create accessibility tests
- [ ] Target: 100% critical path coverage

### Week 5: CI/CD
- [ ] Configure GitHub Actions
- [ ] Set up code coverage reporting
- [ ] Configure test reporting

---

##  Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Testcontainers](https://www.testcontainers.org/)
- [Jasmine Documentation](https://jasmine.github.io/)
- [Playwright Documentation](https://playwright.dev/)
- [AssertJ Assertions](https://assertj.github.io/doc/)

---

**End of Test Implementation Guide**
