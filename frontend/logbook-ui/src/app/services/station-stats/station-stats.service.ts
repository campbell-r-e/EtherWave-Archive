import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StationStatistics, StationStatsSummary } from '../../models/station-stats.model';

/**
 * Service for multi-station contest statistics
 * Fetches per-station metrics, leaderboards, and summaries
 */
@Injectable({
  providedIn: 'root'
})
export class StationStatsService {
  private baseUrl = '/api/logs';

  constructor(private http: HttpClient) {}

  /**
   * Get statistics for all stations in a log
   */
  getStationStats(logId: number): Observable<StationStatistics[]> {
    return this.http.get<StationStatistics[]>(
      `${this.baseUrl}/${logId}/stats/stations`
    );
  }

  /**
   * Get statistics for a specific station
   */
  getStationStatsForStation(logId: number, stationNumber: number): Observable<StationStatistics> {
    return this.http.get<StationStatistics>(
      `${this.baseUrl}/${logId}/stats/stations/${stationNumber}`
    );
  }

  /**
   * Get statistics for GOTA station
   */
  getGotaStats(logId: number): Observable<StationStatistics> {
    return this.http.get<StationStatistics>(
      `${this.baseUrl}/${logId}/stats/gota`
    );
  }

  /**
   * Get comprehensive summary with main/GOTA separation
   * Used for score summary display and leaderboard
   */
  getStationSummary(logId: number): Observable<StationStatsSummary> {
    return this.http.get<StationStatsSummary>(
      `${this.baseUrl}/${logId}/stats/summary`
    );
  }
}
