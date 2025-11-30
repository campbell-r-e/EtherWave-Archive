import { Component, OnInit } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log/log.service';
import { QSORequest, QSO } from '../../models/qso.model';
import { Station, Contest } from '../../models/station.model';
import { getAllBandNames, frequencyToBand } from '../../models/band.constants';

@Component({
  selector: 'app-qso-entry',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './qso-entry.component.html',
  styleUrls: ['./qso-entry.component.css']
})
export class QsoEntryComponent implements OnInit {
    public Object = Object;
  qso: QSORequest = this.getEmptyQSO();
  stations: Station[] = [];
  contests: Contest[] = [];
  availableBands: string[] = getAllBandNames();

  lookupInProgress = false;
  saveInProgress = false;
  lastSavedQSO: QSO | null = null;
  errorMessage = '';
  successMessage = '';

  // Contest-specific fields
  contestDataFields: any = {};

  constructor(
    private apiService: ApiService,
    private logService: LogService
  ) {}

  ngOnInit(): void {
    this.loadStations();
    this.loadContests();
    this.setCurrentDateTime();
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
  }

  clearMessages(): void {
    setTimeout(() => {
      this.successMessage = '';
      this.errorMessage = '';
    }, 5000);
  }
}
