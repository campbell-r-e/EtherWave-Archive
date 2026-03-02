import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DXCCProgress {
  workedCountries: string[];
  confirmedCountries: string[];
  workedCount: number;
  confirmedCount: number;
  totalEntities: number;
}

export interface WASProgress {
  workedStates: string[];
  confirmedStates: string[];
  workedCount: number;
  confirmedCount: number;
  totalStates: number;
}

export interface VUCCProgress {
  workedGrids: string[];
  confirmedGrids: string[];
  workedCount: number;
  confirmedCount: number;
  threshold: number;
}

export interface AwardProgress {
  logId: number;
  logName: string;
  totalQsos: number;
  dxcc: DXCCProgress;
  was: WASProgress;
  vucc: VUCCProgress;
}

@Injectable({
  providedIn: 'root'
})
export class AwardService {
  private readonly apiUrl = `${environment.apiUrl}/awards`;

  constructor(private http: HttpClient) {}

  getProgress(logId: number): Observable<AwardProgress> {
    return this.http.get<AwardProgress>(`${this.apiUrl}/${logId}`);
  }
}
