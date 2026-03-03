import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { QSO, QSORequest } from '../models/qso.model';
import { Station, Contest, Operator, CallsignInfo } from '../models/station.model';

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

  // Export endpoints
  // All export methods use HttpClient so the JWT interceptor can add the Authorization header.
  // The response blob is downloaded via a temporary <a> element.

  private triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  private downloadFromUrl(url: string, fallbackFilename: string): void {
    this.http.get(url, { responseType: 'blob', observe: 'response' }).pipe(
      tap(response => {
        const blob = response.body!;
        const disposition = response.headers.get('content-disposition') ?? '';
        const match = disposition.match(/filename="([^"]+)"/);
        const filename = match ? match[1] : fallbackFilename;
        this.triggerDownload(blob, filename);
      })
    ).subscribe({ error: err => console.error('Export failed:', err) });
  }

  /**
   * Export ADIF by log ID (all QSOs)
   */
  exportAdifByLog(logId: number): void {
    this.downloadFromUrl(`${this.baseUrl}/export/adif/log/${logId}`, `log_${logId}.adi`);
  }

  /**
   * Export ADIF combined (all QSOs including GOTA)
   */
  exportAdifCombined(logId: number): void {
    this.downloadFromUrl(`${this.baseUrl}/export/adif/log/${logId}/combined`, `log_${logId}_combined.adi`);
  }

  /**
   * Export ADIF GOTA QSOs only
   */
  exportAdifGota(logId: number): void {
    this.downloadFromUrl(`${this.baseUrl}/export/adif/log/${logId}/gota`, `log_${logId}_gota.adi`);
  }

  /**
   * Export ADIF non-GOTA QSOs only
   */
  exportAdifNonGota(logId: number): void {
    this.downloadFromUrl(`${this.baseUrl}/export/adif/log/${logId}/non-gota`, `log_${logId}_non_gota.adi`);
  }

  /**
   * Export all QSOs as ADIF (legacy method)
   * @deprecated Use exportAdifByLog instead
   */
  exportADIF(): void {
    this.downloadFromUrl(`${this.baseUrl}/export/adif`, 'logbook.adi');
  }

  /**
   * Export Cabrillo by log ID (supports both contest and personal logs)
   */
  exportCabrilloByLog(logId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/log/${logId}?callsign=${encodeURIComponent(callsign)}`;
    if (operators) url += `&operators=${encodeURIComponent(operators)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    this.downloadFromUrl(url, `log_${logId}.log`);
  }

  /**
   * Export Cabrillo combined (all QSOs including GOTA)
   */
  exportCabrilloCombined(logId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/log/${logId}/combined?callsign=${encodeURIComponent(callsign)}`;
    if (operators) url += `&operators=${encodeURIComponent(operators)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    this.downloadFromUrl(url, `log_${logId}_combined.log`);
  }

  /**
   * Export Cabrillo GOTA QSOs only
   */
  exportCabrilloGota(logId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/log/${logId}/gota?callsign=${encodeURIComponent(callsign)}`;
    if (operators) url += `&operators=${encodeURIComponent(operators)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    this.downloadFromUrl(url, `log_${logId}_gota.log`);
  }

  /**
   * Export Cabrillo non-GOTA QSOs only
   */
  exportCabrilloNonGota(logId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/log/${logId}/non-gota?callsign=${encodeURIComponent(callsign)}`;
    if (operators) url += `&operators=${encodeURIComponent(operators)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    this.downloadFromUrl(url, `log_${logId}_non_gota.log`);
  }

  /**
   * Export Cabrillo by contest ID (legacy method)
   * @deprecated Use exportCabrilloByLog instead
   */
  exportCabrillo(contestId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/${contestId}?callsign=${encodeURIComponent(callsign)}`;
    if (operators) url += `&operators=${encodeURIComponent(operators)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    this.downloadFromUrl(url, `cabrillo_${contestId}.log`);
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
