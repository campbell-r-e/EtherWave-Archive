import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LotwSyncRequest {
  lotwCallsign: string;
  lotwPassword: string;
  since?: string; // ISO date YYYY-MM-DD, optional
}

export interface LotwSyncResult {
  downloaded: number;
  matched: number;
  updated: number;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class LotwService {
  private readonly apiUrl = `${environment.apiUrl}/lotw`;

  constructor(private http: HttpClient) {}

  sync(logId: number, request: LotwSyncRequest): Observable<LotwSyncResult> {
    return this.http.post<LotwSyncResult>(`${this.apiUrl}/sync/${logId}`, request);
  }
}
