# Multi-Station Contest Logging System - Implementation Plan

**Date:** 2025-12-04
**Project:** EtherWave Archive
**Feature:** Multi-Station Contest Logging with GOTA Support

---

## 📋 Overview

Transform the EtherWave Archive into a comprehensive multi-station contest logging system for club operations, Field Day, and other multi-operator events. Support up to 1000 stations plus separate GOTA (Get On The Air) operations.

---

## ✅ Design Decisions

### 1. QSO Storage & Tagging
**Decision:** Auto-tag every QSO with operator's assigned station/GOTA
**Implementation:**
- Add `stationNumber` (Integer, nullable) to QSO entity
- Add `isGota` (Boolean, default false) to QSO entity
- Auto-populate from logged-in user's participant assignment
- Mutually exclusive: station OR GOTA, never both

**Why:** Industry standard (N1MM+, WriteLog), enables proper scoring, duplicate checking, and flexible reporting.

### 2. Dashboard Statistics
**Decision:** Full suite of statistics per station
**Display:**
- QSO count per station
- Points/Score per station
- Band breakdown (20m, 40m, 80m, etc.)
- Mode breakdown (SSB, CW, Digital)
- Recent QSOs (last 5-10 contacts)
- Rate (QSOs/hour) per station

**Why:** Comprehensive real-time visibility for serious contest operations.

### 3. QSO Entry Auto-Tagging
**Decision:** Fully automatic - no user input required
**Behavior:**
- User logs in with assigned station (e.g., Station 2)
- Every QSO automatically tagged `stationNumber=2`
- No manual selection, no extra clicks
- Fast, error-free workflow

**Why:** Fastest workflow, prevents operator errors, matches professional contest software.

### 4. Dashboard View Organization
**Decision:** Tabbed interface
**Tabs:**
- "All" - Combined view of all stations
- "Station 1" - Only Station 1 QSOs
- "Station 2" - Only Station 2 QSOs
- ... (dynamic based on assignments)
- "GOTA" - Only GOTA QSOs

**Why:** Maximum flexibility - coordinators use "All", operators focus on their tab.

### 5. GOTA Display & Scoring
**Decision:** Separate in statistics, integrated in QSO list
**Statistics Display:**
```
Main Stations:  83 QSOs | 420 pts | 27/hr
GOTA Station:   12 QSOs |  48 pts |  4/hr
────────────────────────────────────────
Total QSOs:     95 (GOTA not scored)
```

**QSO List:** GOTA included with other stations, filterable via GOTA tab

**Why:**
- Contest compliance (GOTA never counted in main score)
- Operational visibility (monitor GOTA progress)
- Clean separation for export

### 6. Score Summary Enhancement
**Decision:** Full breakdown with leaderboard
**Display:**
```
╔═══════════════════════════════════════════════╗
║  📊 CONTEST SCORE SUMMARY - Field Day 2025    ║
╠═══════════════════════════════════════════════╣
║  Main Contest Score:                          ║
║    Station 1: 45 QSOs | 230 pts | 15/hr  🥇   ║
║    Station 2: 38 QSOs | 190 pts | 12/hr  🥈   ║
║    Station 3: 22 QSOs | 110 pts |  8/hr       ║
║    ─────────────────────────────────────────  ║
║    Total:    105 QSOs | 530 pts | 35/hr       ║
║                                               ║
║  GOTA (Bonus):                                ║
║    GOTA:     12 QSOs  |  48 pts |  4/hr       ║
║                                               ║
║  Multipliers: 15 sections                     ║
║  Final Score: 7,950                           ║
╚═══════════════════════════════════════════════╝
```

**Features:**
- Per-station performance metrics
- Leaderboard medals (🥇🥈🥉)
- Main vs GOTA separation
- Real-time rate calculation

**Why:** Matches full suite statistics choice, provides competitive motivation.

---

## 🔧 Technical Implementation

### Phase 1: Backend - QSO Entity & Auto-Tagging

#### 1.1 Update QSO Entity
**File:** `/backend/src/main/java/com/hamradio/logbook/entity/QSO.java`

Add fields:
```java
@Column(name = "station_number")
private Integer stationNumber; // 1-1000, null if unassigned

@Column(name = "is_gota", nullable = false)
private Boolean isGota = false; // GOTA designation
```

#### 1.2 Update QSO Request/Response DTOs
**Files:**
- `/backend/src/main/java/com/hamradio/logbook/dto/qso/QSORequest.java`
- `/backend/src/main/java/com/hamradio/logbook/dto/qso/QSOResponse.java`

