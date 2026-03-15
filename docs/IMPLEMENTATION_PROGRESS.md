# EtherWave Archive - Implementation Status

**Last Updated:** March 2026
**Overall Status:** Production Ready (v1.2.0)
**Backend Build:** SUCCESS — 238 tests passing (100%)
**Rig Service Build:** SUCCESS — 51 tests passing (100%)

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
| 10 | Contest Validation & Scoring | Complete | 7 validators, 7 contest config JSONs |
| 11 | Rig Control | Complete | Standalone microservice, multi-client broker, PTT locking + safety timeout, smart caching, cloud relay, WS auth, audit trail, Prometheus |
| 12 | Log Type Separation | Complete | Personal/Shared, backend guard, two-section UI |

---

## Test Breakdown

**Backend tests:**

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
| Station Gateway Handler | 8 | Passing |
| **Total** | **238** | **100% Passing** |

**Rig control service tests:**

| Suite | Tests | Status |
|-------|-------|--------|
| RigCommandDispatcherTest | 15 | Passing |
| PTTLockManagerTest | 15 | Passing |
| RigServiceTest | 11 | Passing |
| CloudRelayClientTest | 10 | Passing |
| **Total** | **51** | **100% Passing** |

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
- Saved filter presets (localStorage-backed; backend persistence is low priority backlog)

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

- Standalone rig control microservice (`rig-control-service/`) — separate Spring Boot app with its own Dockerfile
- Multi-client shared broker: one `rigctld` TCP connection serves multiple simultaneous WebSocket clients
- Three WebSocket endpoints: `/ws/rig/command` (bidirectional), `/ws/rig/status` (broadcast), `/ws/rig/events` (broadcast)
- PTT locking: first-come-first-served exclusive PTT access with automatic release on client disconnect
- PTT safety timeout: configurable countdown (`ptt.safety.timeout.seconds`, default 120s); force-releases PTT and broadcasts `ptt_timeout` event on expiry
- Smart caching: read commands cached at 50ms TTL to reduce rigctld load under multiple clients; request coalescing for in-flight deduplication
- Standalone `docker-compose.yml` and `.env.example` for deployment independent of the main stack
- Frontend rig-status panel with real-time frequency/mode display and QSO form auto-population
- Cloud Relay / Station Gateway: `CloudRelayClient` (rig service) opens outbound WSS to backend `/ws/station-gateway`; BCrypt API key auth; STOMP forwarding for status and events; `StationGatewayHandler` + `StationGatewayRegistry` on backend; `Station` entity has `remoteStation` and `apiKeyHash` fields
- WebSocket API key authentication: `RigApiKeyHandshakeInterceptor` on all 3 WS endpoints; validates `X-Api-Key` header or `?apiKey=` query param; `rig.api.keys` config (empty = allow all, backward compatible)
- Command audit trail: `CommandAuditLog` circular buffer (configurable `rig.audit.max-entries`, default 1000); REST endpoint `GET /api/rig/audit?limit=N`
- Prometheus metrics: `micrometer-registry-prometheus` dependency; exposed at `/actuator/prometheus`

### 12. Log Type Separation (Personal / Shared)

- Backend guard: personal logs are always private
- Two create buttons in the UI (Personal and Shared)
- Two-section log list
- Conditional form fields in the create/edit modal

---

## Completed Sprint Features

| Feature | Notes |
|---------|-------|
| Award tracking (DXCC, WAS, VUCC) | `AwardController` at `/api/awards/{logId}`, `AwardProgressComponent` on dashboard |
| LoTW confirmation sync | `LotwSyncService` + `LotwSyncController` (`POST /api/lotw/sync/{logId}`); frontend form in import panel |
| DX cluster spotting | `DXClusterService` polling DX Summit (60s interval); `DxClusterPanelComponent` on dashboard |
| Propagation prediction | NOAA SWPC solar data; per-band conditions; `PropagationPanelComponent` on dashboard |
| Print-ready QSL card generation | `QslCardComponent` modal with `@media print` CSS |
| Station color preference persistence | `UserPreferencesController` at `/api/user/station-colors`; `User.stationColorPreferences` TEXT column |
| Callsign lookup (QRZ + FCC) | QRZ XML API with session management; FCC ULS JSON lookup |

