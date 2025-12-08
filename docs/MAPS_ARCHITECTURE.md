# EtherWave Archive - Interactive Mapping System Architecture

## 📋 Document Information

**Version:** 1.0.0
**Last Updated:** December 8, 2025
**Status:** Architecture Design
**Author:** EtherWave Archive Team

---

## 🎯 System Overview

The Interactive Mapping System provides WaveLog-style map visualization with EtherWave Archive branding and professional signal-analysis aesthetic. The system includes:

- **Global QSO Map** with real-time updates and server-side clustering
- **Grid Square Coverage Map** (Maidenhead locator system)
- **Heatmap Overlay** for QSO density visualization
- **Contest-Specific Overlays** (ARRL sections, CQ zones, IARU zones)
- **Real-time Multiplier Tracking** for contest operations
- **Comprehensive Filtering** (10 filter types)
- **Multiple Layer Support** (6 layer types)
- **Full Offline Support** with tile and data caching
- **Mobile-First Design** with bottom sheet interface
- **Access Control** (creator-only for shared logs)

---

## 🏗️ Architecture Principles

### Design Goals

1. **Performance First**: Handle 100k+ QSO databases with sub-second load times
2. **Real-time Updates**: Live QSO additions via WebSocket with <100ms latency
3. **Offline Capable**: Full functionality without internet connection
4. **Contest-Aware**: Automatic overlay switching based on log contest type
5. **Mobile-Optimized**: Bottom sheet design for logging while viewing map
6. **Extensible**: Plugin architecture for future propagation overlays
7. **Accessible**: WCAG AA compliant, keyboard navigation, screen reader support

### Technology Stack

**Frontend:**
- Leaflet 1.9.x (mapping library)
- Leaflet.heat (heatmap plugin)
- Leaflet.markercluster (cluster management)
- Canvas rendering for animations
- Service Worker (offline support)

**Backend:**
- Spring Boot 4.0.0 (Java 25)
- PostgreSQL/SQLite (spatial queries)
- WebSocket (real-time updates)
- Haversine distance calculations
- CTY.DAT prefix database

---

## 🗄️ Database Schema

### New Tables

#### `qso_locations`
Cached location data for QSOs to avoid repeated geocoding.

```sql
CREATE TABLE qso_locations (
    id BIGSERIAL PRIMARY KEY,
    qso_id BIGINT NOT NULL REFERENCES qsos(id) ON DELETE CASCADE,

    -- Operator location (hierarchical fallback)
    operator_lat DECIMAL(9,6),
    operator_lon DECIMAL(9,6),
    operator_grid VARCHAR(8),
    location_source VARCHAR(20), -- 'STATION', 'USER', 'SESSION', 'MANUAL'

    -- Contact location
    contact_lat DECIMAL(9,6),
    contact_lon DECIMAL(9,6),
    contact_grid VARCHAR(8),
    contact_dxcc VARCHAR(10),
    contact_continent VARCHAR(2),
    contact_cq_zone INT,
    contact_itu_zone INT,

    -- Distance calculation (cached)
    distance_km DECIMAL(10,2),
    distance_mi DECIMAL(10,2),
    bearing DECIMAL(5,2),

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(qso_id)
);

CREATE INDEX idx_qso_locations_qso ON qso_locations(qso_id);
CREATE INDEX idx_qso_locations_contact_lat_lon ON qso_locations(contact_lat, contact_lon);
CREATE INDEX idx_qso_locations_contact_grid ON qso_locations(contact_grid);
CREATE INDEX idx_qso_locations_dxcc ON qso_locations(contact_dxcc);
```

#### `maidenhead_grids`
Dynamic grid square database (worked grids + neighbors only).

```sql
CREATE TABLE maidenhead_grids (
    id BIGSERIAL PRIMARY KEY,
    grid VARCHAR(8) NOT NULL,
    precision INT NOT NULL, -- 2, 4, 6, 8

    -- Center coordinates
    center_lat DECIMAL(9,6) NOT NULL,
    center_lon DECIMAL(9,6) NOT NULL,

    -- Bounding box
    min_lat DECIMAL(9,6) NOT NULL,
    max_lat DECIMAL(9,6) NOT NULL,
    min_lon DECIMAL(9,6) NOT NULL,
    max_lon DECIMAL(9,6) NOT NULL,

    -- Statistics (per log)
    log_id BIGINT NOT NULL REFERENCES logbooks(id) ON DELETE CASCADE,
    qso_count INT DEFAULT 0,
    band_count INT DEFAULT 0,
    mode_count INT DEFAULT 0,
    first_qso_date TIMESTAMP,
    last_qso_date TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(grid, precision, log_id)
);

CREATE INDEX idx_grids_log ON maidenhead_grids(log_id);
CREATE INDEX idx_grids_grid ON maidenhead_grids(grid);
CREATE INDEX idx_grids_bounds ON maidenhead_grids(min_lat, max_lat, min_lon, max_lon);
```

