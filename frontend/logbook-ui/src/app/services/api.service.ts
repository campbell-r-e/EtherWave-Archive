import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { QSO, QSORequest } from '../models/qso.model';
import { Station, Contest, Operator, CallsignInfo, RigStatus } from '../models/station.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = '/api';

  constructor(private http: HttpClient) {}

  // QSO endpoints
  getQSOs(logId: number, page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('logId', logId.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/qsos`, { params });
  }

  getRecentQSOs(logId: number, limit: number = 10): Observable<QSO[]> {
    const params = new HttpParams()
      .set('logId', logId.toString())
      .set('limit', limit.toString());
    return this.http.get<QSO[]>(`${this.baseUrl}/qsos/recent`, { params });
  }

  getQSO(id: number): Observable<QSO> {
    return this.http.get<QSO>(`${this.baseUrl}/qsos/${id}`);
  }

  createQSO(qso: QSORequest, logId: number): Observable<QSO> {
    const params = new HttpParams().set('logId', logId.toString());
    return this.http.post<QSO>(`${this.baseUrl}/qsos`, qso, { params });
  }

  updateQSO(id: number, qso: QSORequest): Observable<QSO> {
    return this.http.put<QSO>(`${this.baseUrl}/qsos/${id}`, qso);
  }

  deleteQSO(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/qsos/${id}`);
  }

  getContactedStates(logId: number): Observable<string[]> {
    const params = new HttpParams().set('logId', logId.toString());
    return this.http.get<string[]>(`${this.baseUrl}/qsos/states`, { params });
  }

  getStateStatistics(): Observable<{state: string, count: number}[]> {
    return this.http.get<{state: string, count: number}[]>(`${this.baseUrl}/qsos/states/statistics`);
  }

  // Station endpoints
  getStations(): Observable<Station[]> {
    return this.http.get<Station[]>(`${this.baseUrl}/stations`);
  }

  getStation(id: number): Observable<Station> {
    return this.http.get<Station>(`${this.baseUrl}/stations/${id}`);
  }

  createStation(station: Station): Observable<Station> {
    return this.http.post<Station>(`${this.baseUrl}/stations`, station);
  }

  updateStation(id: number, station: Station): Observable<Station> {
    return this.http.put<Station>(`${this.baseUrl}/stations/${id}`, station);
  }

  deleteStation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/stations/${id}`);
  }

  // Contest endpoints
  getContests(): Observable<Contest[]> {
    return this.http.get<Contest[]>(`${this.baseUrl}/contests`);
  }

  getActiveContests(): Observable<Contest[]> {
    return this.http.get<Contest[]>(`${this.baseUrl}/contests/active`);
  }

  getContest(id: number): Observable<Contest> {
    return this.http.get<Contest>(`${this.baseUrl}/contests/${id}`);
  }

  // Operator endpoints
  getOperators(): Observable<Operator[]> {
    return this.http.get<Operator[]>(`${this.baseUrl}/operators`);
  }

  createOperator(operator: Operator): Observable<Operator> {
    return this.http.post<Operator>(`${this.baseUrl}/operators`, operator);
  }

  // Callsign lookup
  lookupCallsign(callsign: string): Observable<CallsignInfo> {
    return this.http.get<CallsignInfo>(`${this.baseUrl}/callsigns/${callsign}`);
  }

  // Rig control (if local rig service available)
  getRigStatus(rigServiceUrl: string = 'http://localhost:8081'): Observable<RigStatus> {
    return this.http.get<RigStatus>(`${rigServiceUrl}/api/rig/status`);
  }

  setRigFrequency(hz: number, rigServiceUrl: string = 'http://localhost:8081'): Observable<string> {
    const params = new HttpParams().set('hz', hz.toString());
    return this.http.post(`${rigServiceUrl}/api/rig/frequency`, null, { params, responseType: 'text' });
  }

  setRigMode(mode: string, bandwidth: number = 0, rigServiceUrl: string = 'http://localhost:8081'): Observable<string> {
    const params = new HttpParams()
      .set('mode', mode)
      .set('bandwidth', bandwidth.toString());
    return this.http.post(`${rigServiceUrl}/api/rig/mode`, null, { params, responseType: 'text' });
  }

  // Export endpoints
  /**
   * Export ADIF by log ID (all QSOs)
   */
  exportAdifByLog(logId: number): void {
    window.open(`${this.baseUrl}/export/adif/log/${logId}`, '_blank');
  }

  /**
   * Export ADIF combined (all QSOs including GOTA)
   */
  exportAdifCombined(logId: number): void {
    window.open(`${this.baseUrl}/export/adif/log/${logId}/combined`, '_blank');
  }

  /**
   * Export ADIF GOTA QSOs only
   */
  exportAdifGota(logId: number): void {
    window.open(`${this.baseUrl}/export/adif/log/${logId}/gota`, '_blank');
  }

  /**
   * Export ADIF non-GOTA QSOs only
   */
  exportAdifNonGota(logId: number): void {
    window.open(`${this.baseUrl}/export/adif/log/${logId}/non-gota`, '_blank');
  }

  /**
   * Export all QSOs as ADIF (legacy method)
   * @deprecated Use exportAdifByLog instead
   */
  exportADIF(): void {
    window.open(`${this.baseUrl}/export/adif`, '_blank');
  }

  /**
   * Export Cabrillo by log ID (supports both contest and personal logs)
   */
  exportCabrilloByLog(logId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/log/${logId}?callsign=${callsign}`;
    if (operators) url += `&operators=${operators}`;
    if (category) url += `&category=${category}`;
    window.open(url, '_blank');
  }

  /**
   * Export Cabrillo combined (all QSOs including GOTA)
   */
  exportCabrilloCombined(logId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/log/${logId}/combined?callsign=${callsign}`;
    if (operators) url += `&operators=${operators}`;
    if (category) url += `&category=${category}`;
    window.open(url, '_blank');
  }

  /**
   * Export Cabrillo GOTA QSOs only
   */
  exportCabrilloGota(logId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/log/${logId}/gota?callsign=${callsign}`;
    if (operators) url += `&operators=${operators}`;
    if (category) url += `&category=${category}`;
    window.open(url, '_blank');
  }

  /**
   * Export Cabrillo non-GOTA QSOs only
   */
  exportCabrilloNonGota(logId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/log/${logId}/non-gota?callsign=${callsign}`;
    if (operators) url += `&operators=${operators}`;
    if (category) url += `&category=${category}`;
    window.open(url, '_blank');
  }

  /**
   * Export Cabrillo by contest ID (legacy method)
   * @deprecated Use exportCabrilloByLog instead
   */
  exportCabrillo(contestId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/${contestId}?callsign=${callsign}`;
    if (operators) url += `&operators=${operators}`;
    if (category) url += `&category=${category}`;
    window.open(url, '_blank');
  }

  // Log participant endpoints
  /**
   * Get current user's station assignment for a log
   */
  getMyStationAssignment(logId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/logs/${logId}/my-assignment`);
  }

  // Import endpoints
  importAdif(file: File, logId: number, stationId: number): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    const params = new HttpParams().set('stationId', stationId.toString());
    return this.http.post<any>(`${this.baseUrl}/import/adif/${logId}`, formData, { params });
  }

  /**
   * Preview ADIF file to extract unique station callsigns
   */
  previewAdifFile(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.baseUrl}/import/adif/preview`, formData);
  }

  /**
   * Import ADIF file with station mapping
   * @param stationMapping Map of imported callsign to local station ID
   */
  importAdifWithMapping(file: File, logId: number, fallbackStationId: number, stationMapping: { [key: string]: number }): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);

    // Add fallback station ID
    formData.append('fallbackStationId', fallbackStationId.toString());

    // Add station mappings as individual form parameters
    for (const [callsign, stationId] of Object.entries(stationMapping)) {
      formData.append(callsign, stationId.toString());
    }

    return this.http.post<any>(`${this.baseUrl}/import/adif/${logId}/with-mapping`, formData);
  }
}