---

## Remaining Backlog

| Feature | Priority | Notes |
|---------|----------|-------|
| Filter preset backend persistence | High | Per-log, per-user, private presets stored in backend DB. On first login after update, migrate existing localStorage presets (key `ewa_map_filter_presets`) to backend. No preset limit. Presets cascade-delete with their log. New entity: `UserFilterPreset` (id, user FK, log FK, name, filterJson TEXT, createdAt). Endpoints: `GET /api/logs/{logId}/filter-presets`, `POST /api/logs/{logId}/filter-presets`, `DELETE /api/logs/{logId}/filter-presets/{id}`. Frontend `map-filter-panel` migrates on init if backend presets are empty and localStorage has data. |
| Duplicate detection | High | Configurable per log (log creator sets policy). Default: warn only — show alert on QSO entry form when the callsign has already been worked on the same band and mode in this log, but allow operator to save anyway. Hard-block mode available as an alternative log setting. Backend: `GET /api/qsos/dupe-check?logId={id}&callsign=W1AW&band=20m&mode=SSB` returns `{ isDupe: true, priorQso: {...} }`. Frontend warns inline on callsign field blur. |
| eQSL integration | High | Send and receive. Upload QSOs to eQSL.cc and pull confirmations back to update `eqslRcvd` field. Per-request credentials (never stored), same pattern as LoTW. Upload: `POST /api/eqsl/upload/{logId}`. Sync confirmations: `POST /api/eqsl/sync/{logId}`. Match by callsign (case-insensitive) + date + band. Returns: uploaded count, confirmed count, errors. |
| ClubLog real-time upload | High | Each QSO pushed to ClubLog API as it is logged. Credentials stored per-user (encrypted or env-configured). `ClubLogService` hooks into `QSOService.createQSO()` — fires async upload after save. Config: `clublog.api.key` per user preference. Failure is non-blocking (log locally, retry on next sync). Manual full-sync endpoint: `POST /api/clublog/sync/{logId}` as fallback. |
| Per-band/mode award tracking | High | Extend `AwardController` and `AwardTrackingService` to return DXCC, WAS, and VUCC progress broken down by band and mode. New queries in `QSORepository`: `findDistinctCountriesByLogIdAndBand()`, `findDistinctCountriesByLogIdAndMode()`, etc. Frontend award panel expands to show a band × mode grid — worked and confirmed counts per cell. Enables 5-Band DXCC, 5-Band WAS, and per-mode VUCC tracking. |
| POTA / SOTA activation workflow | High | Full activation support: (1) park/summit reference field on QSO entry form and QSO entity (`parkRef` / `summitRef` TEXT); (2) GPS auto-fill of grid square via browser Geolocation API on QSO form; (3) direct upload to POTA API (api.pota.app) and SOTA API (api.sota.org.uk) — `POST /api/pota/upload/{logId}` and `POST /api/sota/upload/{logId}`; (4) ADIF export includes `POTA_REF` and `SOTA_REF` fields. Credentials per-request. |
| Mobile-optimised web UI | Medium | Full responsive pass across the entire app (QSO entry, map, dashboard). Refined breakpoints and touch targets within the existing Angular component structure — no separate mobile layout. Map requires full touch support: pinch-zoom, tap markers, swipe filter panel (Leaflet touch handlers + Angular CDK drag where needed). |
| Satellite pass predictions | Medium | Backend fetches TLE data from Celestrak amateur satellite category (cached, auto-refreshed). Pass calculations via predict4java. REST endpoint serves next N passes per satellite given a lat/lon. Dashboard panel shows upcoming passes (time, max elevation, duration) alongside DX cluster and propagation panels. Location source is user-selectable in the panel: GPS (browser Geolocation API) or saved grid square — GPS takes precedence when chosen. Map overlay shows ground track and footprint coverage circle during active pass. Dedicated satellite log type tracks pass number, elevation at contact time, and Doppler-corrected frequency. |
| WebSocket event broadcast | Medium | Real-time STOMP event stream for external integrations. Broadcast QSO, score, band change, and PTT events to `/topic/events/{logId}` so external scoreboards, displays, and tools can subscribe without being built into the app. Mirrors N1MM+'s UDP broadcast concept but via WebSocket. Payload schema: `{ eventType, logId, timestamp, data }`. No auth required for subscription (read-only stream). |
| DXCC Challenge tracking | Medium | Track confirmed entity-band combinations across all amateur bands toward the DXCC Challenge total (3,000+ possible entity-band slots). Separate from standard DXCC. New `AwardTrackingService` method returns challenge score (confirmed entity-band count) and a breakdown matrix (entity × band). Dashboard widget shows challenge score alongside standard DXCC. Queries: `findDistinctConfirmedEntityBandCombinationsByLogId()`. |
| Voice logging | Low | Full hands-free app control via speech recognition. QSO entry (callsign, frequency, mode, report), navigation between views, and common actions (export, filter, open log) all voice-driven. Engine is user-selectable: browser Web Speech API (Chrome/Edge, may need internet) or backend Whisper endpoint (offline-capable, user configures). Voice activation via push-to-talk button or configurable wake word. |
| Custom map styles and themes | Low | User-configurable map appearance saved to backend per user. Features: (1) base tile style switcher — OpenStreetMap, satellite, topographic, dark, minimal; (2) overlay customisation — per-overlay visibility toggle, opacity slider, and colour picker for CQ zones, ITU zones, ARRL sections, DXCC boundaries; (3) QSO marker styling — colour by band/mode/DXCC/date or single custom colour, adjustable marker size; (4) print/screenshot mode — chrome-free full-screen map for printing or QRZ page screenshots. Preferences stored in a new `User.mapPreferences` TEXT column (JSON). |
| Frontend unit tests | Low | Not yet implemented |
| E2E tests for critical user flows | Low | Not yet implemented |
| Band condition alerts | Low | Push notification or dashboard alert when a band opens based on live propagation data (solar flux, K-index thresholds); configurable per band |
| Suggested operating times | Low | Analyse user's own log history to surface best days/times/bands for DX; shown as a stats panel or calendar heat map |
| Grey line tracker | Low | Animated terminator line on the map showing current dawn/dusk boundary; updates in real time to highlight optimal DX propagation windows |
| Contact trends & statistics | Low | Per-user stats page: most worked DXCC, most active bands and modes over time, best DX distance; date-range filterable |
| Activity charts | Low | GitHub-style contribution graph showing QSO count per day over the past year; per-log or across all logs |
| This day in history | Low | Dashboard widget showing contacts made on this calendar date in previous years |
| Personal records | Low | Track and display personal bests: longest distance QSO, rarest DXCC worked, highest band, most QSOs in a single day |
| eQSL support | Low | Upload contacts to eQSL.cc and pull confirmations back; match against existing QSOs and update `eqslRcvd` status |
| ClubLog upload | Low | Push contacts to ClubLog API for DXCC tracking and leaderboard participation; configurable per log |
| WSJT-X ADIF auto-import | Low | Backend watches a configured directory (or user uploads) for new ADIF files from WSJT-X; auto-imports FT8/FT4 contacts into the active log |
| APRS integration | Low | Fetch APRS beacon data for contacted callsigns and show their last known position on the QSO map |
| Club aggregate logs | Low | Club-level view that combines QSOs from multiple member accounts into a shared log; useful for field day totals and club award tracking |
| CAT control auto-logging | Low | Use the existing rig control service to auto-populate frequency and mode on the QSO entry form whenever the rig changes VFO or mode |

---

## Key Technical Notes

### Stack

- Java 25 LTS / Spring Boot 4.0.3 / Lombok 1.18.42
- Angular 21.2.4 standalone components / TypeScript 5.9.3
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
- CORS is handled globally via `SecurityConfig.corsConfigurationSource()` using the `FRONTEND_ORIGIN` environment variable. Per-controller `@CrossOrigin` annotations have been removed from all controllers.
- `console.log` debug statements present in `websocket.service.ts`, `qso-map` component, and auth components — benign, not user-facing.
