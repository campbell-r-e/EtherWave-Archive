import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DXClusterService, DXSpot } from '../../services/dx-cluster/dx-cluster.service';
import { Subscription } from 'rxjs';

const BANDS = ['', '160m', '80m', '60m', '40m', '30m', '20m', '17m', '15m', '12m', '10m', '6m', '2m', '70cm'];

@Component({
  selector: 'app-dx-cluster-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dx-cluster-panel.component.html',
  styleUrls: ['./dx-cluster-panel.component.css']
})
export class DXClusterPanelComponent implements OnInit, OnDestroy {
  spots: DXSpot[] = [];
  loading = false;
  error: string | null = null;
  lastUpdated: Date | null = null;

  selectedBand = '';
  limit = 50;
  availableBands = BANDS;

  isExpanded = true;

  private sub: Subscription | null = null;

  constructor(private dxClusterService: DXClusterService) {}

  ngOnInit(): void {
    this.sub = this.dxClusterService.spots$.subscribe(spots => {
      this.spots = spots;
      if (spots.length > 0) {
        this.lastUpdated = new Date();
        this.loading = false;
      }
    });
    this.refresh();
  }

  ngOnDestroy(): void {
    this.dxClusterService.stopPolling();
    this.sub?.unsubscribe();
  }

  refresh(): void {
    this.loading = true;
    this.error = null;
    this.dxClusterService.startPolling(this.limit, this.selectedBand);
  }

  onFilterChange(): void {
    this.refresh();
  }

  togglePanel(): void {
    this.isExpanded = !this.isExpanded;
  }

  /** Format frequency: show in MHz with 1 decimal */
  formatFreq(khz: number): string {
    if (!khz) return '—';
    return (khz / 1000).toFixed(1);
  }

  /** Age of spot in human-readable form */
  spotAge(isoTime: string): string {
    if (!isoTime) return '';
    const diff = Date.now() - new Date(isoTime).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'now';
    if (mins < 60) return `${mins}m`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h`;
    return `${Math.floor(hours / 24)}d`;
  }

  /** CSS class for band coloring */
  bandClass(band: string): string {
    const b = (band || '').toLowerCase();
    if (['160m', '80m', '60m', '40m'].includes(b)) return 'band-lf';
    if (['30m', '20m', '17m', '15m', '12m'].includes(b)) return 'band-hf';
    if (['10m', '6m'].includes(b)) return 'band-vhf';
    if (['2m', '70cm'].includes(b)) return 'band-uhf';
    return '';
  }
}
