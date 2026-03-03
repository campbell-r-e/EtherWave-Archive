import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log/log.service';
import { AuthService } from '../../services/auth/auth.service';
import { RigControlService } from '../../services/rig-control.service';
import { QSORequest, QSO } from '../../models/qso.model';
import { Station, Contest } from '../../models/station.model';
import { User } from '../../models/auth/user.model';
import { getAllBandNames, frequencyToBand } from '../../models/band.constants';

@Component({
  selector: 'app-qso-entry',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './qso-entry.component.html',
  styleUrls: ['./qso-entry.component.css']
})
export class QsoEntryComponent implements OnInit, OnDestroy {
    public Object = Object;
  @ViewChild('callsignInput') callsignInput!: ElementRef;

  qso: QSORequest = this.getEmptyQSO();
  stations: Station[] = [];
  contests: Contest[] = [];
  availableBands: string[] = getAllBandNames();

  lookupInProgress = false;
  saveInProgress = false;
  lastSavedQSO: QSO | null = null;
  errorMessage = '';
  successMessage = '';

  // Station assignment for multi-station contests
  userStationAssignment: { stationNumber?: number; isGota?: boolean } | null = null;

  // Contest-specific fields
  contestDataFields: any = {};

  // Current user for operator autofill
  currentUser: User | null = null;
  operatorCallsign: string = '';

  private rigSub?: Subscription;

  constructor(
    private apiService: ApiService,
    private logService: LogService,
    private authService: AuthService,
    private rigControlService: RigControlService
  ) {}

  ngOnInit(): void {
    this.loadStations();
    this.loadContests();
    this.setCurrentDateTime();
    this.loadStationAssignment();
    this.loadCurrentUser();
    this.subscribeToRig();
  }

  ngOnDestroy(): void {
    this.rigSub?.unsubscribe();
  }

  private subscribeToRig(): void {
    this.rigSub = this.rigControlService.onStatusUpdate().subscribe(({ status }) => {
      if (status.frequencyHz) {
        this.qso.frequencyKhz = status.frequencyHz / 1000;
        this.onFrequencyChange();
      }
      if (status.mode) {
        this.qso.mode = status.mode;
      }
    });
  }

