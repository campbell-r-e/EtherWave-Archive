export interface Station {
  id?: number;
  stationName: string;
  callsign: string;
  location?: string;
  gridSquare?: string;
  antenna?: string;
  powerWatts?: number;
  rigModel?: string;
  rigControlEnabled?: boolean;
  rigControlHost?: string;
  rigControlPort?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface Contest {
  id?: number;
  contestCode: string;
  contestName: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  isActive?: boolean;
  validatorClass?: string;
  rulesConfig?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Operator {
  id?: number;
  callsign: string;
  name?: string;
  email?: string;
  licenseClass?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CallsignInfo {
  callsign: string;
  name?: string;
  address?: string;
  state?: string;
  country?: string;
  licenseClass?: string;
  gridSquare?: string;
  lookupSource?: string;
  cached?: boolean;
}

export interface RigStatus {
  frequencyHz?: number;
  mode?: string;
  bandwidth?: string;
  pttActive?: boolean;
  sMeter?: number;
  powerMeter?: number;
  swr?: number;
  connected?: boolean;
  timestamp?: string;
  error?: string;
}