#### `map_clusters`
Server-side clustering cache (per log, zoom level).

```sql
CREATE TABLE map_clusters (
    id BIGSERIAL PRIMARY KEY,
    log_id BIGINT NOT NULL REFERENCES logbooks(id) ON DELETE CASCADE,
    zoom_level INT NOT NULL, -- 0-18

    -- Cluster center
    lat DECIMAL(9,6) NOT NULL,
    lon DECIMAL(9,6) NOT NULL,

    -- Cluster metadata
    qso_count INT NOT NULL,
    station_breakdown JSONB, -- {"1": 45, "2": 30, "GOTA": 12}
    band_breakdown JSONB,
    mode_breakdown JSONB,

    -- Bounding box of clustered QSOs
    min_lat DECIMAL(9,6),
    max_lat DECIMAL(9,6),
    min_lon DECIMAL(9,6),
    max_lon DECIMAL(9,6),

    -- Filter state hash (for cache invalidation)
    filter_hash VARCHAR(64),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(log_id, zoom_level, lat, lon, filter_hash)
);

CREATE INDEX idx_clusters_log_zoom ON map_clusters(log_id, zoom_level);
CREATE INDEX idx_clusters_bounds ON map_clusters(min_lat, max_lat, min_lon, max_lon);
```

#### `dxcc_prefixes`
DXCC entity prefix database loaded from CTY.DAT.

```sql
CREATE TABLE dxcc_prefixes (
    id BIGSERIAL PRIMARY KEY,
    prefix VARCHAR(20) NOT NULL,
    dxcc_code INT NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    continent VARCHAR(2),
    cq_zone INT,
    itu_zone INT,

    -- Geographic center
    lat DECIMAL(9,6),
    lon DECIMAL(9,6),

    -- Metadata
    exact_match BOOLEAN DEFAULT false, -- true for =K1ABC style prefixes

    UNIQUE(prefix)
);

CREATE INDEX idx_dxcc_prefix ON dxcc_prefixes(prefix);
CREATE INDEX idx_dxcc_code ON dxcc_prefixes(dxcc_code);
CREATE INDEX idx_dxcc_continent ON dxcc_prefixes(continent);
```

### Schema Updates

#### `stations` table
Add location fields for operator position.

```sql
ALTER TABLE stations ADD COLUMN IF NOT EXISTS latitude DECIMAL(9,6);
ALTER TABLE stations ADD COLUMN IF NOT EXISTS longitude DECIMAL(9,6);
ALTER TABLE stations ADD COLUMN IF NOT EXISTS maidenhead_grid VARCHAR(8);
ALTER TABLE stations ADD COLUMN IF NOT EXISTS location_name VARCHAR(100); -- "Field Day Site 2025"
```

