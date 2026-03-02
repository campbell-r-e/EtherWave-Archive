import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

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
 * Colors are persisted to the backend and cached in localStorage as a fallback.
 *
 * Backend API:
 * - GET  /api/user/station-colors  — load saved colors (204 = use defaults)
 * - PUT  /api/user/station-colors  — save colors
 * - DELETE /api/user/station-colors — reset to defaults
 */
@Injectable({
  providedIn: 'root'
})
export class StationColorPreferencesService {
  private readonly STORAGE_KEY = 'ew_station_colors';
  private readonly apiUrl = `${environment.apiUrl}/user/station-colors`;

  private colorsSubject = new BehaviorSubject<StationColorConfig>(
    this.loadFromLocalStorage()
  );

  public colors$: Observable<StationColorConfig> = this.colorsSubject.asObservable();

  constructor(private http: HttpClient) {
    // Attempt to sync from backend; localStorage cache is already active
    this.loadFromBackend();
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
    this.colorsSubject.next({ ...DEFAULT_STATION_COLORS });
    this.saveToLocalStorage({ ...DEFAULT_STATION_COLORS });
    this.http.delete(this.apiUrl).subscribe({
      error: () => { /* localStorage already reset — backend failure is non-fatal */ }
    });
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
   * Update colors, persist to localStorage and backend
   */
  private updateColors(colors: StationColorConfig): void {
    this.colorsSubject.next(colors);
    this.saveToLocalStorage(colors);
    this.saveToBackend(colors);
  }

  /**
   * Load colors from localStorage
   */
  private loadFromLocalStorage(): StationColorConfig {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
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
   * Load colors from backend; update subject and localStorage on success.
   * 204 No Content means no custom colors saved — keep current defaults.
   */
  private loadFromBackend(): void {
    this.http.get(this.apiUrl, { responseType: 'text', observe: 'response' }).subscribe({
      next: (response) => {
        if (response.status === 200 && response.body) {
          try {
            const parsed = JSON.parse(response.body);
            const merged: StationColorConfig = { ...DEFAULT_STATION_COLORS, ...parsed };
            this.colorsSubject.next(merged);
            this.saveToLocalStorage(merged);
          } catch {
            // Corrupt JSON from backend — keep localStorage value
          }
        }
        // 204 — no custom preferences saved; keep whatever is in localStorage/defaults
      },
      error: () => {
        // Backend unavailable — localStorage fallback already active
      }
    });
  }

  /**
   * Persist colors to backend (fire-and-forget; localStorage is the source of truth on failure)
   */
  private saveToBackend(colors: StationColorConfig): void {
    this.http.put(this.apiUrl, colors).subscribe({
      error: () => { /* Non-fatal — localStorage is already updated */ }
    });
  }
}
