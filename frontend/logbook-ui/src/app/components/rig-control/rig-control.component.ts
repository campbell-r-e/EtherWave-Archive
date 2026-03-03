import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { RigControlService, RigStatus, RigEvent } from '../../services/rig-control.service';

@Component({
  selector: 'app-rig-control',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rig-control.component.html',
  styleUrls: ['./rig-control.component.css']
})
export class RigControlComponent implements OnInit, OnDestroy {
  @Input() stationId!: number;
  @Input() stationName!: string;
  @Input() host?: string;
  @Input() port?: number;

  connected = false;
  connecting = false;
  currentStatus: RigStatus = {};
  recentEvents: RigEvent[] = [];
  errorMessage: string | null = null;

  // Input fields
  frequencyMhz = '14.250';
  selectedMode = 'USB';
  pttActive = false;

  private statusSubscription?: Subscription;
  private eventsSubscription?: Subscription;

  modes = ['USB', 'LSB', 'CW', 'FM', 'AM', 'RTTY', 'PSK', 'FT8', 'FT4'];

  constructor(private rigControlService: RigControlService) {}

  ngOnInit() {
    // Subscribe to status updates
    this.statusSubscription = this.rigControlService.onStatusUpdate().subscribe(update => {
      if (update.stationId === this.stationId) {
        this.currentStatus = update.status;
        if (update.status.frequencyHz) {
          this.frequencyMhz = (update.status.frequencyHz / 1000000).toFixed(3);
        }
        if (update.status.mode) {
          this.selectedMode = update.status.mode;
        }
        if (update.status.pttActive !== undefined) {
          this.pttActive = update.status.pttActive;
        }
      }
    });

    // Subscribe to events
    this.eventsSubscription = this.rigControlService.onEventUpdate().subscribe(update => {
      if (update.stationId === this.stationId) {
        this.recentEvents.unshift(update.event);
        if (this.recentEvents.length > 10) {
          this.recentEvents.pop();
        }
      }
    });

    // Check if already connected
    this.checkConnection();
  }

  ngOnDestroy() {
    this.statusSubscription?.unsubscribe();
    this.eventsSubscription?.unsubscribe();
  }

  checkConnection() {
    this.rigControlService.isConnected(this.stationId).subscribe({
      next: (response) => {
        this.connected = response.connected;
      },
      error: (error) => {
        console.error('Error checking connection:', error);
      }
    });
  }

  connect() {
    this.connecting = true;
    this.errorMessage = null;

    this.rigControlService.connect({
      stationId: this.stationId,
      host: this.host,
      port: this.port
    }).subscribe({
      next: (response) => {
        this.connected = response.success;
        this.connecting = false;
        if (!response.success) {
          this.errorMessage = response.message;
        }
      },
      error: (error) => {
        this.connecting = false;
        this.connected = false;
        this.errorMessage = error.error?.message || 'Failed to connect to rig control service';
        console.error('Connection error:', error);
      }
    });
  }

  disconnect() {
    this.rigControlService.disconnect(this.stationId).subscribe({
      next: () => {
        this.connected = false;
        this.currentStatus = {};
        this.recentEvents = [];
      },
      error: (error) => {
        console.error('Disconnect error:', error);
      }
    });
  }

  setFrequency() {
    const freqHz = parseFloat(this.frequencyMhz) * 1000000;
    this.rigControlService.setFrequency(this.stationId, freqHz).subscribe({
      next: (response) => {
        if (!response.success) {
          this.errorMessage = response.message;
        } else {
          this.errorMessage = null;
        }
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to set frequency';
      }
    });
  }

  setMode() {
    this.rigControlService.setMode(this.stationId, this.selectedMode).subscribe({
      next: (response) => {
        if (!response.success) {
          this.errorMessage = response.message;
        } else {
          this.errorMessage = null;
        }
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to set mode';
      }
    });
  }

  togglePTT() {
    const newState = !this.pttActive;
    this.rigControlService.setPTT(this.stationId, newState).subscribe({
      next: (response) => {
        if (!response.success) {
          this.errorMessage = response.message;
        } else {
          this.errorMessage = null;
          this.pttActive = newState;
        }
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to toggle PTT';
      }
    });
  }

  refreshStatus() {
    this.rigControlService.getStatus(this.stationId).subscribe({
      next: (response) => {
        if (response.success && response.result) {
          this.currentStatus = response.result.status || response.result;
        } else {
          this.errorMessage = response.message;
        }
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to get status';
      }
    });
  }

  formatFrequency(hz: number | undefined): string {
    if (!hz) return 'N/A';
    return (hz / 1000000).toFixed(3) + ' MHz';
  }

  formatEventTime(timestamp: string): string {
    return new Date(timestamp).toLocaleTimeString();
  }
}