#### `users` table
Add default location for user (fallback when station location not set).

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS default_latitude DECIMAL(9,6);
ALTER TABLE users ADD COLUMN IF NOT EXISTS default_longitude DECIMAL(9,6);
ALTER TABLE users ADD COLUMN IF NOT EXISTS default_grid VARCHAR(8);
```

#### `qsos` table
No changes needed - use existing gridsquare field.

---

## 🔌 Backend API Endpoints

### Map Data Endpoints

#### `GET /api/maps/qsos/{logId}`
Get QSO location data with clustering.

**Query Parameters:**
- `zoom` (int, required): Map zoom level (0-18)
- `bounds` (string, optional): `minLat,minLon,maxLat,maxLon`
- `clusterThreshold` (int, optional): QSO count to trigger clustering (default: 10000)
- `pixelRadius` (int, optional): Cluster radius in pixels (default: zoom-dependent)
- `band` (string, optional): Filter by band (e.g., "20M")
- `mode` (string, optional): Filter by mode (e.g., "SSB")
- `station` (int, optional): Filter by station number
- `operator` (string, optional): Filter by operator callsign
- `dxcc` (string, optional): Filter by DXCC entity
- `dateFrom` (date, optional): Filter QSOs from date
- `dateTo` (date, optional): Filter QSOs to date
- `confirmed` (boolean, optional): Filter by confirmed status
- `continent` (string, optional): Filter by continent
- `state` (string, optional): Filter by state/province
- `exchange` (string, optional): Filter by exchange field

**Response:**
```json
{
  "type": "clustered", // or "individual"
  "clusters": [
    {
      "lat": 41.8781,
      "lon": -87.6298,
      "count": 45,
      "stations": {"1": 25, "2": 15, "GOTA": 5},
      "bands": {"20M": 20, "40M": 15, "80M": 10},
      "modes": {"SSB": 30, "CW": 10, "FT8": 5}
    }
  ],
  "individual": [
    {
      "qsoId": 12345,
      "callsign": "W1AW",
      "lat": 41.7144,
      "lon": -72.7273,
      "grid": "FN31pr",
      "dxcc": "K",
      "continent": "NA",
      "band": "20M",
      "mode": "SSB",
      "station": 1,
      "timestamp": "2025-06-29T14:23:00Z",
      "confirmed": true,
      "distance": 1234.5
    }
  ],
  "metadata": {
    "totalQsos": 45,
    "filteredQsos": 45,
    "clusteringApplied": true,
    "cacheHit": true
  }
}
```

#### `GET /api/maps/grids/{logId}`
Get worked grid squares for coverage map.

**Query Parameters:**
- `precision` (int, optional): Grid precision (2, 4, 6, 8) - default: auto-detect
- `includeNeighbors` (boolean, optional): Include neighboring grids (default: true)
- Filters: same as QSO endpoint

**Response:**
```json
{
  "grids": [
    {
      "grid": "FN31pr",
      "precision": 6,
      "centerLat": 41.75,
      "centerLon": -72.75,
      "bounds": {
        "minLat": 41.708,
        "maxLat": 41.792,
        "minLon": -72.833,
        "maxLon": -72.667
      },
      "qsoCount": 12,
      "bandCount": 3,
      "modeCount": 2,
      "firstQso": "2025-06-29T12:00:00Z",
      "lastQso": "2025-06-29T18:00:00Z"
    }
  ],
  "metadata": {
    "totalGrids": 87,
    "precision": 6,
    "autoDetected": true
  }
}
```

#### `GET /api/maps/heatmap/{logId}`
Get heatmap density data.

**Query Parameters:**
- Filters: same as QSO endpoint

**Response:**
```json
{
  "points": [
    {"lat": 41.8781, "lon": -87.6298, "intensity": 0.8},
    {"lat": 40.7128, "lon": -74.0060, "intensity": 0.6}
  ],
  "metadata": {
    "totalPoints": 256,
    "maxIntensity": 45
  }
}
```

#### `GET /api/maps/contest-overlays/{logId}`
Get contest-specific overlay data.

**Response:**
```json
{
  "contestType": "ARRL_FIELD_DAY",
  "overlays": [
    {
      "type": "ARRL_SECTION",
      "sections": [
        {
          "code": "IN",
          "name": "Indiana",
          "worked": true,
          "qsoCount": 5,
          "isMultiplier": true,
          "needed": false
        }
      ]
    }
  ],
  "multipliers": {
    "worked": 45,
    "total": 83,
    "needed": ["AK", "NT", "NWT"]
  }
}
```

#### `POST /api/maps/export/{logId}`
Export map data in various formats.

**Request Body:**
```json
{
  "format": "KML", // PNG, SVG, CSV, ADIF, KML, GEOJSON
  "filters": {}, // same filter options
  "includeMetadata": true,
  "includeStyles": true // for KML/GeoJSON
}
```

**Response:** File download with appropriate Content-Type

#### `GET /api/maps/distance/{logId}/{qsoId}`
Get cached distance calculation for QSO.

**Response:**
```json
{
  "qsoId": 12345,
  "distanceKm": 1234.5,
  "distanceMi": 767.2,
  "bearing": 45.2,
  "operatorLocation": {"lat": 41.8781, "lon": -87.6298, "grid": "EN51wc"},
  "contactLocation": {"lat": 41.7144, "lon": -72.7273, "grid": "FN31pr"}
}
```

### Configuration Endpoints

#### `PUT /api/maps/location/station/{stationId}`
Set station location (operator position).

**Request Body:**
```json
{
  "latitude": 41.8781,
  "longitude": -87.6298,
  "grid": "EN51wc", // optional, can be auto-calculated
  "locationName": "Field Day Site 2025"
}
```

#### `PUT /api/maps/location/user`
Set user default location (fallback).

**Request Body:**
```json
{
  "latitude": 41.8781,
  "longitude": -87.6298,
  "grid": "EN51wc"
}
```

#### `POST /api/maps/location/session/{logId}`
Set temporary session location (override for current session).

**Request Body:**
```json
{
  "latitude": 41.8781,
  "longitude": -87.6298,
  "grid": "EN51wc"
}
```

### WebSocket Events

#### Subscribe: `/topic/maps/{logId}`

**New QSO Event:**
```json
{
  "type": "NEW_QSO",
  "qso": {
    "qsoId": 12345,
    "callsign": "W1AW",
    "lat": 41.7144,
    "lon": -72.7273,
    "grid": "FN31pr",
    "station": 1,
    "timestamp": "2025-06-29T14:23:00Z"
  }
}
```

**QSO Updated Event:**
```json
{
  "type": "QSO_UPDATED",
  "qsoId": 12345,
  "updates": {
    "confirmed": true,
    "grid": "FN31pr"
  }
}
```

**Cluster Invalidation Event:**
```json
{
  "type": "CLUSTER_INVALIDATED",
  "zoomLevels": [10, 11, 12],
  "reason": "NEW_QSO"
}
```

---

## 🎨 Frontend Architecture

### Component Structure

```
src/app/components/maps/
├── qso-map/
│   ├── qso-map.component.ts          // Main map container
│   ├── qso-map.component.html
│   ├── qso-map.component.css
│   └── qso-map.component.spec.ts
├── grid-map/
│   ├── grid-map.component.ts         // Maidenhead coverage
│   ├── grid-map.component.html
│   ├── grid-map.component.css
│   └── grid-map.component.spec.ts
├── map-filters/
│   ├── map-filters.component.ts      // Filter panel
│   ├── map-filters.component.html
│   └── map-filters.component.css
├── map-layers/
│   ├── map-layers.component.ts       // Layer control panel
│   ├── map-layers.component.html
│   └── map-layers.component.css
├── map-export/
│   ├── map-export.component.ts       // Export dialog
│   ├── map-export.component.html
│   └── map-export.component.css
├── contest-overlay/
│   ├── contest-overlay.component.ts  // Contest-specific overlays
│   ├── contest-overlay.component.html
│   └── contest-overlay.component.css
├── mobile-map/
│   ├── mobile-map.component.ts       // Mobile bottom sheet
│   ├── mobile-map.component.html
│   └── mobile-map.component.css
└── shared/
    ├── cluster-icon.ts               // Pie chart cluster renderer
    ├── pulsar-marker.ts              // Animated marker
    ├── map-utils.ts                  // Utility functions
    └── map-types.ts                  // TypeScript interfaces
