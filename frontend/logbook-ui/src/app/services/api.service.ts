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
  getQSOs(page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/qsos`, { params });
  }

  getRecentQSOs(limit: number = 10): Observable<QSO[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<QSO[]>(`${this.baseUrl}/qsos/recent`, { params });
  }

  getQSO(id: number): Observable<QSO> {
    return this.http.get<QSO>(`${this.baseUrl}/qsos/${id}`);
  }

  createQSO(qso: QSORequest): Observable<QSO> {
    return this.http.post<QSO>(`${this.baseUrl}/qsos`, qso);
  }

  updateQSO(id: number, qso: QSORequest): Observable<QSO> {
    return this.http.put<QSO>(`${this.baseUrl}/qsos/${id}`, qso);
  }

  deleteQSO(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/qsos/${id}`);
  }

  getContactedStates(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/qsos/states`);
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
  exportADIF(): void {
    window.open(`${this.baseUrl}/export/adif`, '_blank');
  }

  exportCabrillo(contestId: number, callsign: string, operators?: string, category?: string): void {
    let url = `${this.baseUrl}/export/cabrillo/${contestId}?callsign=${callsign}`;
    if (operators) url += `&operators=${operators}`;
    if (category) url += `&category=${category}`;
    window.open(url, '_blank');
  }
}
