import { Injectable } from '@angular/core';
import * as L from 'leaflet';
import 'leaflet.heat';
import { MapService } from './map.service';

/**
 * Service for managing heatmap overlays on Leaflet maps
 * Shows density visualization of QSO locations
 */
@Injectable({
  providedIn: 'root'
})
export class HeatmapService {
  private heatmapLayers: L.Layer[] = [];

  constructor(private mapService: MapService) {}

  /**
   * Add heatmap overlay to map
   * @param map Leaflet map instance
   * @param logId Log ID
   * @param filters Optional filters to apply
   * @param options Heatmap configuration options
   */
  async addHeatmap(
    map: L.Map,
    logId: number,
    filters?: any,
    options?: HeatmapOptions
  ): Promise<L.Layer> {
    const defaultOptions: HeatmapOptions = {
      radius: 25,
      blur: 15,
      maxZoom: 17,
      max: 1.0,
      gradient: {
        0.0: '#0080ff',   // Blue (low density)
        0.2: '#00d4ff',   // Cyan
        0.4: '#00ff88',   // Green
        0.6: '#ffaa00',   // Orange
        0.8: '#ff5500',   // Red-orange
        1.0: '#ff0088'    // Pink (high density)
      },
      ...options
    };

    // Fetch heatmap data from API
    const response = await this.mapService.getHeatmapData(logId, filters).toPromise();

    if (!response || !response.points || response.points.length === 0) {
      throw new Error('No heatmap data available');
    }

    // Convert API response to Leaflet heatmap format
    const heatmapPoints: [number, number, number][] = response.points.map(point => [
      point.lat,
      point.lon,
      point.intensity || 1.0
    ]);

    // Create heatmap layer
    const heatmapLayer = (L as any).heatLayer(heatmapPoints, {
      radius: defaultOptions.radius,
      blur: defaultOptions.blur,
      maxZoom: defaultOptions.maxZoom,
      max: defaultOptions.max,
      gradient: defaultOptions.gradient
    });

    heatmapLayer.addTo(map);
    this.heatmapLayers.push(heatmapLayer);

    return heatmapLayer;
  }

  /**
   * Remove all heatmap overlays from map
   */
  clearHeatmaps(map: L.Map): void {
    this.heatmapLayers.forEach(layer => {
      map.removeLayer(layer);
    });
    this.heatmapLayers = [];
  }

  /**
   * Toggle heatmap visibility
   */
  toggleHeatmap(heatmapLayer: L.Layer, map: L.Map, visible: boolean): void {
    if (visible) {
      heatmapLayer.addTo(map);
    } else {
      map.removeLayer(heatmapLayer);
    }
  }

  /**
   * Update heatmap options
   */
  updateHeatmapOptions(
    heatmapLayer: any,
    options: Partial<HeatmapOptions>
  ): void {
    if (options.radius !== undefined) {
      heatmapLayer.setOptions({ radius: options.radius });
    }
    if (options.blur !== undefined) {
      heatmapLayer.setOptions({ blur: options.blur });
    }
    if (options.max !== undefined) {
      heatmapLayer.setOptions({ max: options.max });
    }
    if (options.gradient !== undefined) {
      heatmapLayer.setOptions({ gradient: options.gradient });
    }
  }

  /**
   * Create gradient configuration for heatmap
   */
  createGradient(type: 'default' | 'warm' | 'cool' | 'monochrome'): { [key: number]: string } {
    switch (type) {
      case 'warm':
        return {
          0.0: '#ffff00',  // Yellow
          0.3: '#ffaa00',  // Orange
          0.6: '#ff5500',  // Red-orange
          1.0: '#ff0000'   // Red
        };
      case 'cool':
        return {
          0.0: '#00ffff',  // Cyan
          0.3: '#0080ff',  // Blue
          0.6: '#0040ff',  // Dark blue
          1.0: '#000080'   // Navy
        };
      case 'monochrome':
        return {
          0.0: '#cccccc',  // Light gray
          0.3: '#888888',  // Gray
          0.6: '#444444',  // Dark gray
          1.0: '#000000'   // Black
        };
      case 'default':
      default:
        return {
          0.0: '#0080ff',  // Blue
          0.2: '#00d4ff',  // Cyan
          0.4: '#00ff88',  // Green
          0.6: '#ffaa00',  // Orange
          0.8: '#ff5500',  // Red-orange
          1.0: '#ff0088'   // Pink
        };
    }
  }

  /**
   * Get statistics about heatmap data
   */
  async getHeatmapStats(logId: number, filters?: any): Promise<HeatmapStats> {
    const response = await this.mapService.getHeatmapData(logId, filters).toPromise();

    if (!response || !response.points) {
      return {
        totalPoints: 0,
        maxIntensity: 0,
        avgIntensity: 0,
        minIntensity: 0
      };
    }

    const intensities = response.points.map(p => p.intensity || 1.0);
    const totalPoints = response.points.length;
    const maxIntensity = Math.max(...intensities);
    const minIntensity = Math.min(...intensities);
    const avgIntensity = intensities.reduce((a, b) => a + b, 0) / totalPoints;

    return {
      totalPoints,
      maxIntensity,
      avgIntensity,
      minIntensity
    };
  }
}

/**
 * Heatmap configuration options
 */
export interface HeatmapOptions {
  radius?: number;           // Radius of each point in pixels (default: 25)
  blur?: number;             // Amount of blur (default: 15)
  maxZoom?: number;          // Maximum zoom level for heatmap (default: 17)
  max?: number;              // Maximum point intensity (default: 1.0)
  gradient?: { [key: number]: string };  // Color gradient stops
}

/**
 * Heatmap statistics
 */
export interface HeatmapStats {
  totalPoints: number;
  maxIntensity: number;
  avgIntensity: number;
  minIntensity: number;
}
