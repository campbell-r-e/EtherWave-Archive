import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MapService, MapFilters } from '../../services/map.service';

/**
 * Map export dialog component
 * Allows users to export map data in multiple formats
 */
@Component({
  selector: 'app-map-export-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './map-export-dialog.component.html',
  styleUrls: ['./map-export-dialog.component.css']
})
export class MapExportDialogComponent {
  @Input() logId!: number;
  @Input() filters?: MapFilters;
  @Output() closeDialog = new EventEmitter<void>();

  isVisible: boolean = false;
  isExporting: boolean = false;
  selectedFormat: string = 'geojson';
  errorMessage: string = '';

  exportFormats = [
    { value: 'geojson', label: 'GeoJSON', description: 'Geographic data format for GIS tools', icon: 'bi-map' },
    { value: 'kml', label: 'KML', description: 'Google Earth compatible format', icon: 'bi-globe' },
    { value: 'csv', label: 'CSV', description: 'Spreadsheet format with location data', icon: 'bi-table' },
    { value: 'adif', label: 'ADIF', description: 'Amateur radio standard with coordinates', icon: 'bi-broadcast' }
  ];

  constructor(private mapService: MapService) {}

  /**
   * Show export dialog
   */
  show(): void {
    this.isVisible = true;
    this.errorMessage = '';
  }

  /**
   * Hide export dialog
   */
  hide(): void {
    this.isVisible = false;
    this.closeDialog.emit();
  }

  /**
   * Export map data in selected format
   */
  async exportMap(): Promise<void> {
    if (!this.logId) {
      this.errorMessage = 'No log ID provided';
      return;
    }

    this.isExporting = true;
    this.errorMessage = '';

    try {
      const response = await this.mapService.exportMap(
        this.logId,
        this.selectedFormat,
        this.filters
      ).toPromise();

      if (!response || !response.success) {
        throw new Error(response?.message || 'Export failed');
      }

      // Create download link
      const blob = new Blob([response.content], { type: response.mimeType });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = response.filename;
      link.click();
      window.URL.revokeObjectURL(url);

      // Close dialog after successful export
      setTimeout(() => this.hide(), 500);
    } catch (error: any) {
      console.error('Export error:', error);
      this.errorMessage = error.message || 'Failed to export map data';
    } finally {
      this.isExporting = false;
    }
  }

  /**
   * Get selected format details
   */
  getSelectedFormatDetails(): any {
    return this.exportFormats.find(f => f.value === this.selectedFormat);
  }
}
