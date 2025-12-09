import { Injectable } from '@angular/core';
import * as L from 'leaflet';
import { MapService, GridSquare } from './map.service';

/**
 * Service for managing Maidenhead grid square overlays on Leaflet maps
 * Handles grid rendering, coloring, and interactions
 */
@Injectable({
  providedIn: 'root'
})
export class GridOverlayService {
  private gridLayers: L.LayerGroup[] = [];
  private gridPrecision: number = 4; // Default: 4-character grids (e.g., FN31)

  constructor(private mapService: MapService) {}

  /**
   * Add grid coverage overlay to map
   * @param map Leaflet map instance
   * @param logId Log ID
   * @param precision Grid precision (2, 4, 6, or 8)
   * @param includeNeighbors Show neighboring unworked grids
   */
  async addGridOverlay(
    map: L.Map,
    logId: number,
    precision: number = 4,
    includeNeighbors: boolean = true
  ): Promise<L.LayerGroup> {
    this.gridPrecision = precision;

    // Fetch grid data from API
    const response = await this.mapService.getGridCoverage(logId, precision).toPromise();

    if (!response) {
      throw new Error('Failed to load grid coverage data');
    }

    const layerGroup = L.layerGroup();

    // Render worked grids
    response.grids.forEach(grid => {
      const rectangle = this.createGridRectangle(grid, true);
      rectangle.addTo(layerGroup);
    });

    // Render neighboring unworked grids
    if (includeNeighbors && response.neighbors) {
      response.neighbors.forEach(grid => {
        const rectangle = this.createGridRectangle(grid, false);
        rectangle.addTo(layerGroup);
      });
    }

    layerGroup.addTo(map);
    this.gridLayers.push(layerGroup);

    return layerGroup;
  }

  /**
   * Create Leaflet rectangle for a grid square
   */
  private createGridRectangle(grid: GridSquare, isWorked: boolean): L.Rectangle {
    const bounds = this.getGridBounds(grid.grid);

    const color = isWorked ? this.getGridColor(grid.qsoCount) : '#999999';
    const fillOpacity = isWorked ? 0.5 : 0.1;
    const weight = isWorked ? 2 : 1;

    const rectangle = L.rectangle(bounds, {
      color: color,
      fillColor: color,
      fillOpacity: fillOpacity,
      weight: weight,
      opacity: 0.8
    });

    // Add popup with grid info
    const popupContent = this.createGridPopup(grid, isWorked);
    rectangle.bindPopup(popupContent);

    // Highlight on hover
    rectangle.on('mouseover', () => {
      rectangle.setStyle({
        weight: 3,
        fillOpacity: isWorked ? 0.7 : 0.2
      });
    });

    rectangle.on('mouseout', () => {
      rectangle.setStyle({
        weight: weight,
        fillOpacity: fillOpacity
      });
    });

    return rectangle;
  }

  /**
   * Calculate lat/lon bounds for a Maidenhead grid square
   */
  private getGridBounds(grid: string): L.LatLngBoundsExpression {
    const coords = this.maidenheadToCoords(grid);

    // Grid square sizes by precision
    const gridSizes: { [key: number]: { lat: number; lon: number } } = {
      2: { lat: 10, lon: 20 },   // Field (e.g., FN)
      4: { lat: 1, lon: 2 },     // Square (e.g., FN31)
      6: { lat: 1/24, lon: 2/24 }, // Subsquare (e.g., FN31pr)
      8: { lat: 1/240, lon: 2/240 } // Extended subsquare (e.g., FN31pr00)
    };

    const size = gridSizes[grid.length] || gridSizes[4];

    return [
      [coords.lat, coords.lon],
      [coords.lat + size.lat, coords.lon + size.lon]
    ];
  }

