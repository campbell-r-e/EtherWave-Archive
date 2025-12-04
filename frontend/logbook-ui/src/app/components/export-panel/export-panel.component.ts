import { Component, OnInit } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log/log.service';
import { Contest } from '../../models/station.model';
import { Log } from '../../models/log.model';

@Component({
  selector: 'app-export-panel',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './export-panel.component.html',
  styleUrls: ['./export-panel.component.css']
})
export class ExportPanelComponent implements OnInit {
  contests: Contest[] = [];
  selectedContest: number | null = null;
  currentLog: Log | null = null;
  cabrilloOptions = {
    callsign: '',
    operators: '',
    category: ''
  };
  showCabrilloOptions = false;
  cabrilloExportType: 'combined' | 'gota' | 'non-gota' = 'combined';

  constructor(
    private apiService: ApiService,
    private logService: LogService
  ) {}

  ngOnInit(): void {
    this.loadContests();
    this.loadCurrentLog();
  }

  loadContests(): void {
    this.apiService.getContests().subscribe({
      next: (contests) => {
        this.contests = contests;
      },
      error: (err) => console.error('Error loading contests:', err)
    });
  }

  loadCurrentLog(): void {
    this.logService.currentLog$.subscribe({
      next: (log) => {
        this.currentLog = log;
      }
    });
  }

  exportADIF(): void {
    if (!this.currentLog) {
      alert('Please select a log first');
      return;
    }
    this.apiService.exportAdifByLog(this.currentLog.id);
  }

  exportAdifCombined(): void {
    if (!this.currentLog) {
      alert('Please select a log first');
      return;
    }
    this.apiService.exportAdifCombined(this.currentLog.id);
  }

  exportAdifGota(): void {
    if (!this.currentLog) {
      alert('Please select a log first');
      return;
    }
    this.apiService.exportAdifGota(this.currentLog.id);
  }

  exportAdifNonGota(): void {
    if (!this.currentLog) {
      alert('Please select a log first');
      return;
    }
    this.apiService.exportAdifNonGota(this.currentLog.id);
  }

  showCabrilloForm(exportType: 'combined' | 'gota' | 'non-gota' = 'combined'): void {
    this.cabrilloExportType = exportType;
    this.showCabrilloOptions = true;
  }

  cancelCabrilloExport(): void {
    this.showCabrilloOptions = false;
    this.resetCabrilloOptions();
  }

  exportCabrillo(): void {
    if (!this.currentLog) {
      alert('Please select a log first');
      return;
    }

    if (!this.cabrilloOptions.callsign) {
      alert('Please provide a callsign for Cabrillo export');
      return;
    }

    // Export based on selected type
    switch (this.cabrilloExportType) {
      case 'gota':
        this.apiService.exportCabrilloGota(
          this.currentLog.id,
          this.cabrilloOptions.callsign,
          this.cabrilloOptions.operators || undefined,
          this.cabrilloOptions.category || undefined
        );
        break;
      case 'non-gota':
        this.apiService.exportCabrilloNonGota(
          this.currentLog.id,
          this.cabrilloOptions.callsign,
          this.cabrilloOptions.operators || undefined,
          this.cabrilloOptions.category || undefined
        );
        break;
      default:
        this.apiService.exportCabrilloCombined(
          this.currentLog.id,
          this.cabrilloOptions.callsign,
          this.cabrilloOptions.operators || undefined,
          this.cabrilloOptions.category || undefined
        );
    }

    this.showCabrilloOptions = false;
    this.resetCabrilloOptions();
  }

  resetCabrilloOptions(): void {
    this.selectedContest = null;
    this.cabrilloOptions = {
      callsign: '',
      operators: '',
      category: ''
    };
  }

  getContestName(contestId: number): string {
    const contest = this.contests.find(c => c.id === contestId);
    return contest ? contest.contestName : 'Unknown Contest';
  }

  isContestLog(): boolean {
    return this.currentLog?.contestId != null;
  }

  getLogTypeName(): string {
    if (!this.currentLog) return 'No log selected';
    return this.isContestLog()
      ? `Contest Log: ${this.currentLog.contestName}`
      : 'Personal Log';
  }
}
