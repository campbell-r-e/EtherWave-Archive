# EtherWave Archive - Product Specification (Agile Format)

## Product Overview

**Product Name:** EtherWave Archive
**Version:** 1.2.0
**Platform:** Web Application (Angular + Spring Boot)
**Domain:** Amateur Radio (Ham Radio) Logging, Mapping, and Contest System
**Theme:** Signal Analysis / Sci-Fi Aesthetic

### Product Vision
EtherWave Archive is a modern, multi-station amateur radio logging system providing comprehensive QSO management with advanced geographic visualization, real-time updates, contest validation and scoring, rig control integration, and multi-user collaboration. The system supports multiple concurrent operators with role-based access control, seven contest validators, and a plugin-based architecture for contest extensibility.

---

## Epic 1: Core Logging System

### Epic Description
As a radio operator, I need a robust logging system to record, manage, and search my radio contacts across multiple stations and operating modes.

**Status: Complete**

### User Stories

#### US-1.1: Multi-Station Log Management
**As a** radio operator with multiple stations
**I want to** create and manage separate logs for each station
**So that** I can keep my station operations organized and track performance individually

**Acceptance Criteria:**
- [x] System supports unlimited logs per user
- [x] Each log has a unique identifier and metadata (name, description, callsign)
- [x] Logs can be created, edited, and soft-deleted
- [x] Station-specific configuration (stations numbered 1-10 plus GOTA designation)
- [x] Access control enforced: CREATOR / STATION / VIEWER roles per log
- [x] LogType enum (PERSONAL / SHARED) enforced at backend

**Technical Implementation:**
- Entity: `Log` (JPA) with `LogType` enum and `bonusMetadata` TEXT column
- Repository: `LogRepository` with CRUD and soft-delete
- Service: `LogService` with business logic and role resolution
- Controller: `LogController` with REST endpoints
- Frontend: Angular `LogService` + log-selector component

---

#### US-1.2: QSO Entry and Editing
**As a** radio operator
**I want to** log radio contacts with comprehensive details
**So that** I can maintain accurate records for awards and statistics

**Acceptance Criteria:**
- [x] Capture essential QSO fields: callsign, frequency/band, mode, date/time, RST
- [x] 25+ fields including operator name, QTH, grid square, notes, and contest exchange data
- [x] Support for 9+ bands (160M to 70CM)
- [x] Support for 8+ modes (SSB, CW, FT8, FT4, RTTY, PSK31, FM, AM)
- [x] Real-time callsign and grid square validation
- [x] Auto-population of QSO form from rig control data
- [x] Edit and delete capabilities with confirmation

**Technical Implementation:**
- Entity: `QSO` (JPA) with 25+ fields
- Repository: `QSORepository` with pagination and filtering
- Service: `QSOService` with validation logic
- Controller: `QSOController` with REST endpoints
- Frontend: Reactive forms with validation, rig data binding

---

#### US-1.3: ADIF Import/Export and Cabrillo Export
**As a** radio operator
**I want to** import and export logs in standard formats
**So that** I can migrate data between logging applications and submit contest entries

**Acceptance Criteria:**
- [x] Import ADIF 3.1.4 files
- [x] Export logs to ADIF 3.1.4 format
- [x] Cabrillo export for contest submission
- [x] Parse all standard ADIF fields
- [x] Handle custom/vendor-specific fields gracefully
- [x] Validate data integrity during import
- [x] Report import statistics (success/failure counts)

**Technical Implementation:**
- Service: `ADIFService` with parser and generator
- Service: `CabrilloExportService`
- Support for `.adi` and `.adif` file extensions
- Batch processing for large files

---

## Epic 2: Geographic Visualization & Mapping

### Epic Description
As a radio operator, I need advanced mapping capabilities to visualize my contacts geographically, track grid square coverage, and analyze propagation patterns with interactive overlays and real-time updates.

**Status: Complete**

### User Stories

#### US-2.1: Interactive QSO Map
**As a** radio operator
**I want to** see my contacts on an interactive world map
**So that** I can visualize my global communications and identify coverage gaps

**Acceptance Criteria:**
- [x] Interactive Leaflet.js map with pan and zoom
- [x] QSO markers showing contact locations
- [x] Adaptive server-side clustering (10,000 QSO threshold)
- [x] Click markers to view QSO details
- [x] Color-coded by station (up to 10 stations plus GOTA)
- [x] Dark/light theme tile layers (CartoDB)
- [x] Center map on user location by default
- [x] Empty state when no location data available
- [x] `FullscreenMapViewComponent` subscribes to `LogService.currentLog$`, passes `[logId]` and `[filters]` to `<app-qso-map>`

**Technical Implementation:**
- Backend: `MapDataService`, `MapController`
- Frontend: `QSOMapComponent`, `FullscreenMapViewComponent`
- Library: Leaflet 1.9.4
- Tile Layers: CartoDB Positron (light) / Dark Matter (dark)
- Clustering: Haversine distance calculation
- API: `GET /api/maps/qsos/{logId}?zoom={zoom}`

---

#### US-2.2: Maidenhead Grid Square Overlay
**As a** radio operator working on grid square awards
**I want to** see Maidenhead grid squares overlaid on the map
**So that** I can track which grids I have worked and identify needed grids

**Acceptance Criteria:**
- [x] Toggle grid square overlay on/off
- [x] Support 4 precision levels: Field (2-char), Square (4-char), Subsquare (6-char), Extended (8-char)
- [x] Color-coded grid squares based on activity level
- [x] Show worked grids with QSO counts
- [x] Interactive popups with grid statistics
- [x] Grid precision selector in UI
- [x] Grid coverage endpoint correctly passes filters to `GridCoverageService`

**Technical Implementation:**
- Backend: `GridCoverageService`, `MaidenheadConverter`
- Frontend: `GridOverlayService`
- Algorithm: Maidenhead locator to lat/lon conversion
- API: `GET /api/maps/grids/{logId}?precision={2|4|6|8}`

---

#### US-2.3: Heatmap Density Visualization
**As a** radio operator analyzing propagation patterns
**I want to** see a heatmap of QSO density
**So that** I can identify areas of high activity and propagation corridors

**Acceptance Criteria:**
- [x] Toggle heatmap overlay on/off
- [x] Adjustable radius (10-50px slider)
- [x] Real-time radius adjustment without reload
- [x] Multiple gradient themes (default, warm, cool, monochrome)
- [x] EtherWave theme gradient (blue to cyan to green to orange to pink)
- [x] Intensity based on QSO count per location

