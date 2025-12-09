import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Service for interacting with the map API endpoints
 * Handles QSO locations, clustering, grids, heatmaps, and overlays
 */
@Injectable({
  providedIn: 'root'
})
export class MapService {
  private readonly API_URL = `${environment.apiUrl}/api/maps`;
  private readonly DXCC_URL = `${environment.apiUrl}/api/dxcc`;

  constructor(private http: HttpClient) {}

  /**
   * Get QSO locations with adaptive clustering
   * @param logId Log ID
   * @param zoom Map zoom level (0-18)
   * @param filters Optional filters
   */
  getQSOLocations(logId: number, zoom: number, filters?: MapFilters): Observable<MapDataResponse> {
    let params = new HttpParams().set('zoom', zoom.toString());

    if (filters) {
      if (filters.band) params = params.set('band', filters.band);
      if (filters.mode) params = params.set('mode', filters.mode);
      if (filters.station) params = params.set('station', filters.station.toString());
      if (filters.operator) params = params.set('operator', filters.operator);
      if (filters.dxcc) params = params.set('dxcc', filters.dxcc);
      if (filters.dateFrom) params = params.set('dateFrom', filters.dateFrom);
      if (filters.dateTo) params = params.set('dateTo', filters.dateTo);
      if (filters.confirmed !== undefined) params = params.set('confirmed', filters.confirmed.toString());
      if (filters.continent) params = params.set('continent', filters.continent);
      if (filters.state) params = params.set('state', filters.state);
      if (filters.exchange) params = params.set('exchange', filters.exchange);
    }

    return this.http.get<MapDataResponse>(`${this.API_URL}/qsos/${logId}`, { params });
  }

  /**
   * Get grid coverage data
   * @param logId Log ID
   * @param precision Grid precision (2, 4, 6, or 8 characters)
   */
  getGridCoverage(logId: number, precision?: number): Observable<GridCoverageResponse> {
    let params = new HttpParams();
    if (precision) params = params.set('precision', precision.toString());

    return this.http.get<GridCoverageResponse>(`${this.API_URL}/grids/${logId}`, { params });
  }

  /**
   * Get heatmap density data
   * @param logId Log ID
   * @param gridBased Use grid-based heatmap
   * @param filters Optional filters
   */
  getHeatmapData(
    logId: number,
    gridBased: boolean = false,
    filters?: MapFilters
  ): Observable<HeatmapResponse> {
    let params = new HttpParams().set('gridBased', gridBased.toString());

    if (filters) {
      if (filters.band) params = params.set('band', filters.band);
      if (filters.mode) params = params.set('mode', filters.mode);
      // Add other filters as needed
    }

    return this.http.get<HeatmapResponse>(`${this.API_URL}/heatmap/${logId}`, { params });
  }

  /**
   * Get contest overlay data
   * @param logId Log ID
   * @param type Overlay type (CQ_ZONES, ITU_ZONES, ARRL_SECTIONS, DXCC)
   */
  getContestOverlay(logId: number, type: OverlayType): Observable<any> {
    const params = new HttpParams().set('type', type);
    return this.http.get(`${this.API_URL}/contest-overlays/${logId}`, { params });
  }

  /**
   * Get multiplier summary for a contest
   * @param logId Log ID
   * @param contestType Contest type
   */
  getMultipliers(logId: number, contestType: ContestType): Observable<MultiplierSummary> {
    const params = new HttpParams().set('contest', contestType);
    return this.http.get<MultiplierSummary>(`${this.API_URL}/multipliers/${logId}`, { params });
  }

  /**
   * Export map data
   * @param logId Log ID
   * @param format Export format (GEOJSON, KML, CSV, ADIF)
   * @param filters Optional filters
   */
  exportMapData(logId: number, format: ExportFormat, filters?: MapFilters): Observable<ExportResult> {
    const body = { format, filters };
    return this.http.post<ExportResult>(`${this.API_URL}/export/${logId}`, body);
  }

  /**
   * Set station location
   * @param stationId Station ID
   * @param location Location data
   */
  setStationLocation(stationId: number, location: LocationRequest): Observable<LocationUpdateResponse> {
    return this.http.put<LocationUpdateResponse>(`${this.API_URL}/location/station/${stationId}`, location);
  }

  /**
   * Set session location (temporary override)
   * @param logId Log ID
   * @param location Location data
   */
  setSessionLocation(logId: number, location: LocationRequest): Observable<SessionLocationResponse> {
    return this.http.post<SessionLocationResponse>(`${this.API_URL}/location/session/${logId}`, location);
  }

