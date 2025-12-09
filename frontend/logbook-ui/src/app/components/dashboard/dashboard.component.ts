import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';
import { LogService } from '../../services/log/log.service';
import { ThemeService } from '../../services/theme/theme.service';
import { PermissionsService, UserPermissions } from '../../services/permissions/permissions.service';
import { User } from '../../models/auth/user.model';
import { Log, LogParticipant } from '../../models/log.model';
import { getStationColor } from '../../config/station-colors';

// Import all logbook components
import { LogSelectorComponent } from '../log/log-selector/log-selector.component';
import { QsoEntryComponent } from '../qso-entry/qso-entry.component';
import { QsoListComponent } from '../qso-list/qso-list.component';
import { RigStatusComponent } from '../rig-status/rig-status.component';
import { ContestSelectionComponent } from '../contest-selection/contest-selection.component';
import { StationManagementComponent } from '../station-management/station-management.component';
import { ParticipantManagementComponent } from '../participant-management/participant-management.component';
import { ExportPanelComponent } from '../export-panel/export-panel.component';
import { ImportPanelComponent } from '../import-panel/import-panel.component';
import { ScoreSummaryComponent } from '../score-summary/score-summary.component';
import { FullscreenMapViewComponent } from '../fullscreen-map-view/fullscreen-map-view.component';
import { StationColorSettingsComponent } from '../station-color-settings/station-color-settings.component';

@Component({
    selector: 'app-dashboard',
    imports: [
    CommonModule,
    LogSelectorComponent,
    QsoEntryComponent,
    QsoListComponent,
    RigStatusComponent,
    ContestSelectionComponent,
    StationManagementComponent,
    ParticipantManagementComponent,
    ExportPanelComponent,
    ImportPanelComponent,
    ScoreSummaryComponent,
    FullscreenMapViewComponent,
    StationColorSettingsComponent
],
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  currentLog: Log | null = null;
  currentParticipant: LogParticipant | null = null;
  isMapOpen = false;
  permissions: UserPermissions | null = null;

  constructor(
    private authService: AuthService,
    private logService: LogService,
    public themeService: ThemeService,
    private permissionsService: PermissionsService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Subscribe to current user
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      this.loadCurrentParticipant();
    });

    // Subscribe to current log
    this.logService.currentLog$.subscribe(log => {
      this.currentLog = log;
      this.loadCurrentParticipant();
    });

    // Load current log from storage
    this.logService.loadCurrentLogFromStorage();

    // Load user's logs
    this.logService.getMyLogs().subscribe();

    // Load pending invitations count
    this.logService.getPendingInvitations().subscribe();

    // Load permissions for current user and log
    this.permissionsService.getCurrentPermissions().subscribe(permissions => {
      this.permissions = permissions;
    });
  }

  loadCurrentParticipant(): void {
    if (!this.currentLog || !this.currentUser) {
      this.currentParticipant = null;
      return;
    }

    // Load participants and find the current user
    this.logService.getLogParticipants(this.currentLog.id).subscribe({
      next: (participants) => {
        this.currentParticipant = participants.find(
          p => p.userId === this.currentUser!.id
        ) || null;
      },
      error: (err) => {
        console.error('Error loading participant data:', err);
        this.currentParticipant = null;
      }
    });
  }

  getStationAssignmentText(): string {
    if (!this.currentParticipant) return '';

    if (this.currentParticipant.isGota) {
      return 'GOTA';
    }

    if (this.currentParticipant.stationNumber) {
      return `Station ${this.currentParticipant.stationNumber}`;
    }

    return '';
  }

  getStationAssignmentColor(): string {
    if (!this.currentParticipant) return '#9E9E9E';

    if (this.currentParticipant.isGota) {
      return getStationColor('gota', 'primary');
    }

    if (this.currentParticipant.stationNumber) {
      return getStationColor(this.currentParticipant.stationNumber, 'primary');
    }

    return '#9E9E9E'; // Gray for unassigned
  }

  toggleMap(): void {
    this.isMapOpen = !this.isMapOpen;
  }

  closeMap(): void {
    this.isMapOpen = false;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