**Technical Implementation:**
- Backend: `HeatmapService` with point aggregation
- Frontend: `HeatmapService`
- Library: leaflet.heat plugin
- API: `GET /api/maps/heatmap/{logId}`

---

#### US-2.4: Multi-Station Pie Chart Clusters
**As a** radio operator with multiple stations
**I want to** see cluster markers as pie charts showing station breakdown
**So that** I can visualize which stations contributed to each cluster

**Acceptance Criteria:**
- [x] Clusters with multiple stations displayed as SVG pie charts
- [x] Each slice color-coded by station
- [x] Center count badge shows total QSOs
- [x] Slices sorted by count (largest first)
- [x] Single-station clusters show solid color
- [x] SVG-generated charts for crisp rendering

**Technical Implementation:**
- Component: `QSOMapComponent.createPieChartSVG()`
- SVG path generation with arc calculations
- Dynamic sizing based on QSO count (30-70px)

---

#### US-2.5: Recent QSO Pulse Animations
**As a** radio operator
**I want to** see visual animations on recently logged QSOs
**So that** I can quickly identify new contacts on the map

**Acceptance Criteria:**
- [x] QSOs within last 15 minutes show pulse animation
- [x] Expanding ring effect (2-second cycle)
- [x] Infinite loop while QSO remains recent
- [x] Timestamp-based automatic detection
- [x] Animation syncs with real-time WebSocket updates

**Technical Implementation:**
- CSS: `@keyframes pulse` animation
- Detection: `isRecentQSO(timestamp)` method
- Time window: 15 minutes (900,000 ms)

---

#### US-2.6: Contest Overlay Layers
**As a** contest operator
**I want to** overlay contest-specific zones and sections on the map
**So that** I can track multipliers and identify needed entities

**Acceptance Criteria:**
- [x] CQ Zones (40 zones worldwide)
- [x] ITU Zones (90 zones worldwide)
- [x] ARRL Sections (83 US/Canada sections)
- [x] DXCC Entities (340+ countries/territories)
- [x] Dropdown menu with toggles for each overlay
- [x] Color-coded: worked vs. needed
- [x] Interactive popups with zone/section details
- [x] `FullscreenMapViewComponent` wires overlay changes to `QSOMapComponent.toggleContestOverlay()`

**Technical Implementation:**
- Backend: `ContestOverlayService`
- Frontend: `ContestOverlayService`
- Data: CTY.DAT file parsing for DXCC
- API Endpoints:
  - `GET /api/maps/overlays/cq-zones/{logId}`
  - `GET /api/maps/overlays/itu-zones/{logId}`
  - `GET /api/maps/overlays/arrl-sections/{logId}`
  - `GET /api/maps/overlays/dxcc/{logId}`

---

#### US-2.7: Map Data Export
**As a** radio operator
**I want to** export map data in multiple formats
**So that** I can use my data in other GIS and mapping tools

**Acceptance Criteria:**
- [x] Export formats: GeoJSON, KML, CSV, ADIF
- [x] Modal dialog with format selection
- [x] Apply current filters to export
- [x] Automatic file download with correct MIME types
- [x] Loading state during export

**Export Formats:**
- **GeoJSON**: Geographic data for GIS tools (`application/geo+json`)
- **KML**: Google Earth compatible (`application/vnd.google-earth.kml+xml`)
- **CSV**: Spreadsheet with location data (`text/csv`)
- **ADIF**: Ham radio standard with coordinates (`text/plain`)

**Technical Implementation:**
- Backend: `MapExportService` with format converters
- Frontend: `MapExportDialogComponent`
- API: `POST /api/maps/export/{logId}?format={format}`
- Blob download with `URL.createObjectURL()`

---

#### US-2.8: Real-Time Map Updates
**As a** radio operator during active operations
**I want to** see new QSOs appear on the map in real-time
**So that** I do not need to manually refresh to see current activity

**Acceptance Criteria:**
- [x] WebSocket/STOMP connection to backend
- [x] Automatic marker addition for new QSOs
- [x] Incoming QSOs filtered by current filter state
- [x] Cluster recalculation in clustered mode
- [x] Toggle button to enable/disable real-time updates
- [x] Visual indicator (green = enabled, gray = disabled)
- [x] 5-second reconnection with exponential backoff

**Technical Implementation:**
- Backend: WebSocket with STOMP protocol
- Transport: SockJS for compatibility
- Topic: `/topic/qsos`
- Frontend: `WebSocketService` integration

---

## Epic 3: Advanced Filtering & Search

### Epic Description
As a radio operator, I need powerful filtering capabilities to find specific contacts and analyze subsets of my log data.

**Status: Complete**

### User Stories

#### US-3.1: Comprehensive Map Filters
**As a** radio operator
**I want to** filter the map display by multiple criteria
**So that** I can focus on specific subsets of contacts

**Acceptance Criteria:**
- [x] Filter by Band (160M, 80M, 40M, 20M, 15M, 10M, 6M, 2M, 70CM)
- [x] Filter by Mode (SSB, CW, FT8, FT4, RTTY, PSK31, FM, AM)
- [x] Filter by Station (1-10 + GOTA)
- [x] Filter by Operator (text search)
- [x] Filter by DXCC Entity (country name or prefix)
- [x] Filter by Continent (NA, SA, EU, AF, AS, OC)
- [x] Filter by State/Province (text input)
- [x] Filter by Exchange (contest data)
- [x] Filter by Date Range (from/to dates)
- [x] Filter by Confirmation Status (confirmed/unconfirmed/all)
- [x] Active filter pills showing current selections with individual removal
- [x] Clear all filters button
- [x] `FullscreenMapViewComponent` wires filter changes to `QSOMapComponent.applyFilters()`

**Technical Implementation:**
- Component: `MapFilterPanelComponent`
- Interface: `MapFilters` with 10 optional fields
- Server-side filtering in `MapDataService.getFilteredQSOs()` (public method)

---

#### US-3.2: Mobile-Responsive Filter Panel
**As a** mobile user
**I want to** access all filters on my phone or tablet
**So that** I can use the full system on any device

**Acceptance Criteria:**
- [x] Bottom sheet UI on mobile (<768px)
- [x] Fixed position at bottom of screen with swipe handle indicator
- [x] Touch gesture support (swipe up/down, 50px minimum distance)
- [x] Collapsed state (60px visible — header only)
- [x] Expanded state (80vh max height, scrollable)
- [x] Smooth CSS transitions
- [x] Desktop: standard panel above map