  /**
   * Get session location
   * @param logId Log ID
   */
  getSessionLocation(logId: number): Observable<SessionLocation> {
    return this.http.get<SessionLocation>(`${this.API_URL}/location/session/${logId}`);
  }

  /**
   * Clear session location
   * @param logId Log ID
   */
  clearSessionLocation(logId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/location/session/${logId}`);
  }

  /**
   * Look up DXCC information for a callsign
   * @param callsign Callsign to lookup
   */
  lookupDXCC(callsign: string): Observable<DXCCInfo> {
    return this.http.get<DXCCInfo>(`${this.DXCC_URL}/lookup/${callsign}`);
  }

  /**
   * Get location for callsign
   * @param callsign Callsign to lookup
   */
  getLocationForCallsign(callsign: string): Observable<LocationInfo> {
    return this.http.get<LocationInfo>(`${this.DXCC_URL}/location/${callsign}`);
  }
}

// ===== Interfaces =====

export interface MapFilters {
  band?: string;
  mode?: string;
  station?: number;
  operator?: string;
  dxcc?: string;
  dateFrom?: string;
  dateTo?: string;
  confirmed?: boolean;
  continent?: string;
  state?: string;
  exchange?: string;
}

export interface MapDataResponse {
  clustered: boolean;
  clusterThreshold: number;
  totalQSOs: number;
  displayedPoints: number;
  markers: MapMarker[];
  clusters: MapCluster[];
}

export interface MapMarker {
  lat: number;
  lon: number;
  qsoId?: number;
  callsign: string;
  grid?: string;
  distance?: number;
  bearing?: number;
  station?: number;
  band?: string;
  mode?: string;
  timestamp?: string;
}

export interface MapCluster {
  lat: number;
  lon: number;
  count: number;
  bounds: {
    minLat: number;
    maxLat: number;
    minLon: number;
    maxLon: number;
  };
  stationBreakdown?: { [key: number]: number };
  bandBreakdown?: { [key: string]: number };
  modeBreakdown?: { [key: string]: number };
}

export interface GridCoverageResponse {
  precision: number;
  totalGrids: number;
  workedGrids: number;
  grids: GridSquare[];
  neighbors: GridSquare[];
}

export interface GridSquare {
  grid: string;
  qsoCount: number;
  bands: number;
  modes: number;
  firstQSO: string;
  lastQSO: string;
  centerLat: number;
  centerLon: number;
}

export interface HeatmapResponse {
  gridBased: boolean;
  maxIntensity: number;
  points: HeatmapPoint[];
}

export interface HeatmapPoint {
  lat: number;
  lon: number;
  intensity: number;
  count: number;
}

export interface MultiplierSummary {
  contestType: string;
  multiplierType: string;
  totalMultipliers: number;
  workedMultipliers: number;
  neededMultipliers: number;
  percentageComplete: number;
}

export interface ExportResult {
  format: string;
  content: string;
  filename: string;
  mimeType: string;
  success: boolean;
  error?: string;
}

export interface LocationRequest {
  latitude: number;
  longitude: number;
  grid?: string;
  locationName?: string;
}

export interface LocationUpdateResponse {
  success: boolean;
  latitude: number;
  longitude: number;
  grid: string;
  locationName?: string;
  source: string;
  message: string;
}

export interface SessionLocationResponse {
  success: boolean;
  logId: number;
  latitude: number;
  longitude: number;
  grid: string;
  message: string;
}

export interface SessionLocation {
  logId: number;
  latitude: number;
  longitude: number;
  grid: string;
  timestamp: number;
}

export interface DXCCInfo {
  dxccCode: number;
  entityName: string;
  continent: string;
  cqZone: number;
  ituZone: number;
  latitude: number;
  longitude: number;
  primaryPrefix: string;
}

export interface LocationInfo {
  latitude: number;
  longitude: number;
  entityName: string;
  continent: string;
  dxccCode: number;
}

export type OverlayType = 'CQ_ZONES' | 'ITU_ZONES' | 'ARRL_SECTIONS' | 'DXCC';

export type ContestType =
  | 'ARRL_FIELD_DAY'
  | 'ARRL_SWEEPSTAKES'
  | 'CQ_WW_DX'
  | 'CQ_WPX'
  | 'IARU_HF'
  | 'DXCC_CHALLENGE';

export type ExportFormat = 'GEOJSON' | 'KML' | 'CSV' | 'ADIF';
