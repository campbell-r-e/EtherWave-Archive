import { Component, OnInit, OnDestroy } from '@angular/core';

import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log/log.service';
import { WebSocketService } from '../../services/websocket.service';
import { QSO } from '../../models/qso.model';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-qso-list',
    imports: [],
    templateUrl: './qso-list.component.html',
    styleUrls: ['./qso-list.component.css']
})
export class QsoListComponent implements OnInit, OnDestroy {
  qsos: QSO[] = [];
  loading = true;
  private wsSubscription: Subscription | null = null;

  constructor(
    private apiService: ApiService,
    private logService: LogService,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadRecentQSOs();
    this.subscribeToUpdates();
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
        this.qsos = qsos;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading QSOs:', err);
        this.loading = false;
      }
    });
  }

  subscribeToUpdates(): void {
    this.wsSubscription = this.wsService.getQSOUpdates().subscribe({
      next: (newQSO) => {
        // Add new QSO to top of list
        this.qsos.unshift(newQSO);
        // Keep only last 50
        if (this.qsos.length > 50) {
          this.qsos.pop();
        }
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
}