**Technical Implementation:**
- CSS media queries at 768px breakpoint
- Touch events: `touchstart` and `touchend`
- Transform: `translateY()` for sliding effect

---

## Epic 4: DXCC Management

### Epic Description
As a radio operator tracking DXCC awards, I need comprehensive country prefix management and lookup capabilities.

**Status: Complete**

### User Stories

#### US-4.1: DXCC Prefix Database
**As a** radio operator
**I want to** automatically identify countries from callsigns
**So that** I can track my DXCC entity progress

**Acceptance Criteria:**
- [x] Load CTY.DAT file (standard amateur radio country file)
- [x] Parse 340+ DXCC entities
- [x] Support primary and alternate prefixes
- [x] Exact match support (`=W1AW` format)
- [x] Longest-match algorithm for prefix lookup
- [x] Cache lookups for performance (`@Cacheable`)
- [x] Load default file or custom upload

**Technical Implementation:**
- Service: `DXCCLoaderService` with regex parser
- Service: `DXCCLookupService` with caching
- Entity: `DXCCPrefix` with JPA
- Batch saving: 1000 prefixes per batch
- API Endpoints:
  - `POST /api/dxcc/load`
  - `POST /api/dxcc/load-default`
  - `GET /api/dxcc/lookup/{callsign}`
  - `GET /api/dxcc/status`

---

## Epic 5: Session & Location Management

### Epic Description
As a radio operator, I need flexible location management for different operating scenarios including portable, home, and field day operations.

**Status: Complete**

### User Stories

#### US-5.1: Hierarchical Location Fallback
**As a** radio operator
**I want** the system to intelligently determine my operating location
**So that** QSO locations are accurate without manual entry for every contact

**Location Priority:**
1. Station configuration (highest priority)
2. User profile default location
3. Session temporary location
4. Manual QSO-level entry (lowest priority)

**Acceptance Criteria:**
- [x] Station-level location in station configuration
- [x] User-level location in user profile (`defaultLatitude` / `defaultLongitude`)
- [x] Session temporary location with 1-hour TTL
- [x] QSO-level manual override
- [x] Automatic Maidenhead grid calculation from coordinates
- [x] `MapDataService.getOperatorLocation()` resolves user default location via `userRepository.findByCallsign()`
- [x] `LocationManagementService.updateUserLocation(String username, ...)` overload added

**Technical Implementation:**
- Service: `LocationManagementService` with fallback logic
- Service: `SessionLocationService` with in-memory cache
- TTL: 1 hour (3600 seconds)
- Storage: `ConcurrentHashMap` for thread safety
- `MapController.setUserLocation()` uses `Authentication` parameter (not hardcoded user ID)

---

#### US-5.2: Session Location Override
**As a** portable operator
**I want to** temporarily set my location for a session
**So that** I do not need to change my permanent station location

**Acceptance Criteria:**
- [x] Set session location with lat/lon or grid square
- [x] 1-hour automatic expiration
- [x] Clear session location manually
- [x] View current session location
- [x] In-memory storage (no database persistence required)
- [x] Thread-safe implementation

**API Endpoints:**
- `POST /api/maps/session-location/{logId}`
- `GET /api/maps/session-location/{logId}`
- `DELETE /api/maps/session-location/{logId}`
- `PUT /api/maps/location/user`

---

## Epic 6: User Interface & Experience

### Epic Description
As a user, I need an intuitive, visually appealing interface that works across all devices.

**Status: Complete**

### User Stories

#### US-6.1: EtherWave Archive Branding and Theme
**As a** user
**I want** a modern, signal-analysis themed interface
**So that** the application feels professional and unique

**Design Elements:**
- [x] Gradient colors: #0080ff to #00d4ff (blue to cyan)
- [x] Signal analysis / sci-fi aesthetic
- [x] Glassmorphism effects on panels (`backdrop-filter: blur()`)
- [x] Smooth animations and transitions (0.2-0.3s ease)
- [x] Dark/light theme support with CartoDB tile switching
- [x] Bootstrap 5.3.x base with custom overrides

---

#### US-6.2: Fully Responsive Design
**As a** user on any device
**I want** the interface to adapt to my screen size
**So that** I can use all features on desktop, tablet, or mobile

**Breakpoints:**
- Desktop: >= 769px (full layout)
- Tablet: 576-768px (adjusted layout)
- Mobile: <= 575px (stacked layout, bottom-sheet filter panel)

**Acceptance Criteria:**
- [x] Mobile bottom sheet for map filters
- [x] Touch-optimized controls with swipe gesture support
- [x] Responsive map sizing
- [x] Stacked form fields on mobile
- [x] Collapsible sections
- [x] Mobile-friendly buttons and inputs

---

## Epic 7: Performance & Optimization

### Epic Description
As a system administrator, I need the application to perform efficiently under load with large datasets.

**Status: Complete**

### User Stories

#### US-7.1: Server-Side Adaptive Clustering
**As a** radio operator with 10,000+ QSOs
**I want** the map to remain responsive
**So that** I can visualize my entire log without performance issues

**Acceptance Criteria:**
- [x] Automatic clustering activates above 10,000 QSO threshold
- [x] Haversine distance calculation for grouping
- [x] Zoom-dependent pixel radius
- [x] Station and band breakdown in cluster metadata
- [x] Click cluster to zoom to bounds

**Performance Targets:**
- Load time: <2 seconds for 10k QSOs
- Cluster calculation: <500ms
- Map render: <1 second

---

#### US-7.2: Caching Strategy
**As a** system administrator
**I want** frequently accessed data to be cached
**So that** response times are minimized

**Caching Layers:**
- [x] Spring Cache for DXCC lookups (`@Cacheable`)
- [x] In-memory session locations (1-hour TTL via `ConcurrentHashMap`)
- [x] Browser caching for tile layers (CartoDB CDN)

---

## Epic 8: Authentication & Security

### Epic Description
As a system operator, I need secure multi-user authentication to protect log data and enforce access boundaries.

**Status: Complete**

### User Stories

#### US-8.1: JWT-Based Authentication
**As a** user
**I want** to log in securely and stay logged in
**So that** my session is persistent and my data is protected

**Acceptance Criteria:**
- [x] JWT tokens issued on successful login
- [x] 24-hour token expiry
- [x] Bearer token authentication on all protected endpoints
- [x] Token validation middleware applied system-wide

---