Add stationNumber and isGota fields to both DTOs.

#### 1.3 Modify QSO Service - Auto-Tagging Logic
**File:** `/backend/src/main/java/com/hamradio/logbook/service/QSOService.java`

In `createQSO()` method:
1. Get current user's participant record for this log
2. Extract `stationNumber` and `isGota` from participant
3. Auto-populate QSO with these values
4. Validate: cannot have both station and GOTA

```java
// Pseudo-code
LogParticipant participant = getParticipantForUser(log, user);
qso.setStationNumber(participant.getStationNumber());
qso.setIsGota(participant.getIsGota());
```

### Phase 2: Backend - Statistics & Scoring

#### 2.1 Create Station Statistics Service
**New File:** `/backend/src/main/java/com/hamradio/logbook/service/StationStatisticsService.java`

Methods:
- `getStationStats(logId, stationNumber)` → QSO count, points, rate
- `getGotaStats(logId)` → GOTA-specific stats
- `getAllStationStats(logId)` → All stations + GOTA
- `getBandBreakdown(logId, stationNumber)` → QSOs per band
- `getModeBreakdown(logId, stationNumber)` → QSOs per mode
- `getRecentQSOs(logId, stationNumber, limit)` → Recent contacts
- `calculateRate(logId, stationNumber)` → QSOs/hour

#### 2.2 Create Station Statistics DTO
**New File:** `/backend/src/main/java/com/hamradio/logbook/dto/stats/StationStatistics.java`

Fields:
```java
private Integer stationNumber;
private Boolean isGota;
private Integer qsoCount;
private Integer points;
private Double qsoRate; // QSOs per hour
private Map<String, Integer> bandBreakdown; // "20m": 15, "40m": 22
private Map<String, Integer> modeBreakdown; // "SSB": 30, "CW": 7
private List<QSOResponse> recentQSOs;
private Integer rank; // 1st, 2nd, 3rd place
```

#### 2.3 Update Scoring Service
**File:** `/backend/src/main/java/com/hamradio/logbook/service/ScoringService.java`

Enhance to:
- Calculate scores per station
- Calculate GOTA score separately
- Generate leaderboard ranking
- Exclude GOTA from main contest score

#### 2.4 Create Station Statistics Controller Endpoint
**File:** `/backend/src/main/java/com/hamradio/logbook/controller/StationStatisticsController.java`

Endpoints:
- `GET /api/logs/{logId}/stats/stations` → All station stats
- `GET /api/logs/{logId}/stats/stations/{stationNumber}` → Single station
- `GET /api/logs/{logId}/stats/gota` → GOTA stats
- `GET /api/logs/{logId}/stats/summary` → Combined summary for score panel

### Phase 3: Frontend - Models & Services

#### 3.1 Create Station Statistics Models
**File:** `/frontend/logbook-ui/src/app/models/station-stats.model.ts`

Interfaces:
```typescript
export interface StationStatistics {
  stationNumber?: number;
  isGota: boolean;
  qsoCount: number;
  points: number;
  qsoRate: number; // QSOs per hour
  bandBreakdown: { [band: string]: number };
  modeBreakdown: { [mode: string]: number };
  recentQSOs: QSO[];
  rank?: number;
}

export interface StationStatsSummary {
  mainStations: StationStatistics[];
  gota?: StationStatistics;
  mainTotal: {
    qsoCount: number;
    points: number;
    qsoRate: number;
  };
  overallTotal: {
    qsoCount: number;
  };
}
```

#### 3.2 Create Station Statistics Service
**New File:** `/frontend/logbook-ui/src/app/services/station-stats/station-stats.service.ts`

Methods:
```typescript
getStationStats(logId: number): Observable<StationStatistics[]>
getStationSummary(logId: number): Observable<StationStatsSummary>
getGotaStats(logId: number): Observable<StationStatistics>
```

### Phase 4: Frontend - Enhanced Score Summary Component

#### 4.1 Update Score Summary Component
**File:** `/frontend/logbook-ui/src/app/components/score-summary/score-summary.component.ts`

Enhancements:
- Load station statistics from new service
- Display per-station breakdown
- Show leaderboard medals (🥇🥈🥉)
- Separate main vs GOTA totals
- Real-time updates

#### 4.2 Update Score Summary Template
**File:** `/frontend/logbook-ui/src/app/components/score-summary/score-summary.component.html`

New layout:
- Main Contest Score section with per-station rows
- GOTA section (if GOTA exists)
- Totals footer
- Leaderboard indicators

### Phase 5: Frontend - Tabbed QSO List

