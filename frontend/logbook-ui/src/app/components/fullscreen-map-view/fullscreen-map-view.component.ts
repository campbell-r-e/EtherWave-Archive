import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QSOMapComponent } from '../qso-map/qso-map.component';
import { MapVisualizationComponent } from '../map-visualization/map-visualization.component';
import { MapFilterPanelComponent } from '../map-filter-panel/map-filter-panel.component';
import { ContestOverlayControlsComponent, OverlayToggle } from '../contest-overlay-controls/contest-overlay-controls.component';
import { MapFilters } from '../../services/map.service';

@Component({
  selector: 'app-fullscreen-map-view',
  standalone: true,
  imports: [
    CommonModule,
    QSOMapComponent,
    MapVisualizationComponent,
    MapFilterPanelComponent,
    ContestOverlayControlsComponent
  ],
  templateUrl: './fullscreen-map-view.component.html',
  styleUrls: ['./fullscreen-map-view.component.css']
})
export class FullscreenMapViewComponent {
  @Output() closeMap = new EventEmitter<void>();

  activeTab: 'interactive' | 'states' = 'interactive';
  currentFilters: MapFilters = {};
  currentOverlays: OverlayToggle = {
    cqZones: false,
    ituZones: false,
    arrlSections: false,
    dxccEntities: false
  };

  setActiveTab(tab: 'interactive' | 'states'): void {
    this.activeTab = tab;
  }

  onClose(): void {
    this.closeMap.emit();
  }

  /**
   * Handle filter changes from filter panel
   */
  onFiltersChanged(filters: MapFilters): void {
    this.currentFilters = filters;
    // TODO: Apply filters to map
    console.log('Filters changed:', filters);
  }

  /**
   * Handle filter clearing
   */
  onFiltersCleared(): void {
    this.currentFilters = {};
    // TODO: Clear filters from map
    console.log('Filters cleared');
  }

  /**
   * Handle overlay toggles from contest overlay controls
   */
  onOverlaysChanged(overlays: OverlayToggle): void {
    this.currentOverlays = overlays;
    // TODO: Apply overlays to map
    console.log('Overlays changed:', overlays);
  }
}