#### US-8.2: User Registration (Username-Only)
**As a** new user
**I want to** register with a username and password
**So that** I can access the system without providing an email address

**Acceptance Criteria:**
- [x] Registration requires username + password only
- [x] Optional callsign field at registration
- [x] No email field anywhere in the auth system (removed entirely)
- [x] Username uniqueness enforced
- [x] `RegisterRequest` DTO has no email field
- [x] `User` entity has no email column

**Technical Implementation:**
- Entity: `User` — no email field
- DTO: `LoginRequest` — `username` field only (not usernameOrEmail)
- DTO: `RegisterRequest` — no email field
- Repository: `UserRepository` — no email query methods
- Frontend model: `user.model.ts` — no email in `User`, `RegisterRequest`, or `AuthResponse`

---

#### US-8.3: BCrypt Password Hashing
**As a** security-conscious operator
**I want** passwords stored securely
**So that** credentials are protected even if the database is compromised

**Acceptance Criteria:**
- [x] BCrypt hashing applied to all passwords at registration
- [x] Passwords never stored or transmitted in plaintext
- [x] Spring Security BCryptPasswordEncoder used

---

#### US-8.4: Admin User Bootstrap
**As a** system administrator
**I want to** configure the initial admin account via environment variables
**So that** the first admin user does not require a separate setup step

**Acceptance Criteria:**
- [x] `ADMIN_USERNAME` and `ADMIN_PASSWORD` environment variables respected
- [x] Admin user created on first startup if not present
- [x] No `ADMIN_EMAIL` variable (email removed from system)

---

#### US-8.5: Frontend Route Guards
**As a** system designer
**I want** all authenticated pages protected by route guards
**So that** unauthenticated users are redirected to login

**Acceptance Criteria:**
- [x] Angular route guards on all authenticated pages
- [x] Redirect to `/login` on guard failure
- [x] JWT stored and attached to all outgoing API requests via interceptor

---

## Epic 9: Multi-User Collaboration

### Epic Description
As a club or field day operator, I need to invite other users to shared logs, assign them to stations, and manage their roles and permissions within a log.

**Status: Complete**

### User Stories

#### US-9.1: Role-Based Access Control
**As a** log creator
**I want** to control what other users can do in my log
**So that** participants have appropriate access without risking data integrity

**Roles:**
- **CREATOR**: Full control — edit log settings, invite/remove participants, add/edit/delete QSOs
- **STATION**: Operator — add and edit QSOs, view all log data
- **VIEWER**: Read-only — view log and QSOs, no modifications

**Acceptance Criteria:**
- [x] `userRole` field returned on `LogResponse` (CREATOR / STATION / VIEWER)
- [x] All permission checks in backend enforce role boundaries
- [x] `permissions.service.ts` uses `log.userRole` for frontend RBAC
- [x] `isCreator` checks both `log.creatorId` and `log.userRole === CREATOR`
- [x] `isOperator` checks `log.userRole === STATION`

---

#### US-9.2: Invitation System
**As a** log creator
**I want to** invite other users to join my log
**So that** they can participate in a shared operation

**Acceptance Criteria:**
- [x] Send invitations by username or callsign (no email required)
- [x] `InvitationService` supports lookup by username or callsign
- [x] Invited user notified via in-app invitations list
- [x] Invitations panel available in the frontend (`invitations.component.ts`)

---

#### US-9.3: Invitation Lifecycle
**As an** invited user
**I want to** accept or decline invitations
**So that** I control which logs I participate in

**Invitation States:**
- PENDING: Invitation sent, awaiting response
- ACCEPTED: User joined the log
- DECLINED: User declined the invitation
- CANCELLED: Invitation withdrawn by creator
- EXPIRED: Invitation exceeded validity period

**Acceptance Criteria:**
- [x] All five states implemented and enforced
- [x] Creators can cancel pending invitations
- [x] Expired invitations removed from active display

---

#### US-9.4: Participant Management
**As a** log creator
**I want to** view and manage participants in my log
**So that** I can maintain operational control during an event

**Acceptance Criteria:**
- [x] View all participants with their roles and assigned stations
- [x] Remove participants from a log
- [x] Participant list visible to CREATOR only for management actions

---

#### US-9.5: Station Assignment
**As a** log creator
**I want to** assign participants to numbered stations
**So that** QSOs are correctly attributed to the right station in multi-transmitter events

**Acceptance Criteria:**
- [x] Station numbers 1-10 supported
- [x] GOTA (Get On The Air) station designation supported
- [x] Station assignment shown as color-coded badge in participant list
- [x] Station color preferences displayed (localStorage-backed)

---

#### US-9.6: Personal-to-Shared Conversion
**As a** log creator
**I want to** convert a personal log to a shared log
**So that** I can invite others to a log I originally created for myself

**Acceptance Criteria:**
- [x] One-way PERSONAL to SHARED conversion endpoint
- [x] Conversion cannot be reversed
- [x] Personal logs reject invitations with HTTP 400 before conversion
- [x] Frontend exposes conversion action for CREATOR only

---

## Epic 10: Contest Validation & Scoring

### Epic Description
As a contest operator, I need real-time exchange validation, duplicate detection, and scoring so that I can compete accurately without post-event cleanup.

**Status: Complete**

### User Stories

#### US-10.1: Plugin-Based Contest Validator Architecture
**As a** developer
**I want** a standardized interface for contest validators
**So that** new contest types can be added without modifying core logic

**Acceptance Criteria:**
- [x] `ContestValidator` interface defined
- [x] Validators registered by contest code string
- [x] Contest config JSONs loaded from `backend/src/main/resources/contest-configs/`
- [x] All 7 validators implement the interface

---

#### US-10.2: Contest Validators (7 Implemented)
**As a** contest operator
**I want** automated exchange validation for the contests I operate
**So that** invalid contacts are flagged before submission

| Validator | Contest Code | Tests |
|-----------|-------------|-------|
| FieldDayValidator | `ARRL-FD` | 76 |
| POTAValidator | `POTA` | 14 |
| SOTAValidator | `SOTA` | 13 |
| WinterFieldDayValidator | `WFD` | 16 |
| CQWWValidator | `CQWW` | 30 |
| ARRLSweepstakesValidator | `ARRL-SS` | 42 |
| StateQSOPartyValidator | `STATE-QSO-PARTY` | 27 |