#### 5.1 Update QSO List Component
**File:** `/frontend/logbook-ui/src/app/components/qso-list/qso-list.component.ts`

Add:
- Tab state management
- Filter logic per tab
- Dynamic tab generation based on active stations
- Color-coded station badges in QSO rows

#### 5.2 Update QSO List Template
**File:** `/frontend/logbook-ui/src/app/components/qso-list/qso-list.component.html`

Add:
- Tab navigation bar
- Station filter logic
- Station badge display in each QSO row
- Color coding using EtherWave Archive station colors

### Phase 6: Frontend - Station Statistics Panels

#### 6.1 Create Station Stats Panel Component
**New Files:**
- `/frontend/logbook-ui/src/app/components/station-stats-panel/station-stats-panel.component.ts`
- `/frontend/logbook-ui/src/app/components/station-stats-panel/station-stats-panel.component.html`
- `/frontend/logbook-ui/src/app/components/station-stats-panel/station-stats-panel.component.css`

Display:
- Per-station detailed statistics
- Band breakdown chart/table
- Mode breakdown chart/table
- Recent QSOs for that station
- Rate graph/indicator

#### 6.2 Integrate into Dashboard
**File:** `/frontend/logbook-ui/src/app/components/dashboard/dashboard.component.html`

Add station stats panel in appropriate row (after score summary).

### Phase 7: Frontend - QSO Entry Auto-Tagging

#### 7.1 Update QSO Entry Component
**File:** `/frontend/logbook-ui/src/app/components/qso-entry/qso-entry.component.ts`

Modifications:
- Remove manual station selection (if any)
- Auto-populate stationNumber from current user's participant data
- Auto-populate isGota from current user's participant data
- Display assignment in form (read-only badge)
- Send to backend on save

#### 7.2 Update QSO Entry Template
**File:** `/frontend/logbook-ui/src/app/components/qso-entry/qso-entry.component.html`

Add read-only display:
```html
<div class="alert alert-info">
  Logging as:
  <span class="badge" [style.background-color]="getStationColor()">
    {{ getStationLabel() }}
  </span>
</div>
```

---

## 🎨 UI/UX Design Guidelines

