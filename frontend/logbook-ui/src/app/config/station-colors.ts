/**
 * EtherWave Archive - Station Color Configuration
 *
 * Centralized station color mapping ensures consistency across
 * all components (tables, charts, maps, badges, etc.)
 *
 * Now integrates with StationColorPreferencesService for user customization.
 */

import { DEFAULT_STATION_COLORS } from '../services/station-colors/station-color-preferences.service';

export interface StationColorConfig {
  primary: string;
  light: string;
  dark: string;
  contrast: string;
}

/**
 * Get user-customizable station colors
 * This function now uses the StationColorPreferencesService
 */
function getUserStationColors(): { [key: string]: string } {
  // In a real scenario, this would inject the service
  // For now, we'll use localStorage directly for backward compatibility
  try {
    const stored = localStorage.getItem('ew_station_colors');
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Error loading station colors:', error);
  }
  return { ...DEFAULT_STATION_COLORS };
}

/**
 * Generate color variants (light/dark) from a primary color
 */
function generateColorVariants(primary: string): StationColorConfig {
  return {
    primary,
    light: adjustBrightness(primary, 30),
    dark: adjustBrightness(primary, -30),
    contrast: '#FFFFFF'
  };
}

/**
 * Adjust color brightness
 */
function adjustBrightness(hex: string, percent: number): string {
  const num = parseInt(hex.replace('#', ''), 16);
  const amt = Math.round(2.55 * percent);
  const R = Math.min(255, Math.max(0, (num >> 16) + amt));
  const G = Math.min(255, Math.max(0, (num >> 8 & 0x00FF) + amt));
  const B = Math.min(255, Math.max(0, (num & 0x0000FF) + amt));
  return '#' + (0x1000000 + R * 0x10000 + G * 0x100 + B).toString(16).slice(1).toUpperCase();
}

/**
 * Get station colors with user customization support
 */
export function getStationColors(): { [key: string]: StationColorConfig } {
  const userColors = getUserStationColors();

  return {
    '1': generateColorVariants(userColors['station1'] || DEFAULT_STATION_COLORS.station1),
    '2': generateColorVariants(userColors['station2'] || DEFAULT_STATION_COLORS.station2),
    '3': generateColorVariants(userColors['station3'] || DEFAULT_STATION_COLORS.station3),
    '4': generateColorVariants(userColors['station4'] || DEFAULT_STATION_COLORS.station4),
    '5': generateColorVariants(userColors['station5'] || DEFAULT_STATION_COLORS.station5),
    '6': generateColorVariants(userColors['station6'] || DEFAULT_STATION_COLORS.station6),
    'gota': generateColorVariants(userColors['gota'] || DEFAULT_STATION_COLORS.gota),
    'viewer': {
      primary: '#9E9E9E',
      light: '#BDBDBD',
      dark: '#616161',
      contrast: '#FFFFFF'
    },
    'default': {
      primary: '#757575',
      light: '#BDBDBD',
      dark: '#424242',
      contrast: '#FFFFFF'
    }
  };
}

// Export dynamic station colors
export const STATION_COLORS = getStationColors();

/**
 * Get station color by station number or type
 * @param stationNumber - Station number (1, 2) or type ('gota', 'viewer')
 * @param variant - Color variant ('primary', 'light', 'dark', 'contrast')
 * @returns Hex color code
 */
export function getStationColor(
  stationNumber: number | string | null | undefined,
  variant: 'primary' | 'light' | 'dark' | 'contrast' = 'primary'
): string {
  if (stationNumber === null || stationNumber === undefined) {
    return STATION_COLORS['default'][variant];
  }

  const key = String(stationNumber).toLowerCase();
  const config = STATION_COLORS[key] || STATION_COLORS['default'];
  return config[variant];
}

/**
 * Get station color for GOTA status
 * @param isGota - Whether station is GOTA
 * @param stationNumber - Fallback station number if not GOTA
 * @param variant - Color variant
 * @returns Hex color code
 */
export function getStationColorByGotaStatus(
  isGota: boolean,
  stationNumber: number | null | undefined,
  variant: 'primary' | 'light' | 'dark' | 'contrast' = 'primary'
): string {
  if (isGota) {
    return STATION_COLORS['gota'][variant];
  }
  return getStationColor(stationNumber, variant);
}

/**
 * Get all station colors as CSS variables
 * @returns Object with CSS variable names and values
 */
export function getStationCSSVariables(): { [key: string]: string } {
  const variables: { [key: string]: string } = {};

  Object.entries(STATION_COLORS).forEach(([key, config]) => {
    variables[`--station-${key}-primary`] = config.primary;
    variables[`--station-${key}-light`] = config.light;
    variables[`--station-${key}-dark`] = config.dark;
    variables[`--station-${key}-contrast`] = config.contrast;
  });

  return variables;
}

/**
 * Brand color constants for GEKHoosier QSO Suite
 */
export const BRAND_COLORS = {
  // Primary Theme
  hoosierBlue: '#003F87',
  cardinalRed: '#C41E3A',
  qsoGreen: '#4CAF50',

  // Backgrounds
  darkBg: '#1A1A1A',
  lightBg: '#F2F5F7',

  // Accents
  highlightYellow: '#F5C542',
  aiHighlight: '#7E57C2',
  rigOnline: '#00E5FF',
  validationError: '#FF7043'
} as const;
