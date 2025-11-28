export interface User {
  id: number;
  username: string;
  email: string;
  callsign?: string;
  fullName?: string;
  gridSquare?: string;
  roles: UserRole[];
  enabled: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

export enum UserRole {
  ROLE_USER = 'ROLE_USER',
  ROLE_ADMIN = 'ROLE_ADMIN',
  ROLE_CREATOR = 'ROLE_CREATOR',
  ROLE_OPERATOR = 'ROLE_OPERATOR'
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName?: string;
  callsign?: string;
  gridSquare?: string;
  qrzApiKey?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  userId: number;
  username: string;
  email: string;
  callsign?: string;
  fullName?: string;
  roles: UserRole[];
}
