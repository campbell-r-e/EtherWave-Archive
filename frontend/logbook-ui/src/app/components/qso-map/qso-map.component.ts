import { Component, OnInit, OnDestroy, Input, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { MapService, MapFilters, MapMarker, MapCluster } from '../../services/map.service';
import { GridOverlayService } from '../../services/grid-overlay.service';
import { HeatmapService } from '../../services/heatmap.service';
import { Subscription } from 'rxjs';

/**
 * Interactive QSO map component with Leaflet
 * Features:
 * - Adaptive server-side clustering
 * - Dark/light theme tile layers
 * - QSO markers with popups
 * - Cluster markers with breakdown
 * - Grid square overlay with multiple precisions
 * - Real-time updates via WebSocket
 */
@Component({
  selector: 'app-qso-map',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './qso-map.component.html',
  styleUrls: ['./qso-map.component.css']
})
export class QSOMapComponent implements OnInit, OnDestroy {
  @Input() logId!: number;
  @Input() filters?: MapFilters;
  @Input() darkMode: boolean = false;
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef;

  private map!: L.Map;
  private markers: L.Marker[] = [];
  private clusterMarkers: L.Marker[] = [];
  private subscriptions: Subscription[] = [];

  // Tile layers
  private lightTileLayer!: L.TileLayer;
  private darkTileLayer!: L.TileLayer;
  private currentTileLayer!: L.TileLayer;

  // Grid overlay
  private gridOverlayLayer?: L.LayerGroup;
  gridOverlayVisible: boolean = false;
  gridPrecision: number = 4;

  // Heatmap overlay
  private heatmapLayer?: L.Layer;
  heatmapVisible: boolean = false;
  heatmapRadius: number = 25;

  // Map state
  currentZoom: number = 4;
  isLoading: boolean = false;
  totalQSOs: number = 0;
  displayedPoints: number = 0;
  clustered: boolean = false;

  // Default center (US)
  private defaultCenter: L.LatLngExpression = [39.8283, -98.5795];
  private defaultZoom: number = 4;

  constructor(
    private mapService: MapService,
    private gridOverlayService: GridOverlayService,
    private heatmapService: HeatmapService
  ) {}

  ngOnInit(): void {
    this.initializeMap();
    this.loadQSOLocations();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.map) {
      this.map.remove();
    }
  }

  /**
   * Initialize Leaflet map with tile layers
   */
  private initializeMap(): void {
    // Initialize map
    this.map = L.map(this.mapContainer.nativeElement, {
      center: this.defaultCenter,
      zoom: this.defaultZoom,
      zoomControl: true,
      attributionControl: true
    });

    // Light theme tiles (CartoDB Positron)
    this.lightTileLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
      subdomains: 'abcd',
      maxZoom: 19
    });

    // Dark theme tiles (CartoDB Dark Matter)
    this.darkTileLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
      subdomains: 'abcd',
      maxZoom: 19
    });

    // Set initial tile layer based on theme
    this.currentTileLayer = this.darkMode ? this.darkTileLayer : this.lightTileLayer;
    this.currentTileLayer.addTo(this.map);

    // Listen to zoom changes
    this.map.on('zoomend', () => {
      const newZoom = this.map.getZoom();
      if (newZoom !== this.currentZoom) {
        this.currentZoom = newZoom;
        this.loadQSOLocations();
      }
    });

    // Listen to move end (pan)
    this.map.on('moveend', () => {
      // Optionally reload data when panning (if implementing bounds-based loading)
      // this.loadQSOLocations();
    });
  }

  /**
   * Load QSO locations from API
   */
  private loadQSOLocations(): void {
    if (!this.logId) {
      console.warn('No logId provided to QSO map');
      return;
    }

    this.isLoading = true;

    const sub = this.mapService.getQSOLocations(this.logId, this.currentZoom, this.filters)
      .subscribe({
        next: (response) => {
          this.totalQSOs = response.totalQSOs;
          this.displayedPoints = response.displayedPoints;
          this.clustered = response.clustered;

          this.clearMarkers();

          if (response.clustered) {
            this.renderClusters(response.clusters);
          } else {
            this.renderMarkers(response.markers);
          }

          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading QSO locations:', error);
          this.isLoading = false;
        }
      });

    this.subscriptions.push(sub);
  }

  /**
   * Render individual QSO markers
   */
  private renderMarkers(markers: MapMarker[]): void {
    markers.forEach(marker => {
      const leafletMarker = L.marker([marker.lat, marker.lon], {
        icon: this.createQSOIcon(marker)
      });

      // Create popup content
      const popupContent = this.createQSOPopup(marker);
      leafletMarker.bindPopup(popupContent);

      leafletMarker.addTo(this.map);
      this.markers.push(leafletMarker);
    });
  }

  /**
   * Render cluster markers
   */
  private renderClusters(clusters: MapCluster[]): void {
    clusters.forEach(cluster => {
      const leafletMarker = L.marker([cluster.lat, cluster.lon], {
        icon: this.createClusterIcon(cluster)
      });

      // Create popup content
      const popupContent = this.createClusterPopup(cluster);
      leafletMarker.bindPopup(popupContent);

      // Click to zoom to cluster bounds
      leafletMarker.on('click', () => {
        const bounds = L.latLngBounds(
          [cluster.bounds.minLat, cluster.bounds.minLon],
          [cluster.bounds.maxLat, cluster.bounds.maxLon]
        );
        this.map.fitBounds(bounds, { padding: [50, 50] });
      });

      leafletMarker.addTo(this.map);
      this.clusterMarkers.push(leafletMarker);
    });
  }

  /**
   * Create icon for individual QSO marker
   */
  private createQSOIcon(marker: MapMarker): L.DivIcon {
    const color = this.getStationColor(marker.station);
    const isRecent = this.isRecentQSO(marker.timestamp);
    const recentClass = isRecent ? ' recent' : '';

    return L.divIcon({
      html: `<div class="qso-marker${recentClass}" style="background-color: ${color};">
               <div class="qso-marker-inner"></div>
             </div>`,
      className: 'qso-marker-container',
      iconSize: [24, 24],
      iconAnchor: [12, 12],
      popupAnchor: [0, -12]
    });
  }

  /**
   * Create icon for cluster marker
   */
  private createClusterIcon(cluster: MapCluster): L.DivIcon {
    const size = this.getClusterSize(cluster.count);

    // Use pie chart if multiple stations
    if (cluster.stationBreakdown && Object.keys(cluster.stationBreakdown).length > 1) {
      const pieChartSVG = this.createPieChartSVG(cluster.stationBreakdown, size);
      return L.divIcon({
        html: `<div class="cluster-marker pie-chart-marker" style="width: ${size}px; height: ${size}px;">
                 ${pieChartSVG}
                 <span class="cluster-count">${cluster.count}</span>
               </div>`,
        className: 'cluster-marker-container',
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2],
        popupAnchor: [0, -(size / 2)]
      });
    }

    // Default solid color cluster
    return L.divIcon({
      html: `<div class="cluster-marker" style="width: ${size}px; height: ${size}px;">
               <span class="cluster-count">${cluster.count}</span>
             </div>`,
      className: 'cluster-marker-container',
      iconSize: [size, size],
      iconAnchor: [size / 2, size / 2],
      popupAnchor: [0, -(size / 2)]
    });
  }

  /**
   * Create popup content for QSO marker
   */
  private createQSOPopup(marker: MapMarker): string {
    let html = `
      <div class="qso-popup">
        <h6>${marker.callsign}</h6>
        <table class="table table-sm">
          <tr><td><strong>Grid:</strong></td><td>${marker.grid || 'N/A'}</td></tr>
    `;

    if (marker.band) {
      html += `<tr><td><strong>Band:</strong></td><td>${marker.band}</td></tr>`;
    }

    if (marker.mode) {
      html += `<tr><td><strong>Mode:</strong></td><td>${marker.mode}</td></tr>`;
    }

    if (marker.distance) {
      html += `<tr><td><strong>Distance:</strong></td><td>${marker.distance.toFixed(0)} km</td></tr>`;
    }

    if (marker.bearing) {
      html += `<tr><td><strong>Bearing:</strong></td><td>${marker.bearing.toFixed(0)}°</td></tr>`;
    }

    if (marker.station) {
      html += `<tr><td><strong>Station:</strong></td><td>${marker.station}</td></tr>`;
    }

    html += `
        </table>
      </div>
    `;

    return html;
  }

  /**
   * Create popup content for cluster marker
   */
  private createClusterPopup(cluster: MapCluster): string {
    let html = `
      <div class="cluster-popup">
        <h6>Cluster: ${cluster.count} QSOs</h6>
        <p class="text-muted">Click to zoom in</p>
    `;

    if (cluster.stationBreakdown) {
      html += `<p><strong>By Station:</strong></p><ul class="list-unstyled">`;
      for (const [station, count] of Object.entries(cluster.stationBreakdown)) {
        html += `<li>Station ${station}: ${count}</li>`;
      }
      html += `</ul>`;
    }

    if (cluster.bandBreakdown) {
      html += `<p><strong>By Band:</strong></p><ul class="list-unstyled">`;
      for (const [band, count] of Object.entries(cluster.bandBreakdown)) {
        html += `<li>${band}: ${count}</li>`;
      }
      html += `</ul>`;
    }

    html += `</div>`;

    return html;
  }

  /**
   * Clear all markers from map
   */
  private clearMarkers(): void {
    this.markers.forEach(marker => marker.remove());
    this.markers = [];

    this.clusterMarkers.forEach(marker => marker.remove());
    this.clusterMarkers = [];
  }

  /**
   * Get color for station
   */
  private getStationColor(station?: number): string {
    if (!station) return '#0080ff'; // Default blue

    // EtherWave theme colors
    const colors = [
      '#0080ff', // Blue
      '#00d4ff', // Cyan
      '#00ff88', // Green
      '#ffaa00', // Orange
      '#ff0088', // Pink
      '#aa00ff', // Purple
    ];

    return colors[(station - 1) % colors.length];
  }

  /**
   * Create SVG pie chart for cluster with multiple stations
   */
  private createPieChartSVG(stationBreakdown: { [key: string]: number }, size: number): string {
    const total = Object.values(stationBreakdown).reduce((sum, count) => sum + count, 0);
    const radius = size / 2;
    const center = size / 2;

    let currentAngle = -90; // Start at top
    let paths = '';

    // Sort stations by count (descending) for consistent order
    const sortedStations = Object.entries(stationBreakdown)
      .sort(([, a], [, b]) => b - a);

    sortedStations.forEach(([station, count]) => {
      const percentage = count / total;
      const angle = percentage * 360;

      if (percentage === 1) {
        // Full circle for single station (shouldn't happen, but just in case)
        const color = this.getStationColor(parseInt(station));
        paths += `<circle cx="${center}" cy="${center}" r="${radius}" fill="${color}" />`;
      } else {
        // Calculate arc path
        const startAngle = currentAngle;
        const endAngle = currentAngle + angle;

        const startRad = (startAngle * Math.PI) / 180;
        const endRad = (endAngle * Math.PI) / 180;

        const x1 = center + radius * Math.cos(startRad);
        const y1 = center + radius * Math.sin(startRad);
        const x2 = center + radius * Math.cos(endRad);
        const y2 = center + radius * Math.sin(endRad);

        const largeArc = angle > 180 ? 1 : 0;

        const color = this.getStationColor(parseInt(station));
        paths += `<path d="M ${center},${center} L ${x1},${y1} A ${radius},${radius} 0 ${largeArc},1 ${x2},${y2} Z" fill="${color}" />`;

        currentAngle += angle;
      }
    });

    return `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" class="pie-chart-svg">
              ${paths}
            </svg>`;
  }

  /**
   * Calculate cluster marker size based on count
   */
  private getClusterSize(count: number): number {
    if (count < 10) return 30;
    if (count < 50) return 40;
    if (count < 100) return 50;
    if (count < 500) return 60;
    return 70;
  }

  /**
   * Check if QSO is recent (within last 15 minutes)
   */
  private isRecentQSO(timestamp?: string): boolean {
    if (!timestamp) return false;

    const qsoTime = new Date(timestamp).getTime();
    const now = Date.now();
    const fifteenMinutes = 15 * 60 * 1000; // 15 minutes in milliseconds

    return (now - qsoTime) <= fifteenMinutes;
  }

  /**
   * Switch between light and dark theme
   */
  switchTheme(darkMode: boolean): void {
    this.darkMode = darkMode;

    if (this.currentTileLayer) {
      this.map.removeLayer(this.currentTileLayer);
    }

    this.currentTileLayer = darkMode ? this.darkTileLayer : this.lightTileLayer;
    this.currentTileLayer.addTo(this.map);
  }

  /**
   * Apply new filters and reload
   */
  applyFilters(filters: MapFilters): void {
    this.filters = filters;
    this.loadQSOLocations();
  }

  /**
   * Refresh map data
   */
  refresh(): void {
    this.loadQSOLocations();
  }

  /**
   * Center map on coordinates
   */
  centerOn(lat: number, lon: number, zoom?: number): void {
    this.map.setView([lat, lon], zoom || this.currentZoom);
  }

  /**
   * Fit map to bounds
   */
  fitBounds(bounds: L.LatLngBoundsExpression): void {
    this.map.fitBounds(bounds, { padding: [50, 50] });
  }

  /**
   * Toggle grid overlay visibility
   */
  async toggleGridOverlay(): Promise<void> {
    if (this.gridOverlayVisible) {
      // Hide grid overlay
      if (this.gridOverlayLayer) {
        this.gridOverlayService.clearGridOverlays(this.map);
        this.gridOverlayLayer = undefined;
      }
      this.gridOverlayVisible = false;
    } else {
      // Show grid overlay
      if (!this.logId) {
        console.warn('No logId provided for grid overlay');
        return;
      }

      try {
        this.gridOverlayLayer = await this.gridOverlayService.addGridOverlay(
          this.map,
          this.logId,
          this.gridPrecision,
          true // include neighbors
        );
        this.gridOverlayVisible = true;
      } catch (error) {
        console.error('Error loading grid overlay:', error);
      }
    }
  }

  /**
   * Change grid precision and reload overlay if visible
   */
  async setGridPrecision(precision: number): Promise<void> {
    this.gridPrecision = precision;

    if (this.gridOverlayVisible) {
      // Reload overlay with new precision
      if (this.gridOverlayLayer) {
        this.gridOverlayService.clearGridOverlays(this.map);
        this.gridOverlayLayer = undefined;
      }

      try {
        this.gridOverlayLayer = await this.gridOverlayService.addGridOverlay(
          this.map,
          this.logId,
          this.gridPrecision,
          true
        );
      } catch (error) {
        console.error('Error reloading grid overlay:', error);
      }
    }
  }

  /**
   * Toggle heatmap overlay visibility
   */
  async toggleHeatmap(): Promise<void> {
    if (this.heatmapVisible) {
      // Hide heatmap
      if (this.heatmapLayer) {
        this.heatmapService.clearHeatmaps(this.map);
        this.heatmapLayer = undefined;
      }
      this.heatmapVisible = false;
    } else {
      // Show heatmap
      if (!this.logId) {
        console.warn('No logId provided for heatmap');
        return;
      }

      try {
        this.heatmapLayer = await this.heatmapService.addHeatmap(
          this.map,
          this.logId,
          this.filters,
          {
            radius: this.heatmapRadius,
            blur: 15,
            maxZoom: 17
          }
        );
        this.heatmapVisible = true;
      } catch (error) {
        console.error('Error loading heatmap:', error);
      }
    }
  }

  /**
   * Change heatmap radius and reload if visible
   */
  async setHeatmapRadius(radius: number): Promise<void> {
    this.heatmapRadius = radius;

    if (this.heatmapVisible && this.heatmapLayer) {
      // Update heatmap options
      this.heatmapService.updateHeatmapOptions(this.heatmapLayer, {
        radius: this.heatmapRadius
      });
    }
  }
}
