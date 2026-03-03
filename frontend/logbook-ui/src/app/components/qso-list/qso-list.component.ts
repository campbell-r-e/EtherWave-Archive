import { Component, OnInit, OnDestroy } from '@angular/core';

import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log/log.service';
import { WebSocketService } from '../../services/websocket.service';
import { QSO } from '../../models/qso.model';
import { Station } from '../../models/station.model';
import { Subscription } from 'rxjs';
import { QslCardComponent } from '../qsl-card/qsl-card.component';

@Component({
    selector: 'app-qso-list',
    imports: [QslCardComponent],
    templateUrl: './qso-list.component.html',
    styleUrls: ['./qso-list.component.css']
})
export class QsoListComponent implements OnInit, OnDestroy {
  qsos: QSO[] = [];
  allQsos: QSO[] = []; // Store all QSOs for filtering
  stations: Station[] = [];
  loading = true;
  private wsSubscription: Subscription | null = null;

  // Tab filtering: 'all' | station number | 'gota'
  activeTab: string = 'all';
  availableStations: number[] = []; // Unique station numbers that have QSOs
  hasGotaQsos = false;

  // QSL card
  selectedQsoForQsl: QSO | null = null;

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
        this.updateAvailableTabs();
        this.applyStationFilter();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading QSOs:', err);
        this.loading = false;
      }
    });
  }

  /**
   * Update available tabs based on QSOs
   */
  updateAvailableTabs(): void {
    // Get unique station numbers
    const stationNumbers = new Set<number>();
    let gotaFound = false;

    this.allQsos.forEach(qso => {
      if (qso.isGota) {
        gotaFound = true;
      } else if (qso.stationNumber) {
        stationNumbers.add(qso.stationNumber);
      }
    });

    this.availableStations = Array.from(stationNumbers).sort((a, b) => a - b);
    this.hasGotaQsos = gotaFound;
  }

  selectTab(tab: string): void {
    this.activeTab = tab;
    this.applyStationFilter();
  }

  /**
   * Handle keyboard navigation for tabs (Left/Right arrows)
   */
  onTabKeydown(event: KeyboardEvent, currentTab: string): void {
    const tabs = this.getAllTabIds();
    const currentIndex = tabs.indexOf(currentTab);

    if (event.key === 'ArrowLeft' && currentIndex > 0) {
      event.preventDefault();
      const previousTab = tabs[currentIndex - 1];
      this.selectTab(previousTab);
      // Focus the previous tab button
      const tabButton = document.querySelector(`[data-tab-id="${previousTab}"]`) as HTMLElement;
      if (tabButton) tabButton.focus();
    } else if (event.key === 'ArrowRight' && currentIndex < tabs.length - 1) {
      event.preventDefault();
      const nextTab = tabs[currentIndex + 1];
      this.selectTab(nextTab);
      // Focus the next tab button
      const tabButton = document.querySelector(`[data-tab-id="${nextTab}"]`) as HTMLElement;
      if (tabButton) tabButton.focus();
    }
  }

  /**
   * Get all available tab IDs in order
   */
  getAllTabIds(): string[] {
    const tabs = ['all'];
    tabs.push(...this.availableStations.map(s => s.toString()));
    if (this.hasGotaQsos) {
      tabs.push('gota');
    }
    return tabs;
  }

  applyStationFilter(): void {
    if (this.activeTab === 'all') {
      this.qsos = [...this.allQsos];
    } else if (this.activeTab === 'gota') {
      this.qsos = this.allQsos.filter(qso => qso.isGota);
    } else {
      // Filter by station number
      const stationNum = parseInt(this.activeTab);
      this.qsos = this.allQsos.filter(qso => qso.stationNumber === stationNum && !qso.isGota);
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

  openQslCard(qso: QSO): void {
    this.selectedQsoForQsl = qso;
  }

  closeQslCard(): void {
    this.selectedQsoForQsl = null;
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
    const color = this.getStationColorForQSO(qso);
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
   * Get station color based on stationNumber or GOTA
   */
  getStationColorForQSO(qso: Partial<QSO> | QSO): string {
    if (qso.isGota) {
      return '#43A047'; // Green (GEKHoosier theme)
    }

    if (qso.stationNumber) {
      const colors: { [key: number]: string } = {
        1: '#1E88E5',  // Blue
        2: '#E53935',  // Red
        3: '#FB8C00',  // Orange
        4: '#8E24AA',  // Purple
        5: '#00ACC1',  // Cyan
        6: '#FDD835',  // Yellow
      };
      return colors[qso.stationNumber] || '#9E9E9E';
    }

    return '#9E9E9E'; // Gray for unassigned
  }

  /**
   * Get station label for display
   */
  getStationLabel(qso: Partial<QSO> | QSO): string {
    if (qso.isGota) {
      return 'GOTA';
    }
    if (qso.stationNumber) {
      return `Station ${qso.stationNumber}`;
    }
    return 'Unassigned';
  }

  /**
   * Get station color for legacy station-based calls
   */
  getStationColor(station: Station): string {
    if (station.isGota) {
      return '#43A047'; // Green
    }

    if (station.stationNumber) {
      const colors: { [key: number]: string } = {
        1: '#1E88E5',  // Blue
        2: '#E53935',  // Red
        3: '#FB8C00',  // Orange
        4: '#8E24AA',  // Purple
        5: '#00ACC1',  // Cyan
        6: '#FDD835',  // Yellow
      };
      return colors[station.stationNumber] || '#9E9E9E';
    }

    return '#9E9E9E'; // Gray for unassigned
  }
}
