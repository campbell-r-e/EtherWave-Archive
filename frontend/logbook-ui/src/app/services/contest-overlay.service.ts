import { Injectable } from '@angular/core';
import * as L from 'leaflet';
import { MapService } from './map.service';

/**
 * Service for managing contest overlays on Leaflet maps
 * Displays CQ zones, ITU zones, ARRL sections, and DXCC entities
 */
@Injectable({
  providedIn: 'root'
})
export class ContestOverlayService {
  private overlayLayers: Map<string, L.LayerGroup> = new Map();

  constructor(private mapService: MapService) {}

  /**
   * Add CQ zone overlay to map
   */
  async addCQZoneOverlay(
    map: L.Map,
    logId: number
  ): Promise<L.LayerGroup> {
    const response: any = await this.mapService.getContestOverlay(logId, 'CQ_ZONES').toPromise();

    if (!response) {
      throw new Error('Failed to load CQ zone data');
    }

    const layerGroup = L.layerGroup();

    response.zones.forEach((zone: any) => {
      const marker = this.createZoneMarker(
        zone.zone.toString(),
        zone.centerLat,
        zone.centerLon,
        zone.worked,
        zone.qsoCount
      );
      marker.addTo(layerGroup);
    });

    layerGroup.addTo(map);
    this.overlayLayers.set('cq-zones', layerGroup);

    return layerGroup;
  }

  /**
   * Add ITU zone overlay to map
   */
  async addITUZoneOverlay(
    map: L.Map,
    logId: number
  ): Promise<L.LayerGroup> {
    const response: any = await this.mapService.getContestOverlay(logId, 'ITU_ZONES').toPromise();

    if (!response) {
      throw new Error('Failed to load ITU zone data');
    }

    const layerGroup = L.layerGroup();

    response.zones.forEach((zone: any) => {
      const marker = this.createZoneMarker(
        zone.zone.toString(),
        zone.centerLat,
        zone.centerLon,
        zone.worked,
        zone.qsoCount
      );
      marker.addTo(layerGroup);
    });

    layerGroup.addTo(map);
    this.overlayLayers.set('itu-zones', layerGroup);

    return layerGroup;
  }

  /**
   * Add ARRL sections overlay to map
   */
  async addARRLSectionsOverlay(
    map: L.Map,
    logId: number
  ): Promise<L.LayerGroup> {
    const response: any = await this.mapService.getContestOverlay(logId, 'ARRL_SECTIONS').toPromise();

    if (!response) {
      throw new Error('Failed to load ARRL sections data');
    }

    const layerGroup = L.layerGroup();

    response.sections.forEach((section: any) => {
      const marker = this.createSectionMarker(
        section.section,
        section.centerLat,
        section.centerLon,
        section.worked,
        section.qsoCount
      );
      marker.addTo(layerGroup);
    });

    layerGroup.addTo(map);
    this.overlayLayers.set('arrl-sections', layerGroup);

    return layerGroup;
  }

  /**
   * Add DXCC entities overlay to map
   */
  async addDXCCOverlay(
    map: L.Map,
    logId: number
  ): Promise<L.LayerGroup> {
    const response: any = await this.mapService.getContestOverlay(logId, 'DXCC').toPromise();

    if (!response) {
      throw new Error('Failed to load DXCC data');
    }

    const layerGroup = L.layerGroup();

    response.entities.forEach((entity: any) => {
      const marker = this.createDXCCMarker(
        entity.dxccCode,
        entity.name,
        entity.centerLat,
        entity.centerLon,
        entity.worked,
        entity.qsoCount
      );
      marker.addTo(layerGroup);
    });

    layerGroup.addTo(map);
    this.overlayLayers.set('dxcc', layerGroup);

    return layerGroup;
  }

  /**
   * Create marker for zone (CQ/ITU)
   */
  private createZoneMarker(
    zone: string,
    lat: number,
    lon: number,
    worked: boolean,
    qsoCount: number
  ): L.CircleMarker {
    const color = worked ? '#00ff88' : '#ff6666';
    const fillOpacity = worked ? 0.3 : 0.1;

    const marker = L.circleMarker([lat, lon], {
      radius: 8,
      fillColor: color,
      color: color,
      weight: 2,
      opacity: 0.8,
      fillOpacity: fillOpacity
    });

    const popupContent = `
      <div class="contest-overlay-popup">
        <h6>Zone ${zone}</h6>
        <p><strong>Status:</strong> ${worked ? 'Worked' : 'Needed'}</p>
        ${worked ? `<p><strong>QSOs:</strong> ${qsoCount}</p>` : ''}
      </div>
    `;

    marker.bindPopup(popupContent);

    return marker;
  }

  /**
   * Create marker for ARRL section
   */
  private createSectionMarker(
    section: string,
    lat: number,
    lon: number,
    worked: boolean,
    qsoCount: number
  ): L.CircleMarker {
    const color = worked ? '#0080ff' : '#ffaa00';
    const fillOpacity = worked ? 0.3 : 0.1;

    const marker = L.circleMarker([lat, lon], {
      radius: 8,
      fillColor: color,
      color: color,
      weight: 2,
      opacity: 0.8,
      fillOpacity: fillOpacity
    });

    const popupContent = `
      <div class="contest-overlay-popup">
        <h6>${section}</h6>
        <p><strong>Status:</strong> ${worked ? 'Worked' : 'Needed'}</p>
        ${worked ? `<p><strong>QSOs:</strong> ${qsoCount}</p>` : ''}
      </div>
    `;

    marker.bindPopup(popupContent);

    return marker;
  }

  /**
   * Create marker for DXCC entity
   */
  private createDXCCMarker(
    dxccCode: number,
    name: string,
    lat: number,
    lon: number,
    worked: boolean,
    qsoCount: number
  ): L.CircleMarker {
    const color = worked ? '#00d4ff' : '#aa00ff';
    const fillOpacity = worked ? 0.3 : 0.1;

    const marker = L.circleMarker([lat, lon], {
      radius: 6,
      fillColor: color,
      color: color,
      weight: 2,
      opacity: 0.8,
      fillOpacity: fillOpacity
    });

    const popupContent = `
      <div class="contest-overlay-popup">
        <h6>${name}</h6>
        <p><strong>DXCC:</strong> ${dxccCode}</p>
        <p><strong>Status:</strong> ${worked ? 'Worked' : 'Needed'}</p>
        ${worked ? `<p><strong>QSOs:</strong> ${qsoCount}</p>` : ''}
      </div>
    `;

    marker.bindPopup(popupContent);

    return marker;
  }

  /**
   * Remove specific overlay from map
   */
  clearOverlay(map: L.Map, overlayType: string): void {
    const layer = this.overlayLayers.get(overlayType);
    if (layer) {
      map.removeLayer(layer);
      this.overlayLayers.delete(overlayType);
    }
  }

  /**
   * Remove all overlays from map
   */
  clearAllOverlays(map: L.Map): void {
    this.overlayLayers.forEach((layer, key) => {
      map.removeLayer(layer);
    });
    this.overlayLayers.clear();
  }

  /**
   * Toggle overlay visibility
   */
  toggleOverlay(overlayType: string, map: L.Map, visible: boolean): void {
    const layer = this.overlayLayers.get(overlayType);
    if (layer) {
      if (visible) {
        layer.addTo(map);
      } else {
        map.removeLayer(layer);
      }
    }
  }
}
