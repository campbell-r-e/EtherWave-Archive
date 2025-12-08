import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MapFilters } from '../../services/map.service';

/**
 * Map filter panel component
 * Provides UI for all 10 map filters:
 * - Band, Mode, Station, Operator, DXCC
 * - Date Range (From/To), Confirmed Status
 * - Continent, State, Exchange
 */
@Component({
  selector: 'app-map-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './map-filter-panel.component.html',
  styleUrls: ['./map-filter-panel.component.css']
})
export class MapFilterPanelComponent {
  @Input() availableBands: string[] = ['160M', '80M', '40M', '20M', '15M', '10M', '6M', '2M', '70CM'];
  @Input() availableModes: string[] = ['SSB', 'CW', 'FT8', 'FT4', 'RTTY', 'PSK31', 'FM', 'AM'];
  @Input() availableStations: number[] = [1, 2, 3, 4, 5, 6];
  @Input() availableContinents: string[] = ['NA', 'SA', 'EU', 'AF', 'AS', 'OC'];

  @Output() filtersChanged = new EventEmitter<MapFilters>();
  @Output() filtersCleared = new EventEmitter<void>();

  // Filter model
  filters: MapFilters = {};

  // UI State
  isExpanded: boolean = false;
  activeFilterCount: number = 0;

  /**
   * Apply current filters
   */
  applyFilters(): void {
    this.updateActiveFilterCount();
    this.filtersChanged.emit(this.filters);
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.filters = {};
    this.activeFilterCount = 0;
    this.filtersCleared.emit();
    this.filtersChanged.emit(this.filters);
  }

  /**
   * Toggle panel expansion
   */
  togglePanel(): void {
    this.isExpanded = !this.isExpanded;
  }

  /**
   * Update count of active filters
   */
  private updateActiveFilterCount(): void {
    this.activeFilterCount = 0;

    if (this.filters.band) this.activeFilterCount++;
    if (this.filters.mode) this.activeFilterCount++;
    if (this.filters.station) this.activeFilterCount++;
    if (this.filters.operator) this.activeFilterCount++;
    if (this.filters.dxcc) this.activeFilterCount++;
    if (this.filters.dateFrom) this.activeFilterCount++;
    if (this.filters.dateTo) this.activeFilterCount++;
    if (this.filters.confirmed !== undefined) this.activeFilterCount++;
    if (this.filters.continent) this.activeFilterCount++;
    if (this.filters.state) this.activeFilterCount++;
    if (this.filters.exchange) this.activeFilterCount++;
  }

  /**
   * Remove specific filter
   */
  removeFilter(filterName: keyof MapFilters): void {
    delete this.filters[filterName];
    this.applyFilters();
  }

  /**
   * Check if any filters are active
   */
  hasActiveFilters(): boolean {
    return this.activeFilterCount > 0;
  }

  /**
   * Get display text for a filter value
   */
  getFilterDisplayText(key: keyof MapFilters): string {
    const value = this.filters[key];

    if (value === undefined || value === null || value === '') {
      return '';
    }

    // Special handling for boolean confirmed filter
    if (key === 'confirmed') {
      return value ? 'Confirmed only' : 'All QSOs';
    }

    // Special handling for dates
    if (key === 'dateFrom' || key === 'dateTo') {
      return value.toString();
    }

    return value.toString();
  }

  /**
   * Get filter label text
   */
  getFilterLabel(key: keyof MapFilters): string {
    const labels: { [K in keyof MapFilters]?: string } = {
      band: 'Band',
      mode: 'Mode',
      station: 'Station',
      operator: 'Operator',
      dxcc: 'DXCC',
      dateFrom: 'From',
      dateTo: 'To',
      confirmed: 'Confirmed',
      continent: 'Continent',
      state: 'State',
      exchange: 'Exchange'
    };

    return labels[key] || key;
  }
}