**Acceptance Criteria:**
- [x] Each validator enforces exchange field requirements for its contest
- [x] CQWW: CQ zone 1-40 required in exchange
- [x] ARRL Sweepstakes: serial number, precedence (Q/A/B/M/S/U), check, ARRL section required
- [x] State QSO Party: serial number and state required in exchange
- [x] All 218 validator-level tests passing

**Contest Config Files:**
- `backend/src/main/resources/contest-configs/arrl-sweepstakes.json`
- `backend/src/main/resources/contest-configs/cqww.json`
- `backend/src/main/resources/contest-configs/sota.json`
- `backend/src/main/resources/contest-configs/state-qso-party.json`
- (and field-day, pota, wfd configs)

---

#### US-10.3: Real-Time Scoring
**As a** contest operator
**I want to** see my score update as I log contacts
**So that** I know my standing during the contest

**Acceptance Criteria:**
- [x] `ScoringService` calculates QSO points and multipliers in real-time
- [x] `MultiplierTrackingService` tracks worked multipliers
- [x] Score broadcast via WebSocket on each new QSO
- [x] Score summary visible in frontend score-summary component

---

#### US-10.4: Bonus Point Tracking
**As a** contest operator
**I want** bonus points (e.g., club bonus, natural power bonus) tracked automatically
**So that** my final score is accurate without manual calculation

**Acceptance Criteria:**
- [x] `Log.bonusMetadata` TEXT column stores JSON map (bonus key to count/flag integer)
- [x] `ScoringService.calculateBonusPoints()` reads `bonus_points` from contest config JSON
- [x] `LogResponse.bonusMetadata` exposed to frontend
- [x] `LogRequest.bonusMetadata` accepted in log update
- [x] Frontend `Log` model and `LogRequest` interface include `bonusMetadata?: string`

---

#### US-10.5: Duplicate QSO Detection
**As a** contest operator
**I want** the system to flag duplicate contacts
**So that** I do not waste time on contacts that will not count

**Acceptance Criteria:**
- [x] `DuplicateDetectionService` implemented
- [x] Duplicate check considers callsign, band, and mode
- [x] Duplicates flagged in real-time at QSO entry

---

#### US-10.6: Score Summary Dashboard
**As a** contest operator
**I want** a dedicated score summary view
**So that** I can monitor my progress throughout the event

**Acceptance Criteria:**
- [x] Frontend `score-summary` component displays current totals
- [x] Shows QSO count, multipliers, bonus points, and final score
- [x] Updates in real-time as QSOs are logged

---

## Epic 11: Rig Control Integration

### Epic Description
As an active operator, I need the logging system to communicate with my radio hardware so that frequency, mode, and other rig data are auto-populated into the QSO form.

**Status: Complete**

### User Stories

#### US-11.1: Hamlib/rigctld TCP Integration
**As a** radio operator
**I want** the backend to connect to my radio via Hamlib
**So that** rig data is available system-wide without browser plugins

**Acceptance Criteria:**
- [x] `RigControlClient` connects to `rigctld` TCP socket
- [x] Configurable host and port per user/station
- [x] Connection errors handled gracefully with reconnect

---

#### US-11.2: Real-Time Frequency/Mode Polling and WebSocket Broadcast
**As a** radio operator
**I want** the current frequency and mode polled continuously and pushed to my browser
**So that** I see live rig data without manual input

**Acceptance Criteria:**
- [x] Frequency and mode polled on configurable interval
- [x] Data broadcast over WebSocket to subscribed clients
- [x] Frontend updates in real-time

---

#### US-11.3: Rig Status Panel
**As a** radio operator
**I want** a persistent rig status display in the UI
**So that** I always know the current frequency and mode

**Acceptance Criteria:**
- [x] `rig-status` component displays frequency, mode, and connection state
- [x] Visual indicator for connected/disconnected state
- [x] Updates without page refresh

---

#### US-11.4: Auto-Population of QSO Form
**As a** radio operator entering a contact
**I want** the frequency and mode fields pre-filled from the rig
**So that** I only need to enter the callsign and signal report

**Acceptance Criteria:**
- [x] QSO form populates band and mode fields from current rig state on form open
- [x] User can override auto-populated values manually
- [x] Works whether rig is connected or not (graceful fallback to manual)

---

#### US-11.5: Standalone Rig Service Deployment
**As a** system administrator
**I want** to deploy the rig control service independently of the main logbook stack
**So that** the rig service can run on a station PC while the logbook runs elsewhere

**Acceptance Criteria:**
- [x] `rig-control-service/docker-compose.yml` runs the rig service standalone
- [x] `rig-control-service/.env.example` documents all configuration options
- [x] Service connects to `rigctld` on configurable host/port

---

#### US-11.6: Cloud Relay for Remote Station Control
**As a** radio operator using a cloud-hosted logbook
**I want** my home rig (local) to connect to the cloud logbook (remote) via a secure relay
**So that** frequency and mode are auto-populated even when the rig is not on the same network

**Acceptance Criteria:**
- [ ] Rig service opens an outbound WSS connection to the backend gateway (`/ws/station-gateway`)
- [ ] Rig service authenticates with `stationId` and API key
- [ ] Backend `StationGatewayHandler` validates key against BCrypt hash stored on `Station` entity
- [ ] Commands from backend users are forwarded through the gateway to the rig service
- [ ] Status updates from the rig service are forwarded back via STOMP to frontend clients
- [ ] `Station` entity has `remoteStation` (boolean) and `apiKeyHash` (String) fields
- [ ] `RigControlController` routes commands through `StationGatewayRegistry` for remote stations

---

#### US-11.7: PTT Safety Timeout
**As a** radio operator
**I want** PTT to automatically release if a client disconnects or becomes unresponsive
**So that** the transmitter cannot be stuck keyed indefinitely

**Acceptance Criteria:**
- [ ] `ptt.safety.timeout.seconds` config property (default: 120)
- [ ] `PTTLockManager` starts a countdown on PTT acquire
- [ ] Any command from the PTT-holding client resets the countdown
- [ ] On timeout: PTT force-released, `ptt_timeout` event broadcast to all clients

---

#### US-11.8: WebSocket API Key Authentication
**As a** system administrator
**I want** the rig service WebSocket endpoints to require an API key
**So that** only authorized clients (logbook backend, approved dashboards) can connect

**Acceptance Criteria:**
- [ ] `rig.api.keys` config property (comma-separated list)
- [ ] `RigApiKeyHandshakeInterceptor` validates `X-Api-Key` header or `?apiKey=` query param
- [ ] Unauthenticated connections receive HTTP 401 before WebSocket upgrade completes
- [ ] If `rig.api.keys` is empty, all connections are accepted (backward compatible)

