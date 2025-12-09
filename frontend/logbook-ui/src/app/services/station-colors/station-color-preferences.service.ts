import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface StationColorConfig {
  station1: string;
  station2: string;
  station3: string;
  station4: string;
  station5: string;
  station6: string;
  gota: string;
}

export const DEFAULT_STATION_COLORS: StationColorConfig = {
  station1: '#0080ff', // Blue
  station2: '#00d4ff', // Cyan
  station3: '#00ff88', // Green
  station4: '#ffaa00', // Orange
  station5: '#ff0088', // Pink
  station6: '#aa00ff', // Purple
  gota: '#00ff88'      // Green (GOTA - Get On The Air)
};

/**
 * Station Color Preferences Service
 *
 * Manages user-customizable colors for stations 1-6 and GOTA.
 * Colors are used throughout the application for:
 * - Map markers and clusters
 * - Station badges
 * - Charts and visualizations
 * - Legend indicators
 *
 * Backend API Endpoints Required:
 * - GET /api/user/station-colors - Load user's custom colors
 * - PUT /api/user/station-colors - Save user's custom colors
 * - DELETE /api/user/station-colors - Reset to defaults
 */
@Injectable({
  providedIn: 'root'
})
export class StationColorPreferencesService {
  private readonly STORAGE_KEY = 'ew_station_colors';

  private colorsSubject = new BehaviorSubject<StationColorConfig>(
    this.loadFromLocalStorage()
  );

  public colors$: Observable<StationColorConfig> = this.colorsSubject.asObservable();

  constructor() {
    // Load colors from localStorage on initialization
    const stored = this.loadFromLocalStorage();
    this.colorsSubject.next(stored);
  }

  /**
   * Get current color configuration
   */
  getColors(): StationColorConfig {
    return this.colorsSubject.value;
  }

  /**
   * Get color for a specific station
   */
  getStationColor(station: number | 'gota'): string {
    const colors = this.getColors();

    if (station === 'gota') {
      return colors.gota;
    }

    const key = `station${station}` as keyof StationColorConfig;
    return colors[key] || DEFAULT_STATION_COLORS[key];
  }

  /**
   * Set color for a specific station
   */
  setStationColor(station: number | 'gota', color: string): void {
    const colors = { ...this.getColors() };

    if (station === 'gota') {
      colors.gota = color;
    } else {
      const key = `station${station}` as keyof StationColorConfig;
      colors[key] = color;
    }

    this.updateColors(colors);
  }

  /**
   * Update all colors at once
   */
  setColors(colors: Partial<StationColorConfig>): void {
    const updated = { ...this.getColors(), ...colors };
    this.updateColors(updated);
  }

  /**
   * Reset to default colors
   */
  resetToDefaults(): void {
    this.updateColors({ ...DEFAULT_STATION_COLORS });
  }

  /**
   * Check if colors have been customized
   */
  isCustomized(): boolean {
    const current = this.getColors();
    return Object.keys(DEFAULT_STATION_COLORS).some(key => {
      const k = key as keyof StationColorConfig;
      return current[k] !== DEFAULT_STATION_COLORS[k];
    });
  }

  /**
   * Get all station numbers with their colors
   */
  getStationColorMap(): Map<number | 'gota', string> {
    const colors = this.getColors();
    const map = new Map<number | 'gota', string>();

    for (let i = 1; i <= 6; i++) {
      map.set(i, this.getStationColor(i));
    }
    map.set('gota', colors.gota);

    return map;
  }

  /**
   * Get color with optional opacity
   */
  getStationColorWithOpacity(station: number | 'gota', opacity: number = 1): string {
    const color = this.getStationColor(station);
    return this.hexToRgba(color, opacity);
  }

  /**
   * Convert hex color to rgba
   */
  private hexToRgba(hex: string, alpha: number): string {
    const r = parseInt(hex.slice(1, 3), 16);
    const g = parseInt(hex.slice(3, 5), 16);
    const b = parseInt(hex.slice(5, 7), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }

  /**
   * Update colors and persist
   */
  private updateColors(colors: StationColorConfig): void {
    this.colorsSubject.next(colors);
    this.saveToLocalStorage(colors);
    // TODO: Save to backend API
    // this.http.put('/api/user/station-colors', colors).subscribe();
  }

  /**
   * Load colors from localStorage
   */
  private loadFromLocalStorage(): StationColorConfig {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        // Merge with defaults to ensure all keys exist
        return { ...DEFAULT_STATION_COLORS, ...parsed };
      }
    } catch (error) {
      console.error('Error loading station colors from localStorage:', error);
    }

    return { ...DEFAULT_STATION_COLORS };
  }

  /**
   * Save colors to localStorage
   */
  private saveToLocalStorage(colors: StationColorConfig): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(colors));
    } catch (error) {
      console.error('Error saving station colors to localStorage:', error);
    }
  }

  /**
   * Load colors from backend API
   * TODO: Implement when backend endpoint is ready
   */
  async loadFromBackend(): Promise<void> {
    // const colors = await this.http.get<StationColorConfig>('/api/user/station-colors').toPromise();
    // if (colors) {
    //   this.updateColors(colors);
    // }
  }

  /**
   * Save colors to backend API
   * TODO: Implement when backend endpoint is ready
   */
  async saveToBackend(): Promise<void> {
    // const colors = this.getColors();
    // await this.http.put('/api/user/station-colors', colors).toPromise();
  }
}
