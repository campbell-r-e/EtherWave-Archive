import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, interval, Subscription } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface DXSpot {
  spotter: string;
  dxCallsign: string;
  frequency: number;
  band: string;
  mode: string;
  comment: string;
  time: string;
}

@Injectable({
  providedIn: 'root'
})
export class DXClusterService implements OnDestroy {
  private readonly apiUrl = `${environment.apiUrl}/dx-cluster/spots`;
  private readonly POLL_INTERVAL_MS = 60_000; // 60 seconds

  private spotsSubject = new BehaviorSubject<DXSpot[]>([]);
  public spots$: Observable<DXSpot[]> = this.spotsSubject.asObservable();

  private pollSub: Subscription | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Start polling for DX spots. Safe to call multiple times — stops previous poll first.
   */
  startPolling(limit: number = 50, band: string = ''): void {
    this.stopPolling();

    this.pollSub = interval(this.POLL_INTERVAL_MS).pipe(
      startWith(0),
      switchMap(() => this.fetchSpots(limit, band))
    ).subscribe({
      next: (spots) => this.spotsSubject.next(spots),
      error: (err) => console.error('DX cluster poll error:', err)
    });
  }

  stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }

  /** One-shot fetch — also used by the panel's manual refresh button */
  fetchSpots(limit: number = 50, band: string = ''): Observable<DXSpot[]> {
    let params = new HttpParams().set('limit', limit);
    if (band) {
      params = params.set('band', band);
    }
    return this.http.get<DXSpot[]>(this.apiUrl, { params });
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }
}
