export interface QSO {
  id?: number;
  stationId: number;
  stationName?: string;
  operatorId?: number;
  operatorCallsign?: string;
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
  county?: string;
  state?: string;
  country?: string;
  dxcc?: number;
  cqZone?: number;
  ituZone?: number;

  // Operator info
  name?: string;
  licenseClass?: string;

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
  contestData?: string;
  qslSent?: string;
  qslRcvd?: string;
  lotwSent?: string;
  lotwRcvd?: string;
  notes?: string;
}