```

### Services

```
src/app/services/maps/
├── map-data.service.ts               // API calls for map data
├── map-clustering.service.ts         // Client-side cluster coordination
├── maidenhead.service.ts             // Grid square calculations
├── distance.service.ts               // Distance/bearing calculations
├── dxcc-lookup.service.ts            // DXCC entity lookups
├── map-cache.service.ts              // Offline tile/data caching
├── map-websocket.service.ts          // Real-time map updates
└── contest-overlay.service.ts        // Contest-specific logic
```

### Models

```typescript
// src/app/models/map.models.ts

export interface QSOLocation {
  qsoId: number;
  callsign: string;
  lat: number;
  lon: number;
  grid?: string;
  dxcc?: string;
  continent?: string;
  band: string;
  mode: string;
  station: number;
  timestamp: Date;
  confirmed: boolean;
  distance?: number;
}

export interface MapCluster {
  lat: number;
  lon: number;
  count: number;
  stations: { [key: number]: number };
  bands: { [key: string]: number };
  modes: { [key: string]: number };
}

export interface GridSquare {
  grid: string;
  precision: number;
  centerLat: number;
  centerLon: number;
  bounds: {
    minLat: number;
    maxLat: number;
    minLon: number;
    maxLon: number;
  };
  qsoCount: number;
  bandCount: number;
  modeCount: number;
  firstQso: Date;
  lastQso: Date;
}

