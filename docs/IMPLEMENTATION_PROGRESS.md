# EtherWave Archive - Implementation Status

**Last Updated:** March 2026
**Overall Status:** Production Ready (v1.2.0)
**Backend Build:** SUCCESS — 230 tests passing (100%)

---

## Epic Summary

| # | Epic | Status | Notes |
|---|------|--------|-------|
| 1 | Core Logging System | Complete | CRUD, ADIF 3.1.4, Cabrillo, pagination |
| 2 | Geographic Visualization & Mapping | Complete | Leaflet, clustering, grids, heatmap, WebSocket |
| 3 | Advanced Filtering | Complete | 10 filter types, mobile bottom sheet |
| 4 | DXCC Management | Complete | CTY.DAT parser, 340+ entities, Spring Cache |
| 5 | Session & Location Management | Complete | Hierarchical fallback, 1-hour TTL |
| 6 | UI & UX | Complete | Dark/light theme, responsive, accessibility |
| 7 | Performance | Complete | Server-side clustering, distance caching, DXCC cache |
| 8 | Authentication & Security | Complete | JWT, BCrypt, username-only, route guards |
| 9 | Multi-User Collaboration | Complete | RBAC, invitations, station assignment |
| 10 | Contest Validation & Scoring | Complete | 7 validators, 230 total tests |
| 11 | Rig Control | Complete | Hamlib/rigctld, WebSocket, per-user containers |
| 12 | Log Type Separation | Complete | Personal/Shared, backend guard, two-section UI |

---

## Test Breakdown

| Suite | Tests | Status |
|-------|-------|--------|
| ARRL Field Day Validator | 76 | Passing |
| ARRL Sweepstakes Validator | 42 | Passing |
| CQ WW Validator | 30 | Passing |
| State QSO Party Validator | 27 | Passing |
| Winter Field Day Validator | 16 | Passing |
| POTA Validator | 14 | Passing |
| SOTA Validator | 13 | Passing |
| Integration Tests | 12 | Passing |
| **Total** | **230** | **100% Passing** |

Test database: H2 in-memory (`jdbc:h2:mem:testdb`). Switched from SQLite due to Hibernate 7.x `ObjectOptimisticLockingFailureException` issues with `jdbc:sqlite::memory:`. `spring.sql.init.mode=never` in test profile — Hibernate DDL handles schema creation.

---

## Completed Epics

### 1. Core Logging System

- Log CRUD with soft-delete
- QSO entry with 25+ fields
- ADIF 3.1.4 import and export
- Cabrillo export for contest submission
- Real-time QSO list with pagination and sorting

### 2. Geographic Visualization & Mapping

- Leaflet.js interactive map with dark and light themes
- Server-side adaptive clustering (10,000 QSO threshold, Haversine distance)
- Maidenhead grid overlay supporting 2, 4, 6, and 8 character precision
- Heatmap density visualization with adjustable radius
- Pie chart cluster markers (SVG-generated, multi-station)
- Recent QSO pulse animations (15-minute window)
- Contest overlay layers: CQ zones, ITU zones, ARRL sections, DXCC
- Map data export: GeoJSON, KML, CSV, ADIF
- WebSocket real-time map updates
- `MapController.setUserLocation()` uses `Authentication` parameter (not hardcoded `userId=1L`)
- `MapController` grid coverage endpoint correctly passes filters to `GridCoverageService.getGridCoverage()`
- `MapDataService.getFilteredQSOs()` is public, used by `GridCoverageService`
- `MapDataService.getOperatorLocation()` resolves user default location via `userRepository.findByCallsign()`
- `FullscreenMapViewComponent` subscribes to `LogService.currentLog$`, passes `[logId]` and `[filters]` to `<app-qso-map>`, wires filter/overlay changes to `QSOMapComponent`

### 3. Advanced Filtering

- 10 filter types: band, mode, station, operator, DXCC, date range, confirmed status, continent, state, exchange
- Active filter pills with individual removal
- Mobile bottom sheet with swipe gestures

### 4. DXCC Management

- CTY.DAT parser with 340+ entities
- Longest-match callsign prefix lookup
- Spring Cache for lookup performance

### 5. Session & Location Management

- Hierarchical location fallback: station -> user -> session -> manual
- Session temporary location with 1-hour TTL (in-memory)
- `LocationManagementService.updateUserLocation(String username, ...)` overload added alongside `updateUserLocation(Long userId, ...)`

### 6. UI & UX