---

## Epic 12: Log Type Separation — Personal vs Shared

### Epic Description
As a radio operator, I need a clear separation between personal logs (private, single-operator) and shared logs (collaborative, multi-operator) so that the system enforces appropriate rules for each type.

**Status: Complete**

### User Stories

#### US-12.1: LogType Enum at Backend
**As a** system designer
**I want** log type enforced at the data layer
**So that** personal and shared log rules cannot be bypassed via the API

**Acceptance Criteria:**
- [x] `LogType` enum (PERSONAL / SHARED) present on `Log` entity
- [x] Type enforced in all log service operations
- [x] Type returned in `LogResponse`

---

#### US-12.2: Personal Log Privacy Enforcement
**As a** personal log owner
**I want** my personal log to always be private
**So that** other users cannot see or join it

**Acceptance Criteria:**
- [x] `isPublic` forced to `false` for PERSONAL logs regardless of request body
- [x] Invitation attempts on PERSONAL logs rejected with HTTP 400
- [x] No STATION or VIEWER roles possible on a PERSONAL log

---

#### US-12.3: Separate Create Buttons
**As a** user
**I want** distinct creation flows for personal and shared logs
**So that** I clearly understand what I am creating

**Acceptance Criteria:**
- [x] Log selector presents two buttons: "New Personal Log" and "New Shared Log"
- [x] Each button opens a correctly pre-typed creation modal

---

#### US-12.4: Log List Split Into Labeled Sections
**As a** user with both personal and shared logs
**I want** my logs grouped by type in the selector
**So that** I can quickly find the log I need

**Acceptance Criteria:**
- [x] Log selector displays "My Personal Logs" section and "Shared Logs" section
- [x] Each section only shows logs of the correct type

---

#### US-12.5: Adaptive Modal Title and Fields
**As a** user
**I want** the log create/edit modal to adapt based on log type
**So that** irrelevant fields are not shown

**Acceptance Criteria:**
- [x] Modal title reflects log type ("New Personal Log" / "New Shared Log")
- [x] `isPublic` field hidden for PERSONAL logs

---

#### US-12.6: One-Way Personal-to-Shared Conversion
**As a** log creator
**I want to** promote a personal log to a shared log
**So that** I can open it for collaboration after the fact

**Acceptance Criteria:**
- [x] `POST /api/logs/{logId}/convert-to-shared` endpoint available
- [x] Conversion is permanent and cannot be reversed
- [x] Converted log accepts invitations and has SHARED rules applied immediately
- [x] Frontend exposes conversion only to CREATOR role

---

## Technical Architecture

### Backend Stack
- **Framework:** Spring Boot 4.0.3
- **Language:** Java 25 LTS
- **Annotations:** Lombok 1.18.42 (suppressed via `MAVEN_OPTS=--sun-misc-unsafe-memory-access=allow`)
- **Database:** PostgreSQL 16 (production) / SQLite 3.49.1.0 (field/dev)
- **ORM:** Spring Data JPA / Hibernate 7.x
- **Auth:** Spring Security + JWT (24-hour token expiry, BCrypt passwords)
- **WebSocket:** STOMP over SockJS
- **Caching:** Spring Cache
- **Build:** Maven 3.9 (`maven:3.9-eclipse-temurin-25-alpine`)
- **Runtime Image:** `eclipse-temurin:25-jre-noble` (Ubuntu Noble — no Alpine JRE for Java 25)
- **Tests:** 230 unit and integration tests (100% passing)
  - Test DB: H2 in-memory (`spring.sql.init.mode=never`, Hibernate DDL manages schema)
  - Testcontainers: 1.21.3
  - REST Assured: 5.5.2

### Frontend Stack
- **Framework:** Angular 21.2.0 (standalone components)
- **Language:** TypeScript 5.9.3
- **Node:** 22-alpine (build), nginx:stable-alpine (runtime)
- **Mapping:** Leaflet 1.9.4 + leaflet.heat plugin
- **UI Library:** Bootstrap 5.3.x
- **Icons:** Bootstrap Icons
- **HTTP:** RxJS Observables + Angular HttpClient
- **WebSocket:** `@stomp/stompjs` + `sockjs-client`

### Docker Setup
- **Compose plugin:** `docker compose` v5.1.0 (use `docker compose`, not `docker-compose`)
- **Prod compose:** `docker-compose.yml` — PostgreSQL, memory limits, 90-second `start_period` for backend
- **Field compose:** `docker-compose.field.yml` — SQLite, simpler setup
- **DDL_AUTO:** `update` in both compose files — allows schema evolution without migrations
- **Healthcheck:** `curl -sf http://localhost:8080/actuator/health`
- **Entrypoint:** `backend/docker-entrypoint.sh` — detects root, chowns `/app/data`, drops to `appuser` via `gosu`
- **JVM flags:** `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError`

### API Architecture
- **Style:** RESTful JSON
- **Authentication:** JWT Bearer token
- **Endpoints:** 30+ REST endpoints + WebSocket topics
- **Schema:** DDL_AUTO=update (JPA manages schema evolution)

### Key API Endpoints

#### Log Endpoints
```
GET    /api/logs                            # List logs for authenticated user
POST   /api/logs                            # Create log
GET    /api/logs/{logId}                    # Get log
PUT    /api/logs/{logId}                    # Update log
DELETE /api/logs/{logId}                    # Soft-delete log
POST   /api/logs/{logId}/convert-to-shared  # Convert personal to shared (one-way)
```

#### Map Endpoints
```
GET    /api/maps/qsos/{logId}                    # QSO locations with clustering
GET    /api/maps/grids/{logId}                   # Grid square coverage
GET    /api/maps/heatmap/{logId}                 # Heatmap density data
GET    /api/maps/overlays/cq-zones/{logId}       # CQ zone overlay
GET    /api/maps/overlays/itu-zones/{logId}      # ITU zone overlay
GET    /api/maps/overlays/arrl-sections/{logId}  # ARRL sections overlay
GET    /api/maps/overlays/dxcc/{logId}           # DXCC entities overlay
POST   /api/maps/export/{logId}                  # Export map data
POST   /api/maps/session-location/{logId}        # Set session location
GET    /api/maps/session-location/{logId}        # Get session location
DELETE /api/maps/session-location/{logId}        # Clear session location
PUT    /api/maps/location/user                   # Update user default location
GET    /api/maps/statistics/{logId}              # Map statistics
```