  loadCurrentUser(): void {
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      if (user) {
        // Autofill operator with current user's ID and callsign
        this.qso.operatorId = user.id;
        this.operatorCallsign = user.callsign || user.username;
      }
    });
  }

  getEmptyQSO(): QSORequest {
    return {
      stationId: 0,
      callsign: '',
      frequencyKhz: 14250,
      mode: 'SSB',
      qsoDate: '',
      timeOn: '',
      rstSent: '59',
      rstRcvd: '59'
    };
  }

  loadStations(): void {
    this.apiService.getStations().subscribe({
      next: (stations) => {
        this.stations = stations;
        if (stations.length > 0) {
          this.qso.stationId = stations[0].id!;
        }
      },
      error: (err) => console.error('Error loading stations:', err)
    });
  }

  loadContests(): void {
    this.apiService.getActiveContests().subscribe({
      next: (contests) => this.contests = contests,
      error: (err) => console.error('Error loading contests:', err)
    });
  }

  loadStationAssignment(): void {
    const currentLog = this.logService.getCurrentLog();
    if (!currentLog) {
      return;
    }

    this.apiService.getMyStationAssignment(currentLog.id).subscribe({
      next: (participant) => {
        if (participant) {
          this.userStationAssignment = {
            stationNumber: participant.stationNumber,
            isGota: participant.isGota || false
          };
        }
      },
      error: (err) => {
        // 404 is expected if user is not a participant or log is personal
        if (err.status !== 404) {
          console.error('Error loading station assignment:', err);
        }
      }
    });
  }

  setCurrentDateTime(): void {
    const now = new Date();
    // Always use UTC for date and time
    this.qso.qsoDate = now.toISOString().split('T')[0]; // YYYY-MM-DD in UTC
    this.qso.timeOn = now.toISOString().split('T')[1].split('.')[0]; // HH:MM:SS in UTC
  }

  onCallsignChange(): void {
    const callsign = this.qso.callsign.toUpperCase().trim();
    if (callsign.length >= 3) {
      this.lookupCallsign(callsign);
    }
  }

  lookupCallsign(callsign: string): void {
    this.lookupInProgress = true;
    this.apiService.lookupCallsign(callsign).subscribe({
      next: (info) => {
        this.qso.name = info.name;
        this.qso.state = info.state;
        this.qso.country = info.country;
        this.qso.gridSquare = info.gridSquare;
        this.qso.licenseClass = info.licenseClass;
        this.lookupInProgress = false;
      },
      error: () => {
        this.lookupInProgress = false;
      }
    });
  }

  onFrequencyChange(): void {
    // Auto-calculate band from frequency using comprehensive band list
    const band = frequencyToBand(this.qso.frequencyKhz);
    if (band) {
      this.qso.band = band;
    }
  }

  onContestChange(): void {
    // Initialize contest-specific fields based on contest
    const contest = this.contests.find(c => c.id === this.qso.contestId);
    if (contest) {
      this.contestDataFields = {};
      if (contest.contestCode.includes('FD') || contest.contestCode.includes('WFD')) {
        this.contestDataFields.class = '';
        this.contestDataFields.section = '';
      } else if (contest.contestCode === 'POTA') {
        this.contestDataFields.park_ref = '';
      }
    }
  }

  saveQSO(): void {
    if (!this.validateQSO()) {
      return;
    }

    // Get current log
    const currentLog = this.logService.getCurrentLog();
    if (!currentLog) {
      this.errorMessage = 'Please select a log first';
      return;
    }

    // Build contest data JSON if applicable
    if (this.qso.contestId && Object.keys(this.contestDataFields).length > 0) {
      this.qso.contestData = JSON.stringify(this.contestDataFields);
    }

    this.saveInProgress = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.apiService.createQSO(this.qso, currentLog.id).subscribe({
      next: (savedQSO) => {
        this.lastSavedQSO = savedQSO;
        this.successMessage = `QSO with ${savedQSO.callsign} saved successfully!`;
        if (savedQSO.validationErrors) {
          this.errorMessage = savedQSO.validationErrors;
        }
        // Update station assignment from saved QSO (in case it changed)
        if (savedQSO.stationNumber || savedQSO.isGota) {
          this.userStationAssignment = {
            stationNumber: savedQSO.stationNumber,
            isGota: savedQSO.isGota || false
          };
        }
        this.saveInProgress = false;
        this.resetForm();
      },
      error: (err) => {
        this.errorMessage = 'Error saving QSO: ' + (err.error?.message || err.message);
        this.saveInProgress = false;
      }
    });
  }

  validateQSO(): boolean {
    if (!this.qso.stationId) {
      this.errorMessage = 'Please select a station';
      return false;
    }
    if (!this.qso.callsign || this.qso.callsign.length < 3) {
      this.errorMessage = 'Please enter a valid callsign';
      return false;
    }
    if (!this.qso.frequencyKhz) {
      this.errorMessage = 'Please enter frequency';
      return false;
    }
    if (!this.qso.mode) {
      this.errorMessage = 'Please select mode';
      return false;
    }
    return true;
  }

  resetForm(): void {
    const stationId = this.qso.stationId;
    const contestId = this.qso.contestId;
    this.qso = this.getEmptyQSO();
    this.qso.stationId = stationId;
    this.qso.contestId = contestId;
    this.setCurrentDateTime();
    this.contestDataFields = {};
    if (contestId) {
      this.onContestChange();
    }

    // Focus callsign input for rapid logging (accessibility + UX)
    setTimeout(() => {
      if (this.callsignInput) {
        this.callsignInput.nativeElement.focus();
      }
    }, 0);
  }

  clearMessages(): void {
    setTimeout(() => {
      this.successMessage = '';
      this.errorMessage = '';
    }, 5000);
  }

  // Multi-station assignment display helpers
  getStationAssignmentLabel(): string {
    // Use userStationAssignment if available (loaded on init)
    if (this.userStationAssignment) {
      if (this.userStationAssignment.isGota) {
        return 'GOTA';
      }
      if (this.userStationAssignment.stationNumber) {
        return `Station ${this.userStationAssignment.stationNumber}`;
      }
    }
    // Fall back to lastSavedQSO for backwards compatibility
    if (this.lastSavedQSO?.isGota) {
      return 'GOTA';
    }
    if (this.lastSavedQSO?.stationNumber) {
      return `Station ${this.lastSavedQSO.stationNumber}`;
    }
    return '';
  }

  getStationAssignmentColor(): string {
    const colors: { [key: number]: string } = {
      1: '#1E88E5',  // Blue
      2: '#E53935',  // Red
      3: '#FB8C00',  // Orange
      4: '#8E24AA',  // Purple
      5: '#00ACC1',  // Cyan
      6: '#FDD835',  // Yellow
    };

    // Use userStationAssignment if available (loaded on init)
    if (this.userStationAssignment) {
      if (this.userStationAssignment.isGota) {
        return '#43A047'; // Green (GEKHoosier theme)
      }
      if (this.userStationAssignment.stationNumber) {
        return colors[this.userStationAssignment.stationNumber] || '#9E9E9E';
      }
    }
    // Fall back to lastSavedQSO for backwards compatibility
    if (this.lastSavedQSO?.isGota) {
      return '#43A047'; // Green (GEKHoosier theme)
    }
    if (this.lastSavedQSO?.stationNumber) {
      return colors[this.lastSavedQSO.stationNumber] || '#9E9E9E';
    }
    return '#9E9E9E'; // Gray
  }

  hasStationAssignment(): boolean {
    // Check userStationAssignment first
    if (this.userStationAssignment) {
      return !!(this.userStationAssignment.stationNumber || this.userStationAssignment.isGota);
    }
    // Fall back to lastSavedQSO
    return !!(this.lastSavedQSO?.stationNumber || this.lastSavedQSO?.isGota);
  }
}
