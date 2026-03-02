import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QSO } from '../../models/qso.model';

@Component({
  selector: 'app-qsl-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './qsl-card.component.html',
  styleUrls: ['./qsl-card.component.css']
})
export class QslCardComponent {
  @Input() qso: QSO | null = null;
  @Output() closed = new EventEmitter<void>();

  close(): void {
    this.closed.emit();
  }

  print(): void {
    window.print();
  }

  /** Format frequency kHz → MHz string with 3 decimal places */
  formatFreq(khz: number | undefined): string {
    if (!khz) return '—';
    return (khz / 1000).toFixed(3) + ' MHz';
  }

  /** Format date string for display */
  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    return dateStr;
  }

  /** Determine mode display — prefer mode, fallback to band context */
  get modeDisplay(): string {
    return this.qso?.mode || '—';
  }

  /** RST sent/received or default */
  get rstDisplay(): string {
    const sent = this.qso?.rstSent || '59';
    const rcvd = this.qso?.rstRcvd || '59';
    return `${sent} / ${rcvd}`;
  }
}