#### DXCC Endpoints
```
POST   /api/dxcc/load              # Upload custom CTY.DAT
POST   /api/dxcc/load-default      # Load default CTY.DAT from classpath
GET    /api/dxcc/status            # Check loaded status
GET    /api/dxcc/lookup/{callsign} # Lookup DXCC entity by callsign
```

#### Auth Endpoints
```
POST   /api/auth/register          # Register new user
POST   /api/auth/login             # Authenticate and receive JWT
```

#### Invitation Endpoints
```
POST   /api/invitations            # Send invitation (by username or callsign)
GET    /api/invitations/incoming   # List incoming invitations for current user
PUT    /api/invitations/{id}/accept
PUT    /api/invitations/{id}/decline
PUT    /api/invitations/{id}/cancel
```

#### WebSocket Topics
```
/topic/qsos              # New QSO notifications (real-time map updates)
/topic/scoring/{logId}   # Live score updates
/topic/telemetry/*       # Rig telemetry (all stations)
/topic/telemetry/{id}    # Station-specific rig telemetry
```

### Callsign Lookup Services
- **QRZ XML API:** Session-based authentication, XML parsing via `DocumentBuilder`
- **FCC ULS JSON:** Direct HTTP lookup
- **Config:** `qrz.api.username` / `qrz.api.password` (env: `QRZ_USERNAME` / `QRZ_PASSWORD`)
- **Note:** `UriComponentsBuilder.fromHttpUrl()` removed in Spring 7 — URL encoding uses plain string concatenation with `URLEncoder.encode()`

---

## Definition of Done (System-Wide)

### Code Quality
- [x] All code follows project conventions
- [x] No compiler warnings or errors
- [x] TypeScript strict mode enabled
- [x] Java 25 features utilized appropriately
- [x] Proper error handling implemented
- [x] Logging configured appropriately

### Testing
- [x] Unit tests for business logic (230 backend tests, 100% passing)
- [x] Integration tests for API endpoints (H2 in-memory test database)
- [ ] Frontend unit tests (not yet implemented)
- [ ] E2E tests for critical user flows (not yet implemented)

### Documentation
- [x] Code comments for complex logic
- [x] JavaDoc for public service methods
- [x] README files for setup
- [x] This agile specification document updated

### Performance
- [x] Map loads in <2 seconds (10k QSOs)
- [x] Clustering algorithm <500ms
- [x] WebSocket reconnection works
- [x] Mobile performance acceptable

### Security
- [x] JWT authentication implemented and enforced
- [x] Role-based authorization enforced at service layer
- [x] BCrypt password hashing
- [x] No plaintext credentials stored
- [x] No email addresses collected or stored

---

## Product Backlog (Remaining Items)

### Completed (this sprint)
- [x] Filter preset save/load — localStorage (UI implemented); backend persistence pending (low priority)
- [x] Award tracking — DXCC, WAS, VUCC dashboard (`AwardProgressComponent`, `AwardController`)
- [x] LoTW confirmation sync — `LotwSyncService` + `LotwSyncController`; frontend sync form in import panel
- [x] DX cluster spotting — `DXClusterService` polling DX Summit; `DxClusterPanelComponent`
- [x] Propagation prediction overlays — NOAA SWPC solar data; per-band conditions panel
- [x] Print-ready QSL card generation — `QslCardComponent` modal + print CSS
- [x] Station color preference persistence to backend — `UserPreferencesController` + `User.stationColorPreferences`

### High Priority (remaining)
- [ ] Frontend unit tests
- [ ] E2E tests for critical user flows

### Medium Priority (remaining)
- [ ] QSL card photo attachments (file upload per QSO)

### Low Priority
- [ ] Offline mode with local-first sync
- [ ] Mobile native apps (iOS/Android)
- [ ] Voice logging via speech recognition
- [ ] AI-powered contact suggestions
- [ ] Social features (friends list, activity feed)
- [ ] Custom map styles and themes
- [ ] Satellite pass predictions

---

## Known Limitations

- **eQSL / QRZ QSO confirmation** — not integrated; LoTW confirmation sync is implemented via `LotwSyncController`
- **Frontend unit tests** — not yet implemented
- **E2E tests** — not yet implemented
- **QSL card photo attachments** — QSL card print generation is implemented; photo file upload per QSO is not
- **Filter preset backend persistence** — presets are saved to localStorage only; server-side persistence is not yet implemented
- PITest (mutation testing) does not support Java 25 — skip or ignore PITest goals in CI
- `MapController.java` uses `@CrossOrigin(origins = "*")` — acceptable for development; tighten for production deployment
- `console.log` debug statements present in `websocket.service.ts`, `qso-map` component, and auth components — benign, not user-facing

---

## Release Notes

### Version 1.2.0 — March 2026

#### New in v1.2.0
- **Log Type Separation (Epic 12):** PERSONAL and SHARED log types enforced at backend. Personal logs always private; invitations rejected. Log selector split into labeled sections with separate create buttons.
- **Rig Control Integration (Epic 11):** Standalone rig control microservice with Hamlib/rigctld integration, multi-client shared broker, three WebSocket endpoints, PTT locking, smart caching, and frontend rig-status component with QSO form auto-population. Cloud relay architecture (US-11.6) enables remote station connections via a backend gateway. PTT safety timeout (US-11.7) and WebSocket API key authentication (US-11.8) are planned for Sprint 2.
- **Contest Validators — CQWW, ARRL Sweepstakes, State QSO Party (Epic 10 expansion):** Three additional validators added (CQWWValidator 30 tests, ARRLSweepstakesValidator 42 tests, StateQSOPartyValidator 27 tests). Total validator count: 7.
- **Bonus Point Tracking:** `Log.bonusMetadata` JSON field and `ScoringService.calculateBonusPoints()` read bonus definitions from contest config JSON files.
- **Frontend RBAC Fix:** `permissions.service.ts` now uses `log.userRole` from backend (CREATOR/STATION/VIEWER) rather than only `creatorId` comparison — invited STATION participants now have correct write permissions.
- **Map Wiring:** `FullscreenMapViewComponent` subscribes to `LogService.currentLog$`, passes `[logId]` and `[filters]` to `<app-qso-map>`, and wires filter/overlay change handlers.
- **Location Management:** `LocationManagementService` extended with `updateUserLocation(String username, ...)` overload. `MapController.setUserLocation()` uses `Authentication` parameter.
- **Java 25 LTS upgrade:** Runtime image switched to `eclipse-temurin:25-jre-noble` (Ubuntu Noble). `curl` added to runtime image for healthchecks. Lombok upgraded to 1.18.42.
- **Spring Boot 4.0.3 upgrade:** All dependencies updated to compatible versions.