- EtherWave Archive branding
- Dark and light themes with system-preference detection
- Full responsive design for desktop, tablet, and mobile
- Accessibility audit completed

### 7. Performance

- Server-side adaptive clustering (10,000 QSO threshold)
- Distance caching via `QSOLocation` entity
- DXCC Spring Cache

### 8. Authentication & Security

- JWT authentication with 24-hour expiry
- BCrypt password hashing
- Username-only registration — no email field anywhere in the auth system
- Admin user bootstrap via environment variables
- Angular route guards

### 9. Multi-User Collaboration

- CREATOR / STATION / VIEWER RBAC per log
- In-app invitation system by username or callsign (no email)
- Participant management and station assignment (stations 1-10 plus GOTA)
- Personal-to-Shared log conversion
- `permissions.service.ts` uses `log.userRole` for RBAC — `isCreator` checks both `log.creatorId` and `log.userRole === CREATOR`; `isOperator` checks `log.userRole === STATION`

### 10. Contest Validation & Scoring

- Plugin-based validator architecture with JSON contest config files
- 7 validators: ARRL Field Day, POTA, SOTA, Winter Field Day, CQ WW, ARRL Sweepstakes, State QSO Party
- Contest config JSONs in `backend/src/main/resources/contest-configs/` (all 7 present)
- Real-time scoring and multiplier tracking
- `ScoringService.calculateBonusPoints()` reads `bonus_points` from contest config JSON, applies counts from `Log.bonusMetadata`
- `Log.bonusMetadata` TEXT column stores a JSON map of bonus_key to count/flag integer
- `LogResponse.bonusMetadata` exposed; `LogRequest.bonusMetadata` accepted in `updateLog()`
- Duplicate detection service

### 11. Rig Control

- Hamlib/rigctld TCP socket integration
- Real-time frequency and mode updates via WebSocket
- Per-user Docker containers
- Frontend rig-status panel

### 12. Log Type Separation (Personal / Shared)

- Backend guard: personal logs are always private
- Two create buttons in the UI (Personal and Shared)
- Two-section log list
- Conditional form fields in the create/edit modal

---

## In Progress

| Feature | Status | Notes |
|---------|--------|-------|
| Saved filter presets | In progress | localStorage-backed, frontend only |

---

## Remaining Backlog (Not Started)

| Feature | Priority |
|---------|----------|
| QSO confirmation via LoTW / eQSL / QRZ | Medium |
| Award tracking (DXCC, WAS, VUCC) | Medium |
| DX cluster spotting | Medium |
| Propagation prediction overlays | Low |
| QSL card generation | Low |
| Frontend unit tests | Low |
| E2E tests | Low |
| Offline mode | Low |

---

## Key Technical Notes

### Stack

- Java 25 LTS / Spring Boot 4.0.3 / Lombok 1.18.42
- Angular 21.2.0 standalone components / TypeScript 5.9.3
- PostgreSQL (production) / SQLite (field deployment)
- Testcontainers 1.21.3 / rest-assured 5.5.2 / sqlite-jdbc 3.49.1.0

### Docker

- Backend runtime: `eclipse-temurin:25-jre-noble` (Ubuntu Noble — no Alpine JRE for Java 25)
- Backend build: `maven:3.9-eclipse-temurin-25-alpine`
- Runtime installs `gosu` and `curl` via apt-get (not Alpine tools)
- Healthcheck uses `curl -sf http://localhost:8080/actuator/health`
- `docker-entrypoint.sh` detects root, chowns `/app/data`, drops to appuser via `gosu` (needed for field SQLite bind-mount)
- JVM flags: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError`
- Use `docker compose` (v5.1.0 plugin), not `docker-compose`

### Known Quirks

- Lombok 1.18.42 uses `sun.misc.Unsafe` internally on Java 25. Harmless; suppressed via `MAVEN_OPTS=--sun-misc-unsafe-memory-access=allow` in Dockerfile.
- `UriComponentsBuilder.fromHttpUrl()` was removed in Spring 7. Callsign lookup uses plain string concatenation with `URLEncoder.encode()`.
- `spring.sql.init.mode=never` in test profile prevents the SQLite-syntax `schema.sql` from running against H2.
- `spring.jpa.open-in-view=false` in test profile.
- PITest (mutation testing) does not support Java 25 — skip or ignore PITest failures.
- `station-color-preferences.service.ts` backend API is not implemented; localStorage fallback is active.
- `@CrossOrigin(origins = "*")` on `MapController` — production deployment should restrict this to the frontend origin.
