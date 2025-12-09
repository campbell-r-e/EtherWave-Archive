export interface QSO {
  id?: number;
  logId?: number;
  stationId: number;
  stationName?: string;
  operatorId?: number;
  operatorCallsign?: string;
  operator?: string;
  contestId?: number;
  contestCode?: string;

  callsign: string;
  frequencyKhz: number;
  mode: string;
  qsoDate: string;
  timeOn: string;
  timeOff?: string;

  rstSent?: string;
  rstRcvd?: string;
  band?: string;
  powerWatts?: number;

  // Location
  gridSquare?: string;
  latitude?: number;
  longitude?: number;
  county?: string;
  state?: string;
  country?: string;
  dxcc?: number;
  cqZone?: number;
  ituZone?: number;

  // Operator info
  name?: string;
  licenseClass?: string;

  // Multi-station contest support
  stationNumber?: number; // 1-1000, null if unassigned
  isGota?: boolean;       // Get On The Air station

  // Contest data (JSON string)
  contestData?: string;

  // QSL
  qslSent?: string;
  qslRcvd?: string;
  lotwSent?: string;
  lotwRcvd?: string;

  // Validation
  isValid?: boolean;
  validationErrors?: string;

  // Scoring (calculated by backend)
  points?: number;
  isDuplicate?: boolean;
  isMultiplier?: boolean;
  multiplierTypes?: string;

  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface QSORequest {
  stationId: number;
  operatorId?: number;
  contestId?: number;
  callsign: string;
  frequencyKhz: number;
  mode: string;
  qsoDate: string;
  timeOn: string;
  timeOff?: string;
  rstSent?: string;
  rstRcvd?: string;
  band?: string;
  powerWatts?: number;
  gridSquare?: string;
  county?: string;
  state?: string;
  country?: string;
  dxcc?: number;
  cqZone?: number;
  ituZone?: number;
  name?: string;
  licenseClass?: string;
  stationNumber?: number; // 1-1000, null if unassigned
  isGota?: boolean;       // Get On The Air station
  contestData?: string;
  qslSent?: string;
  qslRcvd?: string;
  lotwSent?: string;
  lotwRcvd?: string;
  notes?: string;
}