export interface MapFilters {
  band?: string;
  mode?: string;
  station?: number;
  operator?: string;
  dxcc?: string;
  dateFrom?: Date;
  dateTo?: Date;
  confirmed?: boolean;
  continent?: string;
  state?: string;
  exchange?: string;
}

export interface MapLayer {
  id: string;
  name: string;
  enabled: boolean;
  type: 'overlay' | 'base';
  opacity: number;
}

export interface ContestOverlay {
  contestType: string;
  sections?: ContestSection[];
  zones?: ContestZone[];
  multipliers: {
    worked: number;
    total: number;
    needed: string[];
  };
}

export interface ContestSection {
  code: string;
  name: string;
  worked: boolean;
  qsoCount: number;
  isMultiplier: boolean;
  needed: boolean;
}
```

### Theme Integration

```typescript
// src/app/services/maps/map-theme.service.ts

export class MapThemeService {
  constructor(private themeService: ThemeService) {}

  getTileLayer(): string {
    if (this.themeService.isDarkTheme()) {
      // Dark theme tiles
      return 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';
    } else {
      // Light theme tiles
      return 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png';
    }
  }

  getStationColor(station: number): string {
    // Use existing station color utilities
    return getStationColor(station);
  }

  getClusterStyles(): any {
    return {
      fillColor: this.themeService.isDarkTheme() ? '#DAA520' : '#B8860B',
      strokeColor: '#003F87',
      textColor: '#FFFFFF'
    };
  }
}
```

---

## 🎭 UI/UX Design

### Desktop Layout

```
┌─────────────────────────────────────────────────────────────┐
│ [Navbar with EtherWave Logo]                                │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────┬──────────────────────────────┐    │
│  │ Map Filters          │ Map Layers                   │    │
│  │ □ Band: [20M ▼]     │ ☑ Worked but not confirmed  │    │
│  │ □ Mode: [SSB ▼]     │ ☑ LoTW confirmed            │    │
│  │ □ Station: [1 ▼]    │ ☑ Manual QSOs               │    │
│  │ □ DXCC: [K ▼]       │ ☑ DXCC shading              │    │
│  │ □ Date Range        │ ☑ CQ Zones                  │    │
│  │ [Clear Filters]     │ ☑ ITU Zones                 │    │
│  └──────────────────────┴──────────────────────────────┘    │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                   MAP AREA                          │    │
│  │                                                      │    │
│  │   [Interactive Leaflet Map with QSOs]              │    │
│  │                                                      │    │
│  │   Legend:                                           │    │
│  │   ● Station 1 (Blue)  ● Station 2 (Red)           │    │
│  │   ● GOTA (Green)      ○ Unconfirmed               │    │
│  │                                                      │    │
│  │   [Zoom Controls] [Layers] [Export] [📍My QTH]    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                               │
│  Stats: 245 QSOs | 87 Grids | 45 DXCC | Longest: 1234 km   │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Mobile Layout (Bottom Sheet)

```
┌──────────────────────┐
│ [☰] EtherWave  [⚙️]  │
├──────────────────────┤
│                      │
│   MAP VIEW           │
│   (Simplified)       │
│                      │
│   [📍] [Filters]     │
│                      │
├──────────────────────┤
│ ═══ Drag Handle ═══  │  ← Draggable
├──────────────────────┤
│ Recent QSOs          │
│ ┌──────────────────┐ │
│ │ W1AW 20M SSB     │ │
│ │ FN31pr • 1234km  │ │
│ └──────────────────┘ │
│ ┌──────────────────┐ │
│ │ K3LR 40M CW      │ │
│ │ EN90xq • 567km   │ │
│ └──────────────────┘ │
└──────────────────────┘
```

### Cluster Marker Design (Pie Chart)

```
    ┌─────────────┐
    │   ╱│╲       │   Multi-station cluster
    │  ╱ │ ╲      │   Blue = Station 1 (60%)
    │ ╱  │  ╲     │   Red = Station 2 (30%)
    │────┼────     │   Green = GOTA (10%)
    │    │         │
    │   [45]      │   Count in center
    └─────────────┘
```

### Pulsar Animation (Recent QSOs)

