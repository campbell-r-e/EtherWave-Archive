import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type BandCondition = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR';

export interface BandStatus {
  band: string;
  displayName: string;
  condition: BandCondition;
  description: string;
}

export interface PropagationConditions {
  sfi: number;
  kIndex: number;
  aIndex: number;
  fetchedAt: string;
  bands: { [band: string]: BandStatus };
}

@Injectable({
  providedIn: 'root'
})
export class PropagationService {
  private readonly apiUrl = `${environment.apiUrl}/propagation/conditions`;

  constructor(private http: HttpClient) {}

  getConditions(): Observable<PropagationConditions> {
    return this.http.get<PropagationConditions>(this.apiUrl);
  }
}
