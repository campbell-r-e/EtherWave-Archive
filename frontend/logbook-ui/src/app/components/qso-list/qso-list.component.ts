import { Component, OnInit, OnDestroy } from '@angular/core';

import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log/log.service';
import { WebSocketService } from '../../services/websocket.service';
import { QSO } from '../../models/qso.model';
import { Station } from '../../models/station.model';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-qso-list',
    imports: [],
    templateUrl: './qso-list.component.html',
    styleUrls: ['./qso-list.component.css']
})
export class QsoListComponent implements OnInit, OnDestroy {
  qsos: QSO[] = [];
  allQsos: QSO[] = []; // Store all QSOs for filtering
  stations: Station[] = [];
  loading = true;
  private wsSubscription: Subscription | null = null;
  selectedStationFilter: number | null = null; // null = all stations, number = specific station ID

  constructor(
    private apiService: ApiService,
    private logService: LogService,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadStations();
    this.loadRecentQSOs();
    this.subscribeToUpdates();
  }

  loadStations(): void {
    this.apiService.getStations().subscribe({
      next: (stations) => {
        this.stations = stations;
      },
      error: (err) => console.error('Error loading stations:', err)
    });
  }

  ngOnDestroy(): void {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
  }

  loadRecentQSOs(): void {
    const currentLog = this.logService.getCurrentLog();
    if (!currentLog) {
      console.warn('No log selected');
      this.loading = false;
      return;
    }

    this.loading = true;
    this.apiService.getRecentQSOs(currentLog.id, 50).subscribe({
      next: (qsos) => {
        this.allQsos = qsos;
        this.applyStationFilter();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading QSOs:', err);
        this.loading = false;
      }
    });
  }

  filterByStation(stationId: number | null): void {
    this.selectedStationFilter = stationId;
    this.applyStationFilter();
  }

  applyStationFilter(): void {
    if (this.selectedStationFilter === null) {
      this.qsos = [...this.allQsos];
    } else {
      this.qsos = this.allQsos.filter(qso => qso.stationId === this.selectedStationFilter);
    }
  }

  subscribeToUpdates(): void {
    this.wsSubscription = this.wsService.getQSOUpdates().subscribe({
      next: (newQSO) => {
        // Add new QSO to all QSOs
        this.allQsos.unshift(newQSO);
        // Keep only last 50 in allQsos
        if (this.allQsos.length > 50) {
          this.allQsos.pop();
        }
        // Apply filter to update displayed QSOs
        this.applyStationFilter();
        // Visual feedback
        this.highlightNewQSO(newQSO);
      }
    });
  }

  highlightNewQSO(qso: QSO): void {
    // Add animation class temporarily
    setTimeout(() => {
      const element = document.querySelector(`[data-qso-id="${qso.id}"]`);
      if (element) {
        element.classList.add('new-qso-highlight');
        setTimeout(() => element.classList.remove('new-qso-highlight'), 2000);
      }
    }, 100);
  }

  deleteQSO(qso: QSO): void {
    if (!confirm(`Delete QSO with ${qso.callsign}?`)) {
      return;
    }

    this.apiService.deleteQSO(qso.id!).subscribe({
      next: () => {
        this.qsos = this.qsos.filter(q => q.id !== qso.id);
      },
      error: (err) => {
        console.error('Error deleting QSO:', err);
        alert('Error deleting QSO');
      }
    });
  }

  formatFrequency(khz: number): string {
    if (khz >= 1000) {
      return (khz / 1000).toFixed(3) + ' MHz';
    }
    return khz + ' kHz';
  }

  getValidationClass(qso: QSO): string {
    if (!qso.isValid) return 'table-danger';
    if (qso.validationErrors) return 'table-warning';
    return '';
  }

  /**
   * Get station for a QSO
   */
  getStationForQSO(qso: QSO): Station | undefined {
    return this.stations.find(s => s.id === qso.stationId);
  }

  /**
   * Get station color for left border (5px solid)
   */
  getStationBorder(qso: QSO): string {
    const station = this.getStationForQSO(qso);
    if (!station) return '5px solid #cccccc';

    const color = this.getStationColor(station);
    return `5px solid ${color}`;
  }

  /**
   * Get subtle background color for row
   */
  getRowBackground(qso: QSO): string {
    if (qso.isDuplicate) {
      return '#fff3cd'; // Light yellow for duplicates
    }
    return 'transparent';
  }

  /**
   * Get station color based on ARRL conventions
   */
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

    return '#666666'; // Gray for unassigned
  }
}
