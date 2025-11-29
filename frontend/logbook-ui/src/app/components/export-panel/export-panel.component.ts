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
    this.apiService.exportADIF();
  }

  showCabrilloForm(): void {
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

    // Use new log-based export method (works for both contest and personal logs)
    this.apiService.exportCabrilloByLog(
      this.currentLog.id,
      this.cabrilloOptions.callsign,
      this.cabrilloOptions.operators || undefined,
      this.cabrilloOptions.category || undefined
    );

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