```css
@keyframes pulsar {
  0% {
    box-shadow: 0 0 0 0 rgba(67, 160, 71, 0.7);
    transform: scale(1);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(67, 160, 71, 0);
    transform: scale(1.1);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(67, 160, 71, 0);
    transform: scale(1);
  }
}

.pulsar-marker {
  animation: pulsar 2s infinite;
  border-radius: 50%;
  background: var(--qso-green);
  border: 2px solid #fff;
}
```

---

## ⚡ Performance Optimizations

### Server-Side Clustering

**Algorithm:**
1. Check QSO count for log/filter combination
2. If count > 10,000: enable clustering
3. Calculate pixel radius based on zoom:
   - Zoom 0-8: 80px radius
   - Zoom 9-12: 65px radius
   - Zoom 13-15: 50px radius
   - Zoom 16-18: Individual markers
4. Group QSOs within pixel radius at current zoom
5. Cache clusters by (logId, zoomLevel, filterHash)
6. Invalidate cache on new QSO or filter change

**Lazy Caching Strategy:**
```java
@Service
public class MapClusterService {

    @Cacheable(value = "mapClusters", key = "#logId + '_' + #zoom + '_' + #filterHash")
    public ClusterResponse getClusters(Long logId, int zoom, String filterHash) {
        // Check cache first
        List<MapCluster> cached = clusterRepository
            .findByLogIdAndZoomAndFilterHash(logId, zoom, filterHash);

        if (!cached.isEmpty() && !isCacheStale(cached.get(0))) {
            return buildResponse(cached);
        }

        // Cache miss or stale - recompute
        List<QSOLocation> qsos = qsoService.getLocations(logId, filters);

        if (qsos.size() > 10000) {
            List<MapCluster> clusters = computeClusters(qsos, zoom);
            clusterRepository.saveAll(clusters);
            return buildResponse(clusters);
        } else {
            return buildIndividualResponse(qsos);
        }
    }
}
```

### Frontend Optimizations

**Canvas Rendering for Animations:**
```typescript
// Render pulsar animations on canvas layer instead of DOM
export class PulsarLayer {
  private canvas: HTMLCanvasElement;
  private animationFrame: number;
  private markers: PulsarMarker[] = [];

  render() {
    const ctx = this.canvas.getContext('2d');
    ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

    this.markers.forEach(marker => {
      const phase = (Date.now() - marker.timestamp) % 2000 / 2000;
      const radius = 5 + (phase * 10);
      const alpha = 0.7 - (phase * 0.7);

      ctx.beginPath();
      ctx.arc(marker.x, marker.y, radius, 0, 2 * Math.PI);
      ctx.fillStyle = `rgba(67, 160, 71, ${alpha})`;
      ctx.fill();
    });

    this.animationFrame = requestAnimationFrame(() => this.render());
  }
}
```

**Virtual Scrolling for QSO List:**
```typescript
// Mobile bottom sheet with virtual scrolling
@Component({
  template: `
    <cdk-virtual-scroll-viewport itemSize="60" class="qso-list">
      <div *cdkVirtualFor="let qso of filteredQsos">
        <app-qso-card [qso]="qso"></app-qso-card>
      </div>
    </cdk-virtual-scroll-viewport>
  `
})
export class MobileMapComponent {
  // Only render visible QSOs
}
```

**Debounced Filter Updates:**
```typescript
filterChanges$ = new Subject<MapFilters>();

ngOnInit() {
  this.filterChanges$.pipe(
    debounceTime(300), // Wait 300ms after last change
    distinctUntilChanged(),
    switchMap(filters => this.mapDataService.getQSOs(this.logId, filters))
  ).subscribe(data => this.updateMap(data));
}
```

---

## 🔐 Access Control

### Permission System

```typescript
export class MapAccessControl {

  canViewMap(user: User, log: Logbook): boolean {
    // Creator always has access
    if (log.createdBy === user.id) {
      return true;
    }

    // For shared logs
    if (log.shared) {
      // Only creator can view maps
      return false;
    }

    // For non-shared logs (personal/team)
    if (log.owner === user.id || this.isTeamMember(user, log)) {
      return true;
    }

    return false;
  }

  hideMapUI(log: Logbook): boolean {
    // Hide map UI completely for non-creators on shared logs
    return log.shared && !this.isCreator(log);
  }
}
```

