import { Component, OnInit } from '@angular/core';

import { Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';
import { LogService } from '../../services/log/log.service';
import { User } from '../../models/auth/user.model';
import { Log } from '../../models/log.model';

// Import all logbook components
import { LogSelectorComponent } from '../log/log-selector/log-selector.component';
import { QsoEntryComponent } from '../qso-entry/qso-entry.component';
import { QsoListComponent } from '../qso-list/qso-list.component';
import { RigStatusComponent } from '../rig-status/rig-status.component';
import { MapVisualizationComponent } from '../map-visualization/map-visualization.component';
import { ContestSelectionComponent } from '../contest-selection/contest-selection.component';
import { StationManagementComponent } from '../station-management/station-management.component';
import { ExportPanelComponent } from '../export-panel/export-panel.component';
import { ImportPanelComponent } from '../import-panel/import-panel.component';

@Component({
    selector: 'app-dashboard',
    imports: [
    LogSelectorComponent,
    QsoEntryComponent,
    QsoListComponent,
    RigStatusComponent,
    MapVisualizationComponent,
    ContestSelectionComponent,
    StationManagementComponent,
    ExportPanelComponent,
    ImportPanelComponent
],
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  currentLog: Log | null = null;

  constructor(
    private authService: AuthService,
    private logService: LogService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Subscribe to current user
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
    });

    // Subscribe to current log
    this.logService.currentLog$.subscribe(log => {
      this.currentLog = log;
    });

    // Load current log from storage
    this.logService.loadCurrentLogFromStorage();

    // Load user's logs
    this.logService.getMyLogs().subscribe();

    // Load pending invitations count
    this.logService.getPendingInvitations().subscribe();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
