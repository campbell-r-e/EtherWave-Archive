import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';

export interface RigCommandRequest {
  command: string;
  params: any;
}

export interface RigCommandResponse {
  success: boolean;
  message: string;
  result?: any;
}

export interface RigConnectionRequest {
  stationId: number;
  host?: string;
  port?: number;
}

export interface RigStatus {
  frequency?: number;
  mode?: string;
  ptt?: boolean;
  sMeter?: number;
  connected?: boolean;
}

export interface RigEvent {
  timestamp: string;
  eventType: string;
  clientId: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class RigControlService {
  private baseUrl = '/api/rig-control';

  // Subjects for real-time updates
  private statusUpdates = new Subject<{ stationId: number, status: RigStatus }>();
  private eventUpdates = new Subject<{ stationId: number, event: RigEvent }>();

  constructor(private http: HttpClient) {}

  /**
   * Connect to rig control service
   */
  connect(request: RigConnectionRequest): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/connect`, request);
  }

  /**
   * Disconnect from rig control service
   */
  disconnect(stationId: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/disconnect/${stationId}`, {});
  }

  /**
   * Send a generic command to the rig
   */
  sendCommand(stationId: number, request: RigCommandRequest): Observable<RigCommandResponse> {
    return this.http.post<RigCommandResponse>(`${this.baseUrl}/command/${stationId}`, request);
  }

  /**
   * Set frequency
   */
  setFrequency(stationId: number, frequencyHz: number): Observable<RigCommandResponse> {
    const params = new HttpParams().set('frequencyHz', frequencyHz.toString());
    return this.http.post<RigCommandResponse>(`${this.baseUrl}/frequency/${stationId}`, {}, { params });
  }

  /**
   * Set mode
   */
  setMode(stationId: number, mode: string, bandwidth: number = 0): Observable<RigCommandResponse> {
    const params = new HttpParams()
      .set('mode', mode)
      .set('bandwidth', bandwidth.toString());
    return this.http.post<RigCommandResponse>(`${this.baseUrl}/mode/${stationId}`, {}, { params });
  }

  /**
   * Set PTT (Push-to-Talk)
   */
  setPTT(stationId: number, enable: boolean): Observable<RigCommandResponse> {
    const params = new HttpParams().set('enable', enable.toString());
    return this.http.post<RigCommandResponse>(`${this.baseUrl}/ptt/${stationId}`, {}, { params });
  }

  /**
   * Get current status
   */
  getStatus(stationId: number): Observable<RigCommandResponse> {
    return this.http.get<RigCommandResponse>(`${this.baseUrl}/status/${stationId}`);
  }

  /**
   * Check if connected
   */
  isConnected(stationId: number): Observable<{ connected: boolean, stationId: number }> {
    return this.http.get<{ connected: boolean, stationId: number }>(`${this.baseUrl}/connected/${stationId}`);
  }

  /**
   * Subscribe to status updates
   */
  onStatusUpdate(): Observable<{ stationId: number, status: RigStatus }> {
    return this.statusUpdates.asObservable();
  }

  /**
   * Subscribe to event updates
   */
  onEventUpdate(): Observable<{ stationId: number, event: RigEvent }> {
    return this.eventUpdates.asObservable();
  }

  /**
   * Handle status update from WebSocket
   * (Called by WebSocketService)
   */
  handleStatusUpdate(stationId: number, status: RigStatus) {
    this.statusUpdates.next({ stationId, status });
  }

  /**
   * Handle event from WebSocket
   * (Called by WebSocketService)
   */
  handleEvent(stationId: number, event: RigEvent) {
    this.eventUpdates.next({ stationId, event });
  }
}