---

### Version 1.1.0 — February 2026

#### New in v1.1.0
- **Contest Validation & Scoring (Epic 10 initial):** Plugin-based `ContestValidator` architecture. Four validators: FieldDayValidator (76 tests), POTAValidator (14 tests), SOTAValidator (13 tests), WinterFieldDayValidator (16 tests). `ScoringService`, `MultiplierTrackingService`, `DuplicateDetectionService`. Frontend `score-summary` component.
- **Multi-User Collaboration (Epic 9):** Role-based access control (CREATOR / STATION / VIEWER). In-app invitation system (by username or callsign). Full invitation lifecycle (PENDING / ACCEPTED / DECLINED / CANCELLED / EXPIRED). Participant management and station assignment (1-10 + GOTA) with color badges. Personal-to-Shared conversion endpoint.
- **Authentication & Security (Epic 8):** JWT-based authentication with 24-hour expiry. Username-only registration (no email). BCrypt password hashing. Admin bootstrap via `ADMIN_USERNAME` / `ADMIN_PASSWORD` environment variables. Angular route guards on all authenticated pages.
- **Callsign Lookup:** QRZ XML API (session management, XML parsing) and FCC ULS JSON lookup implemented.
- **Test Infrastructure:** Switched integration test database from SQLite in-memory to H2 in-memory to resolve Hibernate 7.x `ObjectOptimisticLockingFailureException`. `spring.sql.init.mode=never` in test profile; `spring.jpa.open-in-view=false` in test profile.

---

### Version 1.0.0 — December 2025

#### Initial Release
- **Core Logging System (Epic 1):** Multi-station log management with unlimited logs, 25+ QSO fields, real-time validation, ADIF 3.1.4 import/export, Cabrillo export.
- **Geographic Visualization & Mapping (Epic 2):** Interactive Leaflet.js map, adaptive server-side clustering (10k threshold), Maidenhead grid overlay (4 precision levels), heatmap density visualization, multi-station SVG pie chart clusters, 15-minute pulse animations, contest overlay layers (CQ zones, ITU zones, ARRL sections, DXCC), GeoJSON/KML/CSV/ADIF export, real-time WebSocket updates.
- **Advanced Filtering (Epic 3):** 10-filter system with visual filter pills. Mobile-responsive bottom sheet with swipe gestures.
- **DXCC Management (Epic 4):** CTY.DAT loading and parsing, 340+ entities, longest-match algorithm, cached lookups.
- **Session & Location Management (Epic 5):** Hierarchical location fallback (station → user → session → manual), session override with 1-hour TTL.
- **UI & UX (Epic 6):** EtherWave Archive branding, glassmorphism theme, dark/light mode, fully responsive design.
- **Performance (Epic 7):** Server-side adaptive clustering, Spring Cache for DXCC, in-memory session locations.

---

## System Requirements

- **Browser:** Chrome 90+, Firefox 88+, Safari 14+, Edge 90+
- **Screen:** 320px minimum width
- **Network:** Broadband recommended for real-time features
- **Server:** Java 25 LTS, PostgreSQL 13+ (production) or SQLite (field)
- **Containers:** Docker Engine 24+ with Compose plugin v5.x

---

## Support & Contribution

### Getting Started
1. Clone repository
2. Backend: `mvn clean install && mvn spring-boot:run`
3. Frontend: `npm install && npm start`
4. Docker (production): `docker compose up -d`
5. Docker (field): `docker compose -f docker-compose.field.yml up -d`
6. Access: `http://localhost:4200`

### Issue Reporting
- Use GitHub Issues for bug reports
- Include: Browser, OS, steps to reproduce, screenshots
- Label appropriately: bug, enhancement, question

### Contributing
- Follow Angular style guide for frontend
- Follow Spring Boot best practices for backend
- Write tests for new features (minimum: service-layer unit tests)
- Submit pull requests against `develop` branch

---

## Appendix A: Glossary

**ADIF** - Amateur Data Interchange Format, standard for ham radio log interchange
**ARRL** - American Radio Relay League, US amateur radio organization
**Band** - Frequency range (e.g., 20M = 14.000-14.350 MHz)
**Cabrillo** - Standard log submission format for contest adjudication
**CQ Zone** - One of 40 geographic zones used in contest scoring
**CTY.DAT** - Standard file format for DXCC entity and prefix data
**DXCC** - DX Century Club, award for contacting 100+ DXCC entities
**FT8/FT4** - Digital weak-signal modes (WSJT-X)
**GOTA** - Get On The Air — special station category in ARRL Field Day
**Grid Square** - Maidenhead locator system coordinate
**Hamlib** - Open-source rig control library; `rigctld` is its TCP daemon
**ITU Zone** - One of 90 zones defined by the International Telecommunication Union
**LoTW** - Logbook of the World, ARRL's QSO confirmation system
**Maidenhead** - Geographic grid locator system used in amateur radio
**Mode** - Transmission type (SSB, CW, FT8, etc.)
**POTA** - Parks on the Air, award program for activating parks
**QSO** - Radio contact between two stations
**QSL** - Confirmation of a radio contact
**QTH** - Station location
**RST** - Signal report (Readability, Strength, Tone)
**SOTA** - Summits on the Air, award program for activating mountain summits
**Station** - Radio equipment configuration, numbered 1-10 or GOTA in this system
**WAS** - Worked All States award
**VUCC** - VHF/UHF Century Club, grid square award

---

## Appendix B: Statistics

### Feature Coverage
- **Epics Completed:** 12 of 12 (100%)
- **User Stories Delivered:** 46
- **Contest Validators:** 7
- **Backend Tests:** 230 (100% passing)
- **Frontend Tests:** 0 (not yet implemented)
- **API Endpoints:** 30+ REST + 4 WebSocket topics

### Performance Metrics
- **Map Load Time:** <2 seconds (10k QSOs)
- **Clustering Performance:** <500ms
- **WebSocket Latency:** <100ms
- **Bundle Size:** ~915 KB uncompressed

---

**Document Version:** 1.2.0
**Last Updated:** 2026-03-02
**Author:** EtherWave Archive Development Team
**Status:** Production Ready
