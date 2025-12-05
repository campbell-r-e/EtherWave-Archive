/**
 * GEKHoosier QSO Suite - Station Color Configuration
 *
 * Centralized station color mapping ensures consistency across
 * all components (tables, charts, maps, badges, etc.)
 */

export interface StationColorConfig {
  primary: string;
  light: string;
  dark: string;
  contrast: string;
}

export const STATION_COLORS: { [key: string]: StationColorConfig } = {
  '1': {
    primary: '#1E88E5',    // Station 1 - Blue
    light: '#64B5F6',
    dark: '#1565C0',
    contrast: '#FFFFFF'
  },
  '2': {
    primary: '#E53935',    // Station 2 - Red
    light: '#EF5350',
    dark: '#C62828',
    contrast: '#FFFFFF'
  },
  'gota': {
    primary: '#43A047',    // GOTA - Green
    light: '#66BB6A',
    dark: '#2E7D32',
    contrast: '#FFFFFF'
  },
  'viewer': {
    primary: '#9E9E9E',    // Viewer - Gray
    light: '#BDBDBD',
    dark: '#616161',
    contrast: '#FFFFFF'
  },
  'default': {
    primary: '#757575',    // Default/Unknown
    light: '#BDBDBD',
    dark: '#424242',
    contrast: '#FFFFFF'
  }
};

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
