import { Injectable } from '@angular/core';
import { Observable, combineLatest, map } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { LogService } from '../log/log.service';
import { User } from '../../models/auth/user.model';
import { Log, ParticipantRole } from '../../models/log.model';

export interface UserPermissions {
  // Log-level permissions
  canEditLog: boolean;
  canDeleteLog: boolean;
  canInviteUsers: boolean;
  canManageStations: boolean;
  canManageContests: boolean;

  // QSO permissions
  canCreateQSO: boolean;
  canEditQSO: boolean;
  canDeleteQSO: boolean;

  // Contest permissions
  canViewContestOverlays: boolean;
  canManageContestData: boolean;

  // Map permissions
  canExportMapData: boolean;

  // Import/Export permissions
  canImportData: boolean;
  canExportData: boolean;

  // Role information
  isLogCreator: boolean;
  isStationOperator: boolean;
  isViewer: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class PermissionsService {

  constructor(
    private authService: AuthService,
    private logService: LogService
  ) {}

  /**
   * Get current user's permissions for the current log
   */
  getCurrentPermissions(): Observable<UserPermissions> {
    return combineLatest([
      this.authService.currentUser,
      this.logService.currentLog$
    ]).pipe(
      map(([user, log]) => this.calculatePermissions(user, log))
    );
  }

  /**
   * Calculate permissions based on user and log
   */
  private calculatePermissions(user: User | null, log: Log | null): UserPermissions {
    // Default permissions (no access)
    const defaultPermissions: UserPermissions = {
      canEditLog: false,
      canDeleteLog: false,
      canInviteUsers: false,
      canManageStations: false,
      canManageContests: false,
      canCreateQSO: false,
      canEditQSO: false,
      canDeleteQSO: false,
      canViewContestOverlays: false,
      canManageContestData: false,
      canExportMapData: false,
      canImportData: false,
      canExportData: false,
      isLogCreator: false,
      isStationOperator: false,
      isViewer: false
    };

    if (!user || !log) {
      return defaultPermissions;
    }

    // Determine role from participant data returned by the API (log.userRole)
    const userRole = log.userRole;
    const isCreator = log.creatorId === user.id || userRole === ParticipantRole.CREATOR;
    const isOperator = userRole === ParticipantRole.STATION;

    // Calculate permissions based on role
    if (isCreator) {
      // Log creator has full permissions
      return {
        canEditLog: true,
        canDeleteLog: true,
        canInviteUsers: true,
        canManageStations: true,
        canManageContests: true,
        canCreateQSO: true,
        canEditQSO: true,
        canDeleteQSO: true,
        canViewContestOverlays: true,
        canManageContestData: true,
        canExportMapData: true,
        canImportData: true,
        canExportData: true,
        isLogCreator: true,
        isStationOperator: true,
        isViewer: false
      };
    } else if (isOperator) {
      // Station operator can log QSOs and view contest data
      return {
        canEditLog: false,
        canDeleteLog: false,
        canInviteUsers: false,
        canManageStations: false,
        canManageContests: false,
        canCreateQSO: true,
        canEditQSO: true,
        canDeleteQSO: true,
        canViewContestOverlays: true,
        canManageContestData: false,
        canExportMapData: true,
        canImportData: false,
        canExportData: true,
        isLogCreator: false,
        isStationOperator: true,
        isViewer: false
      };
    } else {
      // Viewer can only view data
      return {
        canEditLog: false,
        canDeleteLog: false,
        canInviteUsers: false,
        canManageStations: false,
        canManageContests: false,
        canCreateQSO: false,
        canEditQSO: false,
        canDeleteQSO: false,
        canViewContestOverlays: false,
        canManageContestData: false,
        canExportMapData: true,
        canImportData: false,
        canExportData: true,
        isLogCreator: false,
        isStationOperator: false,
        isViewer: true
      };
    }
  }

  /**
   * Check if user can perform a specific action
   */
  canPerformAction(action: keyof UserPermissions): Observable<boolean> {
    return this.getCurrentPermissions().pipe(
      map(permissions => permissions[action] as boolean)
    );
  }

  /**
   * Check if user is log creator
   */
  isLogCreator(): Observable<boolean> {
    return this.canPerformAction('isLogCreator');
  }

  /**
   * Check if user is station operator
   */
  isStationOperator(): Observable<boolean> {
    return this.canPerformAction('isStationOperator');
  }

  /**
   * Check if user can view contest features
   */
  canViewContestFeatures(): Observable<boolean> {
    return this.canPerformAction('canViewContestOverlays');
  }
}
