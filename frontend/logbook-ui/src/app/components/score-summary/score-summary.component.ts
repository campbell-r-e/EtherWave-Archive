import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log/log.service';
import { Log } from '../../models/log.model';
import { Station } from '../../models/station.model';
import { QSO } from '../../models/qso.model';

interface StationScore {
  station: Station;
  qsoCount: number;
  points: number;
  isGota: boolean;
  color: string;
}

@Component({
  selector: 'app-score-summary',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './score-summary.component.html',
  styleUrls: ['./score-summary.component.css']
})
export class ScoreSummaryComponent implements OnInit {
  currentLog: Log | null = null;
  stations: Station[] = [];
  stationScores: StationScore[] = [];
  loading = true;

  constructor(
    private apiService: ApiService,
    private logService: LogService
  ) {}

  ngOnInit(): void {
    this.logService.currentLog$.subscribe({
      next: (log) => {
        this.currentLog = log;
        if (log) {
          this.loadStations();
        }
      }
    });
  }

  loadStations(): void {
    this.loading = true;
    this.apiService.getStations().subscribe({
      next: (stations) => {
        this.stations = stations;
        this.calculateStationScores();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading stations:', err);
        this.loading = false;
      }
    });
  }

  calculateStationScores(): void {
    if (!this.currentLog) return;

    // Get QSOs for current log
    this.apiService.getRecentQSOs(this.currentLog.id, 1000).subscribe({
      next: (qsos) => {
        this.stationScores = this.stations.map(station => {
          const stationQsos = qsos.filter(q => q.stationId === station.id);
          const points = stationQsos
            .filter(q => !q.isDuplicate)
            .reduce((sum, q) => sum + (q.points || 0), 0);

          return {
            station: station,
            qsoCount: stationQsos.length,
            points: points,
            isGota: station.isGota || false,
            color: this.getStationColor(station)
          };
        }).filter(s => s.qsoCount > 0); // Only show stations with QSOs
      },
      error: (err) => console.error('Error loading QSOs:', err)
    });
  }

  getStationColor(station: Station): string {
    if (station.isGota) {
      return '#00AA00'; // Green
    }

    if (station.stationNumber) {
      const colors: { [key: number]: string } = {
        1: '#0066CC',  // Blue
        2: '#CC0000',  // Red
        3: '#FF6600',  // Orange
        4: '#9900CC',  // Purple
        5: '#00CCCC',  // Cyan
        6: '#CCCC00',  // Yellow
      };
      return colors[station.stationNumber] || '#666666';
    }

    return '#666666'; // Gray
  }

  getGotaStations(): StationScore[] {
    return this.stationScores.filter(s => s.isGota);
  }

  getNonGotaStations(): StationScore[] {
    return this.stationScores.filter(s => !s.isGota);
  }

  getTotalGotaPoints(): number {
    return this.getGotaStations().reduce((sum, s) => sum + s.points, 0);
  }

  getTotalGotaQsos(): number {
    return this.getGotaStations().reduce((sum, s) => sum + s.qsoCount, 0);
  }

  hasGotaStations(): boolean {
    return this.getGotaStations().length > 0;
  }

  getScorePercentage(points: number): number {
    if (!this.currentLog?.totalPoints) return 0;
    return Math.round((points / this.currentLog.totalPoints) * 100);
  }
}