**UI Implementation:**
```html
<!-- Dashboard widget -->
<div class="card" *ngIf="canViewMap(currentLog)">
  <div class="card-header">
    <h5>📍 QSO Map</h5>
  </div>
  <div class="card-body">
    <app-qso-map [logId]="currentLog.id" [compact]="true"></app-qso-map>
  </div>
</div>

<!-- Navigation link -->
<li class="nav-item" *ngIf="canViewMap(currentLog)">
  <a class="nav-link" routerLink="/maps">
    <i class="bi bi-map"></i> Maps
  </a>
</li>
```

---

## 📡 Real-Time Updates

### WebSocket Integration

```typescript
@Injectable()
export class MapWebSocketService {
  private stompClient: any;

  connect(logId: number) {
    this.stompClient.subscribe(`/topic/maps/${logId}`, (message) => {
      const event = JSON.parse(message.body);
      this.handleMapEvent(event);
    });
  }

  private handleMapEvent(event: MapEvent) {
    switch (event.type) {
      case 'NEW_QSO':
        this.addQSOMarker(event.qso);
        this.invalidateGridCache();
        break;
      case 'QSO_UPDATED':
        this.updateQSOMarker(event.qsoId, event.updates);
        break;
      case 'CLUSTER_INVALIDATED':
        this.refreshClusters(event.zoomLevels);
        break;
    }
  }

  private addQSOMarker(qso: QSOLocation) {
    // Add pulsar animation for new QSO
    const marker = new PulsarMarker(qso);
    this.map.addLayer(marker);

    // Fade out after 30 seconds
    setTimeout(() => {
      marker.stopPulsar();
    }, 30000);
  }
}
```

---

## 🗺️ Contest Overlay System

### ARRL Field Day Sections

```typescript
@Injectable()
export class ARRLSectionOverlay {

  getSections(): ContestSection[] {
    return [
      { code: 'CT', name: 'Connecticut', bounds: [...] },
      { code: 'EMA', name: 'Eastern Massachusetts', bounds: [...] },
      { code: 'IN', name: 'Indiana', bounds: [...] },
      // ... all 83 sections
    ];
  }

  renderOverlay(map: L.Map, workedSections: string[]) {
    this.getSections().forEach(section => {
      const polygon = L.polygon(section.bounds, {
        color: workedSections.includes(section.code) ? '#4CAF50' : '#E53935',
        fillOpacity: 0.2,
        weight: 2
      });

      polygon.bindPopup(`
        <strong>${section.name} (${section.code})</strong><br>
        ${workedSections.includes(section.code) ? '✓ Worked' : '✗ Needed'}
      `);

      polygon.addTo(map);
    });
  }
}
```

### CQ Zone Overlay

```typescript
@Injectable()
export class CQZoneOverlay {

  getZones(): CQZone[] {
    return [
      { zone: 1, name: 'Western North America', bounds: [...] },
      { zone: 2, name: 'Eastern North America', bounds: [...] },
      // ... all 40 zones
    ];
  }

  renderOverlay(map: L.Map, workedZones: number[]) {
    this.getZones().forEach(zone => {
      const isWorked = workedZones.includes(zone.zone);
      const isMultiplier = this.isMultiplierZone(zone.zone);

      const polygon = L.polygon(zone.bounds, {
        color: isWorked ? '#4CAF50' : (isMultiplier ? '#F5C542' : '#9E9E9E'),
        fillOpacity: 0.15,
        weight: 1
      });

      polygon.bindPopup(`
        <strong>CQ Zone ${zone.zone}</strong><br>
        ${zone.name}<br>
        ${isWorked ? '✓ Worked' : isMultiplier ? '★ Needed Multiplier' : '✗ Needed'}
      `);

      polygon.addTo(map);
    });
  }
}
```

---

## 💾 Offline Support

### Service Worker Strategy

```typescript
// service-worker.js

const TILE_CACHE = 'map-tiles-v1';
const DATA_CACHE = 'map-data-v1';

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // Cache map tiles
  if (url.hostname.includes('basemaps.cartocdn.com')) {
    event.respondWith(
      caches.open(TILE_CACHE).then((cache) => {
        return cache.match(event.request).then((response) => {
          return response || fetch(event.request).then((fetchResponse) => {
            cache.put(event.request, fetchResponse.clone());
            return fetchResponse;
          });
        });
      })
    );
  }

  // Cache map data API calls
  if (url.pathname.startsWith('/api/maps/')) {
    event.respondWith(
      caches.open(DATA_CACHE).then((cache) => {
        return fetch(event.request).then((response) => {
          cache.put(event.request, response.clone());
          return response;
        }).catch(() => {
          return cache.match(event.request);
        });
      })
    );
  }
});
```

