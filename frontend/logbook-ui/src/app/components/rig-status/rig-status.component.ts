import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { RigStatus } from '../../models/station.model';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Component({
    selector: 'app-rig-status',
    imports: [CommonModule],
    templateUrl: './rig-status.component.html',
    styleUrls: ['./rig-status.component.css']
})
export class RigStatusComponent implements OnInit, OnDestroy {
  rigStatus: RigStatus | null = null;
  rigServiceUrl = 'http://localhost:8081';
  pollingEnabled = false;
  private pollingSubscription: Subscription | null = null;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.checkRigConnection();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  checkRigConnection(): void {
    this.apiService.getRigStatus(this.rigServiceUrl).subscribe({
      next: (status) => {
        this.rigStatus = status;
        if (status.connected && !this.pollingEnabled) {
          this.startPolling();
        }
      },
      error: () => {
        this.rigStatus = {
          connected: false,
          error: 'Cannot connect to rig control service'
        };
      }
    });
  }

  startPolling(): void {
    this.pollingEnabled = true;
    this.pollingSubscription = interval(2000)
      .pipe(switchMap(() => this.apiService.getRigStatus(this.rigServiceUrl)))
      .subscribe({
        next: (status) => {
          this.rigStatus = status;
        },
        error: () => {
          this.rigStatus = {
            connected: false,
            error: 'Lost connection to rig'
          };
          this.stopPolling();
        }
      });
  }

  stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = null;
    }
    this.pollingEnabled = false;
  }

  formatFrequency(hz: number | undefined): string {
    if (!hz) return '---';
    const mhz = hz / 1000000;
    return mhz.toFixed(4) + ' MHz';
  }

  getConnectionStatusClass(): string {
    if (!this.rigStatus) return 'bg-secondary';
    return this.rigStatus.connected ? 'bg-success' : 'bg-danger';
  }

  getPTTClass(): string {
    if (!this.rigStatus?.pttActive) return 'text-muted';
    return 'text-danger blink';
  }
}