### Color Coding
- **Station 1:** Blue (#1E88E5)
- **Station 2:** Red (#E53935)
- **GOTA:** Green (#43A047)
- **Unassigned:** Gray (#9E9E9E)

### Badges
Use colored badges consistently:
- In QSO list rows
- In statistics panels
- In score summary
- In navbar (already implemented)

### Responsive Design
- Full breakdowns on desktop
- Condensed stats on mobile
- Scrollable tables for many stations
- Collapsible sections on small screens

---

## 📊 Export Compliance

### Current Implementation (Already Compliant)
✅ **Non-GOTA Export** - Official contest submission (excludes all GOTA QSOs)
✅ **GOTA Only Export** - Separate GOTA submission
✅ **Combined Export** - Backup/review only (includes everything)

### No Changes Required
The export system is already contest-legal. GOTA will never be mixed into the main contest log export.

---

## 🧪 Testing Plan

### Unit Tests
- QSO auto-tagging logic
- Station statistics calculations
- Rate calculations (QSOs/hour)
- Score calculation with/without GOTA
- Leaderboard ranking algorithm

### Integration Tests
- Create shared log → assign stations → log QSOs → verify auto-tagging
- Multi-station scoring calculations
- Export separation (main vs GOTA)
- Tab filtering functionality

### Manual Testing Scenarios
1. **Field Day Simulation:**
   - Create shared log for Field Day
   - Assign 3 operators to Station 1, 2, GOTA
   - Each logs 10 QSOs
   - Verify auto-tagging, stats, score summary, exports

2. **Station Reassignment:**
   - Move operator from Station 1 to Station 2
   - Verify new QSOs tagged with Station 2
   - Verify old QSOs remain Station 1
   - Verify statistics update correctly

3. **GOTA Isolation:**
   - Verify GOTA QSOs excluded from main score
   - Verify GOTA tab shows only GOTA QSOs
   - Verify Non-GOTA export excludes GOTA
   - Verify GOTA export includes only GOTA

---

## 📅 Implementation Phases

### Phase 1: Backend Foundation (2-3 hours)
- Update QSO entity with station fields
- Implement auto-tagging in QSOService
- Create StationStatisticsService
- Add statistics endpoints

### Phase 2: Backend Statistics (1-2 hours)
- Implement all statistics calculations
- Add leaderboard ranking
- Update scoring service for GOTA separation

### Phase 3: Frontend Models & Services (1 hour)
- Create station statistics models
- Implement StationStatisticsService
- Update QSO models

### Phase 4: Frontend Score Summary (1-2 hours)
- Enhanced score summary component
- Per-station breakdown display
- Leaderboard medals
- Main/GOTA separation

### Phase 5: Frontend Tabbed QSO List (2 hours)
- Tab navigation implementation
- Filter logic per tab
- Station badge display
- Color coding

### Phase 6: Frontend Station Stats Panels (2 hours)
- Create station stats panel component
- Band/mode breakdowns
- Recent QSOs display
- Rate indicators

### Phase 7: QSO Entry Auto-Tagging (1 hour)
- Remove manual station selection
- Auto-populate from participant
- Display assignment badge

### Phase 8: Testing & Refinement (2 hours)
- Unit tests
- Integration tests
- Manual testing
- Bug fixes

**Total Estimated Time:** 12-15 hours

---

## 🚀 Deployment Considerations

### Database Migration
- Add `station_number` column to `qsos` table (nullable Integer)
- Add `is_gota` column to `qsos` table (Boolean, default false)
- No data migration needed (existing QSOs will have null station)

### Backward Compatibility
- Existing QSOs without station assignment will show as "Unassigned"
- Export will include all QSOs regardless of station assignment
- Personal logs unaffected (station features only visible for shared logs)

### Performance
- Add database indexes on `qsos.station_number` and `qsos.is_gota` for fast filtering
- Cache station statistics with 30-second refresh
- Use WebSocket updates for real-time rate calculations

---

## 📖 User Documentation Updates Needed

### Update Files:
- `docs/USER_GUIDE.md` - Add multi-station operations section
- `docs/CONTEST_GUIDE.md` - Create new guide for contest operations
- `README.md` - Update features list

### New Sections:
1. **Multi-Station Setup**
   - Creating shared logs
   - Assigning stations to operators
   - Understanding GOTA

2. **Contest Operations**
   - QSO auto-tagging
   - Monitoring station performance
   - Using the tabbed interface
   - Understanding the score summary

3. **Exports for Contests**
   - Non-GOTA export (main submission)
   - GOTA export (bonus submission)
   - Contest compliance

---

## ✅ Success Criteria

### Functional Requirements
✅ QSOs automatically tagged with operator's station/GOTA
✅ Per-station statistics (QSO count, points, band, mode, rate)
✅ Tabbed QSO list (All, Station 1, Station 2, GOTA)
✅ Enhanced score summary with per-station breakdown
✅ Leaderboard ranking with medals
✅ GOTA separated in scoring, integrated in QSO list
✅ Export compliance (GOTA never in main contest log)

### Performance Requirements
✅ Statistics update in < 1 second
✅ Tab switching instant (client-side filtering)
✅ Real-time rate calculations
✅ Handle 1000+ QSOs without lag

### UX Requirements
✅ No extra clicks for operators (auto-tagging)
✅ Clear visual station identification (color badges)
✅ Intuitive tabbed navigation
✅ Mobile-responsive design
✅ Competitive leaderboard motivation

---

## 🎯 Future Enhancements (Post-MVP)

### Potential Additions:
1. **Band/Mode Matrix View** - Visual grid showing coverage
2. **Duplicate Detection** - Cross-station dupe checking with alerts
3. **Rate Graphs** - Visual QSO rate over time per station
4. **Station Messages** - Internal chat between operators
5. **Auto Station Assignment** - Suggest optimal station based on band conditions
6. **Multi-Log Comparison** - Compare performance across multiple contests
7. **Historical Statistics** - Track station performance over multiple events
8. **Audio Alerts** - Sound when station reaches milestones

---

## 📝 Notes & Decisions Log

### Design Decisions
- **Why auto-tag instead of manual?** Fast workflow, prevents errors, matches industry standard
- **Why tabbed instead of multi-column?** Better on mobile, cleaner UI, easier filtering
- **Why separate GOTA in stats?** Contest compliance, clear scoring rules
- **Why full suite instead of minimal stats?** User requested comprehensive visibility

### Technical Decisions
- **Why add to QSO entity?** Direct relationship, enables filtering at database level
- **Why not separate QSO tables per station?** Complicates queries, harder to maintain, no performance benefit
- **Why cache statistics?** Expensive calculations, 30s staleness acceptable for contest logging
- **Why WebSocket for rates?** Real-time updates important for competitive operations

---

**Document Version:** 1.0
**Last Updated:** 2025-12-04
**Status:** Approved - Ready for Implementation

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
