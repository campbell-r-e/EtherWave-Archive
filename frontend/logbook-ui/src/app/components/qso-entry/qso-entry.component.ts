import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { QSORequest, QSO } from '../../models/qso.model';
import { Station, Contest } from '../../models/station.model';

@Component({
  selector: 'app-qso-entry',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './qso-entry.component.html',
  styleUrls: ['./qso-entry.component.css']
})
export class QsoEntryComponent implements OnInit {
    public Object = Object;
  qso: QSORequest = this.getEmptyQSO();
  stations: Station[] = [];
  contests: Contest[] = [];

  lookupInProgress = false;
  saveInProgress = false;
  lastSavedQSO: QSO | null = null;
  errorMessage = '';
  successMessage = '';

  // Contest-specific fields
  contestDataFields: any = {};

  constructor(private apiService: ApiService) {}

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
    this.qso.qsoDate = now.toISOString().split('T')[0];
    this.qso.timeOn = now.toTimeString().split(' ')[0];
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
    // Auto-calculate band from frequency
    const freq = this.qso.frequencyKhz;
    if (freq >= 1800 && freq <= 2000) this.qso.band = '160m';
    else if (freq >= 3500 && freq <= 4000) this.qso.band = '80m';
    else if (freq >= 7000 && freq <= 7300) this.qso.band = '40m';
    else if (freq >= 14000 && freq <= 14350) this.qso.band = '20m';
    else if (freq >= 21000 && freq <= 21450) this.qso.band = '15m';
    else if (freq >= 28000 && freq <= 29700) this.qso.band = '10m';
    else if (freq >= 50000 && freq <= 54000) this.qso.band = '6m';
    else if (freq >= 144000 && freq <= 148000) this.qso.band = '2m';
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

    // Build contest data JSON if applicable
    if (this.qso.contestId && Object.keys(this.contestDataFields).length > 0) {
      this.qso.contestData = JSON.stringify(this.contestDataFields);
    }

    this.saveInProgress = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.apiService.createQSO(this.qso).subscribe({
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
