import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface OverlayToggle {
  cqZones: boolean;
  ituZones: boolean;
  arrlSections: boolean;
  dxccEntities: boolean;
}

/**
 * Contest Overlay Controls Component
 * Provides UI for toggling contest-specific overlay layers:
 * - CQ Zones (40 zones worldwide)
 * - ITU Zones (90 zones worldwide)
 * - ARRL Sections (83 US/Canada sections)
 * - DXCC Entities (340+ countries/territories)
 */
@Component({
  selector: 'app-contest-overlay-controls',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contest-overlay-controls.component.html',
  styleUrls: ['./contest-overlay-controls.component.css']
})
export class ContestOverlayControlsComponent {
  @Output() overlaysChanged = new EventEmitter<OverlayToggle>();

  overlays: OverlayToggle = {
    cqZones: false,
    ituZones: false,
    arrlSections: false,
    dxccEntities: false
  };

  isExpanded: boolean = false;

  /**
   * Toggle dropdown panel
   */
  togglePanel(): void {
    this.isExpanded = !this.isExpanded;
  }

  /**
   * Handle overlay toggle change
   */
  onOverlayChange(): void {
    this.overlaysChanged.emit(this.overlays);
  }

  /**
   * Clear all overlays
   */
  clearAll(): void {
    this.overlays = {
      cqZones: false,
      ituZones: false,
      arrlSections: false,
      dxccEntities: false
    };
    this.overlaysChanged.emit(this.overlays);
  }

  /**
   * Check if any overlays are active
   */
  hasActiveOverlays(): boolean {
    return this.overlays.cqZones ||
           this.overlays.ituZones ||
           this.overlays.arrlSections ||
           this.overlays.dxccEntities;
  }

  /**
   * Get count of active overlays
   */
  getActiveOverlayCount(): number {
    let count = 0;
    if (this.overlays.cqZones) count++;
    if (this.overlays.ituZones) count++;
    if (this.overlays.arrlSections) count++;
    if (this.overlays.dxccEntities) count++;
    return count;
  }
}
