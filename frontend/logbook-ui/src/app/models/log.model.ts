export enum LogType {
  PERSONAL = 'PERSONAL',
  SHARED = 'SHARED'
}

export enum ParticipantRole {
  CREATOR = 'CREATOR',
  STATION = 'STATION',
  VIEWER = 'VIEWER'
}

export enum InvitationStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED'
}

export interface Log {
  id: number;
  name: string;
  description?: string;
  type: LogType;
  creatorId: number;
  creatorUsername: string;
  contestId?: number;
  contestName?: string;
  startDate?: string;
  endDate?: string;
  active: boolean;
  editable: boolean;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
  userRole?: ParticipantRole;
  participantCount: number;
  qsoCount: number;

  // Contest scoring fields
  totalQsos?: number;
  totalPoints?: number;
  totalMultipliers?: number;
  calculatedScore?: number;
  lastScoreCalculation?: string;
}

export interface LogRequest {
  name: string;
  description?: string;
  type: LogType;
  contestId?: number;
  startDate?: string;
  endDate?: string;
  isPublic?: boolean;
}

export interface LogParticipant {
  id: number;
  logId: number;
  logName: string;
  userId: number;
  username: string;
  userCallsign?: string;
  role: ParticipantRole;
  stationCallsign?: string;
  stationNumber?: number;  // 1-1000, null if unassigned
  isGota: boolean;  // Get On The Air station designation
  active: boolean;
  joinedAt: string;
}

export interface Invitation {
  id: number;
  logId: number;
  logName: string;
  inviterId: number;
  inviterUsername: string;
  inviteeId: number;
  inviteeUsername: string;
  inviteeEmail?: string;
  inviteeCallsign?: string;
  proposedRole: ParticipantRole;
  stationCallsign?: string;
  status: InvitationStatus;
  message?: string;
  createdAt: string;
  expiresAt?: string;
  canRespond: boolean;
}

export interface InvitationRequest {
  logId: number;
  inviteeUsername: string;
  proposedRole: ParticipantRole;
  stationCallsign?: string;
  message?: string;
  expiresAt?: string;
}

export interface StationAssignmentRequest {
  stationNumber?: number;  // 1-1000, null to unassign
  isGota?: boolean;  // defaults to false if null
}
