import { Component, EventEmitter, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QSOMapComponent } from '../qso-map/qso-map.component';
import { MapVisualizationComponent } from '../map-visualization/map-visualization.component';
import { MapFilterPanelComponent } from '../map-filter-panel/map-filter-panel.component';
import { ContestOverlayControlsComponent, OverlayToggle } from '../contest-overlay-controls/contest-overlay-controls.component';
import { MapFilters } from '../../services/map.service';
import { LogService } from '../../services/log/log.service';
import { Subscription } from 'rxjs';

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
export class FullscreenMapViewComponent implements OnInit, OnDestroy {
  @Output() closeMap = new EventEmitter<void>();
  @ViewChild('qsoMap') qsoMap?: QSOMapComponent;

  activeTab: 'interactive' | 'states' = 'interactive';
  currentFilters: MapFilters = {};
  currentLogId: number | null = null;
  currentOverlays: OverlayToggle = {
    cqZones: false,
    ituZones: false,
    arrlSections: false,
    dxccEntities: false
  };

  private logSubscription?: Subscription;

  constructor(private logService: LogService) {}

  ngOnInit(): void {
    this.logSubscription = this.logService.currentLog$.subscribe(log => {
      this.currentLogId = log?.id ?? null;
    });
  }

  ngOnDestroy(): void {
    this.logSubscription?.unsubscribe();
  }

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
    this.qsoMap?.applyFilters(filters);
  }

  /**
   * Handle filter clearing
   */
  onFiltersCleared(): void {
    this.currentFilters = {};
    this.qsoMap?.applyFilters({});
  }

  /**
   * Handle overlay toggles from contest overlay controls
   */
  async onOverlaysChanged(overlays: OverlayToggle): Promise<void> {
    const prev = this.currentOverlays;
    this.currentOverlays = overlays;

    if (this.qsoMap) {
      if (overlays.cqZones !== prev.cqZones) await this.qsoMap.toggleContestOverlay('cqZones');
      if (overlays.ituZones !== prev.ituZones) await this.qsoMap.toggleContestOverlay('ituZones');
      if (overlays.arrlSections !== prev.arrlSections) await this.qsoMap.toggleContestOverlay('arrlSections');
      if (overlays.dxccEntities !== prev.dxccEntities) await this.qsoMap.toggleContestOverlay('dxcc');
    }
  }
}