  /**
   * Convert Maidenhead grid to lat/lon (southwest corner)
   */
  private maidenheadToCoords(grid: string): { lat: number; lon: number } {
    grid = grid.toUpperCase();

    // Field (first 2 characters)
    const lonField = (grid.charCodeAt(0) - 65) * 20 - 180;
    const latField = (grid.charCodeAt(1) - 65) * 10 - 90;

    if (grid.length === 2) {
      return { lat: latField, lon: lonField };
    }

    // Square (next 2 digits)
    const lonSquare = parseInt(grid[2]) * 2;
    const latSquare = parseInt(grid[3]) * 1;

    if (grid.length === 4) {
      return { lat: latField + latSquare, lon: lonField + lonSquare };
    }

    // Subsquare (next 2 characters)
    const lonSubsquare = (grid.charCodeAt(4) - 65) * (2 / 24);
    const latSubsquare = (grid.charCodeAt(5) - 65) * (1 / 24);

    if (grid.length === 6) {
      return {
        lat: latField + latSquare + latSubsquare,
        lon: lonField + lonSquare + lonSubsquare
      };
    }

    // Extended subsquare (next 2 digits)
    const lonExtended = parseInt(grid[6]) * (2 / 240);
    const latExtended = parseInt(grid[7]) * (1 / 240);

    return {
      lat: latField + latSquare + latSubsquare + latExtended,
      lon: lonField + lonSquare + lonSubsquare + lonExtended
    };
  }

  /**
   * Get color for grid based on QSO count
   */
  private getGridColor(qsoCount: number): string {
    // EtherWave theme gradient from blue to cyan based on activity
    if (qsoCount >= 50) return '#00ff88'; // Green - very active
    if (qsoCount >= 20) return '#00d4ff'; // Cyan - active
    if (qsoCount >= 10) return '#0080ff'; // Blue - moderate
    if (qsoCount >= 5) return '#4da6ff';  // Light blue - some activity
    return '#80bfff'; // Very light blue - minimal activity
  }

  /**
   * Create popup content for grid square
   */
  private createGridPopup(grid: GridSquare, isWorked: boolean): string {
    if (!isWorked) {
      return `
        <div class="grid-popup">
          <h6 class="text-muted">${grid.grid}</h6>
          <p class="mb-0"><em>Not worked</em></p>
        </div>
      `;
    }

    return `
      <div class="grid-popup">
        <h6>${grid.grid}</h6>
        <table class="table table-sm mb-0">
          <tr>
            <td><strong>QSOs:</strong></td>
            <td>${grid.qsoCount}</td>
          </tr>
          <tr>
            <td><strong>Bands:</strong></td>
            <td>${grid.bands}</td>
          </tr>
          <tr>
            <td><strong>Modes:</strong></td>
            <td>${grid.modes}</td>
          </tr>
          <tr>
            <td><strong>First QSO:</strong></td>
            <td>${this.formatDate(grid.firstQSO)}</td>
          </tr>
          <tr>
            <td><strong>Last QSO:</strong></td>
            <td>${this.formatDate(grid.lastQSO)}</td>
          </tr>
        </table>
      </div>
    `;
  }

  /**
   * Format date string
   */
  private formatDate(dateStr: string): string {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleDateString();
  }

  /**
   * Remove all grid overlays from map
   */
  clearGridOverlays(map: L.Map): void {
    this.gridLayers.forEach(layer => {
      map.removeLayer(layer);
    });
    this.gridLayers = [];
  }

  /**
   * Toggle grid overlay visibility
   */
  toggleGridOverlay(layerGroup: L.LayerGroup, map: L.Map, visible: boolean): void {
    if (visible) {
      layerGroup.addTo(map);
    } else {
      map.removeLayer(layerGroup);
    }
  }

  /**
   * Get grid statistics
   */
  async getGridStatistics(logId: number, precision: number = 4): Promise<{
    totalGrids: number;
    workedGrids: number;
    percentageComplete: number;
  }> {
    const response = await this.mapService.getGridCoverage(logId, precision).toPromise();

    if (!response) {
      return { totalGrids: 0, workedGrids: 0, percentageComplete: 0 };
    }

    return {
      totalGrids: response.totalGrids,
      workedGrids: response.workedGrids,
      percentageComplete: (response.workedGrids / response.totalGrids) * 100
    };
  }
}
