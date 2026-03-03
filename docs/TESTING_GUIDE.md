# Testing Guide - Ham Radio Logbook System

##  Test Coverage Overview

- **Backend (Java/Spring Boot)**: 230 tests — 100% passing
- **Frontend (Angular)**: No tests implemented yet
- **E2E**: No tests implemented yet
- **Rig Control**: No tests implemented yet

See [backend/TESTING.md](TESTING.md) for the full backend test breakdown.

---

##  Running Tests

### Backend Tests (Java/Spring Boot)

#### Run All Tests
```bash
cd backend
mvn clean test
```

#### Run Specific Test Class
```bash
mvn test -Dtest=QSOServiceTest
```

#### Run Tests with Coverage Report (JaCoCo)
```bash
mvn clean test jacoco:report
```
Coverage report will be generated at: `backend/target/site/jacoco/index.html`

#### Run Mutation Tests (PITest)
```bash
mvn org.pitest:pitest-maven:mutationCoverage
```
Mutation report will be generated at: `backend/target/pit-reports/index.html`

#### Run Specific Validator Suite
```bash
mvn test -Dtest="CQWWValidatorTest"
mvn test -Dtest="ARRLSweepstakesValidatorTest"
mvn test -Dtest="StateQSOPartyValidatorTest"
```

---

### Frontend Tests

Frontend tests are not yet implemented. When added, they will be run with:
```bash
cd frontend/logbook-ui
npm test
```

### E2E Tests

E2E tests are not yet implemented.

---

##  Coverage Reports

### Backend Coverage (JaCoCo)

**Minimum Thresholds:**
- Line Coverage: **85%**
- Branch Coverage: **80%**

**View Report:**
1. Run: `cd backend && mvn clean test jacoco:report`
2. Open: `backend/target/site/jacoco/index.html`

---

##  Mutation Testing

### Backend Mutation Testing (PITest)

**Run Mutation Tests:**
```bash
cd backend
mvn org.pitest:pitest-maven:mutationCoverage
```

**Note**: PITest does not yet support Java 25. Mutation testing will fail to run, but all 230 unit tests pass.

**Mutation Score Target**: **80%** (when PITest supports Java 25)

---

##  Test Categories

### Validation Tests (Backend)
Test contest-specific validation rules in isolation.

**Examples:**
- `FieldDayValidatorTest.java` - 76 tests for ARRL Field Day
- `POTAValidatorTest.java` - 14 tests for Parks on the Air
- `SOTAValidatorTest.java` - 13 tests for Summits on the Air
- `WinterFieldDayValidatorTest.java` - 16 tests for Winter Field Day
- `CQWWValidatorTest.java` - 30 tests for CQ World Wide DX
- `ARRLSweepstakesValidatorTest.java` - 42 tests for ARRL Sweepstakes
- `StateQSOPartyValidatorTest.java` - 27 tests for State QSO Parties

### Service Tests (Backend)
Test export/import logic.

**Examples:**
- `AdifExportServiceTest.java` - 7 tests for ADIF export
- `AdifImportServiceTest.java` - 3 tests for ADIF import
- `CabrilloExportServiceTest.java` - 2 tests for Cabrillo export

---

##  Test Quality Standards

All tests follow these standards:

### 1. AAA Pattern
```java
// Arrange - Set up test data
QSO qso = createTestQSO();

// Act - Execute the method
QSO result = qsoService.createQSO(qso, user);

// Assert - Verify the results
assertThat(result).isNotNull();
assertThat(result.getCallsign()).isEqualTo("W1AW");
```

### 2. Descriptive Test Names
```java
@Test
@DisplayName("createQSO - Null Callsign - Throws ValidationException")
void createQSO_nullCallsign_throwsValidationException() {
    // Test implementation
}
```

### 3. Realistic Test Data
All tests use authentic ham radio data:
- Valid callsign formats: W1AW, K2ABC, VE3ABC
- Real frequency ranges: 1.8-450 MHz
- Actual contest codes: ARRL-FD, WFD, POTA
- Proper RST reports: 59 (phone), 599 (CW)

### 4. Comprehensive Assertions
```java
assertThat(qso)
    .isNotNull()
    .satisfies(q -> {
        assertThat(q.getCallsign()).isEqualTo("W1AW");
        assertThat(q.getFrequencyKhz()).isBetween(14000L, 14350L);
        assertThat(q.getMode()).isIn("SSB", "CW", "FT8");
    });
```

---

##  CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run backend tests
        run: |
          cd backend
          docker run --rm -v $(pwd):/app -v ~/.m2:/root/.m2 -w /app \
            maven:3.9-eclipse-temurin-25-alpine mvn test --no-transfer-progress
```

---

##  Adding New Tests

### Backend Test Template
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Your Service Tests")
class YourServiceTest {

    @Mock
    private YourRepository repository;

    @InjectMocks
    private YourService service;

    @Test
    @DisplayName("methodName - scenario - expectedResult")
    void methodName_scenario_expectedResult() {
        // Arrange

        // Act

        // Assert
    }
}
```

### Frontend Test Template
```typescript
describe('YourComponent', () => {
  let component: YourComponent;
  let fixture: ComponentFixture<YourComponent>;
  let mockService: jasmine.SpyObj<YourService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('YourService', ['method']);

    await TestBed.configureTestingModule({
      imports: [YourComponent],
      providers: [{ provide: YourService, useValue: spy }]
    }).compileComponents();

    mockService = TestBed.inject(YourService) as jasmine.SpyObj<YourService>;
    fixture = TestBed.createComponent(YourComponent);
    component = fixture.componentInstance;
  });

  it('should do something', () => {
    // Arrange

    // Act

    // Assert
  });
});
```

---

##  Debugging Failing Tests

### Backend
```bash
# Run single test with debug output (via Docker)
docker run --rm -v $(pwd)/backend:/app -v ~/.m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-25-alpine mvn test -Dtest=YourTest -X --no-transfer-progress
```

---

##  Test Metrics

### Current Test Statistics
- **Backend Tests**: 230 (100% passing)
- **Frontend Tests**: Not yet implemented
- **E2E Tests**: Not yet implemented
- **Average Backend Execution Time**: ~20 seconds (via Docker)

### Coverage Goals
- **Line Coverage**: ≥85%
- **Branch Coverage**: ≥80%

---

##  Best Practices

1. **Test Independence**: Each test should run independently
2. **Fast Tests**: Keep unit tests under 100ms each
3. **Clear Naming**: Use descriptive test names
4. **One Assertion Per Concept**: Test one thing at a time
5. **Use Test Data Builders**: Create reusable test data factories
6. **Mock External Dependencies**: Use mocks for external services
7. **Test Edge Cases**: Include null, empty, boundary conditions
8. **Maintain Tests**: Update tests when code changes

---

##  Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Jasmine Documentation](https://jasmine.github.io/)
- [Playwright Documentation](https://playwright.dev/)
- [PITest Mutation Testing](https://pitest.org/)
- [Stryker Mutator](https://stryker-mutator.io/)
- [JaCoCo Code Coverage](https://www.jacoco.org/jacoco/)

---

##  Contributing

When adding new features:
1. Write tests FIRST (TDD approach)
2. Ensure all existing tests pass
3. Add edge case tests
4. Update this guide if needed
5. Verify coverage thresholds are met

---

##  Backend Test Suite: 230/230 Passing

All backend validators and export/import services have comprehensive test coverage. Frontend and E2E tests are planned for a future milestone.
