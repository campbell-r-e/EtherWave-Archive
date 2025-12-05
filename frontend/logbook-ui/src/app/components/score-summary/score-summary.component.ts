import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LogService } from '../../services/log/log.service';
import { StationStatsService } from '../../services/station-stats/station-stats.service';
import { Log } from '../../models/log.model';
import { StationStatsSummary, StationStatistics } from '../../models/station-stats.model';

@Component({
  selector: 'app-score-summary',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './score-summary.component.html',
  styleUrls: ['./score-summary.component.css']
})
export class ScoreSummaryComponent implements OnInit {
  currentLog: Log | null = null;
  summary: StationStatsSummary | null = null;
  loading = true;

  constructor(
    private logService: LogService,
    private stationStatsService: StationStatsService
  ) {}

  ngOnInit(): void {
    this.logService.currentLog$.subscribe({
      next: (log) => {
        this.currentLog = log;
        if (log) {
          this.loadStationSummary();
        }
      }
    });
  }

  loadStationSummary(): void {
    if (!this.currentLog) return;

    this.loading = true;
    this.stationStatsService.getStationSummary(this.currentLog.id).subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading station summary:', err);
        this.loading = false;
      }
    });
  }

  getStationColor(stationNumber?: number, isGota?: boolean): string {
    if (isGota) {
      return '#43A047'; // Green (GEKHoosier theme)
    }

    if (stationNumber) {
      const colors: { [key: number]: string } = {
        1: '#1E88E5',  // Blue
        2: '#E53935',  // Red
        3: '#FB8C00',  // Orange
        4: '#8E24AA',  // Purple
        5: '#00ACC1',  // Cyan
        6: '#FDD835',  // Yellow
      };
      return colors[stationNumber] || '#9E9E9E';
    }

    return '#9E9E9E'; // Gray
  }

  getMedalIcon(rank?: number): string {
    if (!rank) return '';
    switch (rank) {
      case 1: return '🥇';
      case 2: return '🥈';
      case 3: return '🥉';
      default: return '';
    }
  }

  getScorePercentage(points: number, totalPoints: number): number {
    if (!totalPoints) return 0;
    return Math.round((points / totalPoints) * 100);
  }

  hasMultipleStations(): boolean {
    return (this.summary?.mainStations?.length || 0) > 1;
  }

  hasGotaStation(): boolean {
    return !!this.summary?.gota;
  }
}