### Offline Indicator

```typescript
@Component({
  template: `
    <div class="offline-banner" *ngIf="isOffline">
      ⚠️ Offline Mode - Using cached map data
    </div>
  `
})
export class MapComponent {
  isOffline = false;

  ngOnInit() {
    window.addEventListener('online', () => this.isOffline = false);
    window.addEventListener('offline', () => this.isOffline = true);
  }
}
```

---

## 📊 Implementation Phases

### Phase 1: Foundation (Week 1)
- ✅ Database schema creation
- ✅ Maidenhead grid converter service
- ✅ Distance calculation service
- ✅ DXCC prefix database loader
- ✅ Basic map component with Leaflet
- ✅ Theme integration (dark/light tiles)

### Phase 2: Core Features (Week 2)
- ✅ QSO marker rendering
- ✅ Server-side clustering API
- ✅ Grid square coverage map
- ✅ Heatmap overlay
- ✅ Basic filter panel
- ✅ Real-time WebSocket updates

### Phase 3: Advanced Features (Week 3)
- ✅ Pie chart cluster markers
- ✅ Pulsar animations for recent QSOs
- ✅ All 10 filter types
- ✅ All 6 layer types
- ✅ Access control implementation
- ✅ Mobile bottom sheet UI

### Phase 4: Contest Integration (Week 4)
- ✅ Contest type detection
- ✅ ARRL section overlay
- ✅ CQ zone overlay
- ✅ ITU zone overlay
- ✅ Real-time multiplier tracking
- ✅ Contest-specific coloring

### Phase 5: Polish & Export (Week 5)
- ✅ Export functionality (PNG, SVG, KML, GeoJSON, CSV/ADIF)
- ✅ Offline support (Service Worker)
- ✅ Performance optimizations
- ✅ Mobile responsiveness
- ✅ Documentation
- ✅ Testing

---

## 🧪 Testing Strategy

### Unit Tests
- Maidenhead converter accuracy
- Distance calculations (Haversine)
- DXCC prefix matching
- Clustering algorithm correctness
- Filter logic

### Integration Tests
- API endpoint responses
- WebSocket message handling
- Database query performance
- Cache invalidation
- Access control

### E2E Tests
- Map loading and rendering
- Filter application
- Real-time QSO addition
- Export functionality
- Mobile bottom sheet interaction
- Offline mode

### Performance Tests
- 100k QSO load time < 2s
- Clustering computation < 500ms
- WebSocket latency < 100ms
- Map tile loading
- Memory usage with animations

---

## 📚 Dependencies

### Backend
```xml
<!-- pom.xml additions -->
<dependency>
    <groupId>org.locationtech.jts</groupId>
    <artifactId>jts-core</artifactId>
    <version>1.19.0</version>
</dependency>
```

### Frontend
```json
{
  "dependencies": {
    "leaflet": "^1.9.4",
    "leaflet.heat": "^0.2.0",
    "leaflet.markercluster": "^1.5.3",
    "@types/leaflet": "^1.9.8",
    "@types/leaflet.heat": "^0.2.2",
    "@types/leaflet.markercluster": "^1.5.4"
  }
}
```

---

## 🎯 Success Metrics

### Performance
- ✅ Map loads in < 2 seconds for 100k QSO database
- ✅ WebSocket latency < 100ms
- ✅ Clustering computation < 500ms
- ✅ Smooth animations at 60 FPS

### User Experience
- ✅ Mobile-first responsive design
- ✅ WCAG AA accessibility compliance
- ✅ Offline functionality
- ✅ Real-time updates feel instantaneous

### Contest Operations
- ✅ Multiplier tracking accuracy: 100%
- ✅ Automatic overlay switching
- ✅ Visual needed/worked indication
- ✅ Quick identification of strategic targets

---

## 📞 Support & Maintenance

### Monitoring
- Map API response times
- WebSocket connection stability
- Cache hit/miss ratios
- Clustering performance
- Offline mode usage

### Maintenance Tasks
- DXCC prefix database updates (annual)
- Contest overlay updates (as needed)
- Map tile provider monitoring
- Performance optimization based on metrics

---

**Last Updated:** December 8, 2025
**Document Version:** 1.0.0
**Status:** Ready for Implementation

73 and happy mapping!
