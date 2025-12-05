import { QSO } from './qso.model';

/**
 * Station statistics for multi-station contest operations
 * Provides comprehensive performance metrics per station or GOTA
 */
export interface StationStatistics {
  stationNumber?: number;           // Station number (1-1000), null if GOTA or summary
  isGota: boolean;                   // Whether this is GOTA station
  stationLabel: string;              // Display label (e.g., "Station 1", "GOTA")
  qsoCount: number;                  // Total QSOs for this station
  points: number;                    // Total points scored
  qsoRate: number;                   // QSOs per hour
  bandBreakdown: { [band: string]: number };  // QSOs per band
  modeBreakdown: { [mode: string]: number };  // QSOs per mode
  recentQSOs: QSO[];                // Recent contacts (last 5-10)
  rank?: number;                     // Leaderboard position (1st, 2nd, 3rd)
  operators?: string[];              // Operator usernames assigned to this station
}

/**
 * Overall multi-station contest summary
 * Separates main contest stations from GOTA for scoring compliance
 */
export interface StationStatsSummary {
  mainStations: StationStatistics[];  // All main contest stations (non-GOTA)
  gota?: StationStatistics;           // GOTA station stats (if present)
  mainTotal: MainTotal;               // Combined totals for main stations only
  overallTotal: OverallTotal;         // Overall totals including GOTA
}

/**
 * Total for main contest stations (GOTA excluded per contest rules)
 */
export interface MainTotal {
  qsoCount: number;
  points: number;
  qsoRate: number;
}

/**
 * Overall total including GOTA (informational only)
 */
export interface OverallTotal {
  qsoCount: number;
}
