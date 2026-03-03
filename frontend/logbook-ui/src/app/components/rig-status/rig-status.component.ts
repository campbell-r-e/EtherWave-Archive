import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { RigControlService, RigStatus } from '../../services/rig-control.service';

@Component({
  selector: 'app-rig-status',
  imports: [CommonModule],
  templateUrl: './rig-status.component.html',
  styleUrls: ['./rig-status.component.css']
})
export class RigStatusComponent implements OnInit, OnDestroy {
  rigStatus: RigStatus | null = null;
  connected = false;

  private statusSub?: Subscription;
  private eventSub?: Subscription;

  constructor(private rigControlService: RigControlService) {}

  ngOnInit(): void {
    this.statusSub = this.rigControlService.onStatusUpdate().subscribe(({ status }) => {
      this.rigStatus = status;
      this.connected = status.connected !== false;
    });

    this.eventSub = this.rigControlService.onEventUpdate().subscribe(({ event }) => {
      if (event.eventType === 'client_disconnected' || event.eventType === 'error') {
        this.connected = false;
      } else if (event.eventType === 'client_connected') {
        this.connected = true;
      }
    });
  }

  ngOnDestroy(): void {
    this.statusSub?.unsubscribe();
    this.eventSub?.unsubscribe();
  }

  formatFrequency(hz: number | undefined): string {
    if (!hz) return '---';
    return (hz / 1_000_000).toFixed(4) + ' MHz';
  }

  getConnectionStatusClass(): string {
    return this.connected ? 'bg-success' : 'bg-secondary';
  }

  getPTTClass(): string {
    return this.rigStatus?.ptt ? 'text-danger blink' : 'text-muted';
  }
}
