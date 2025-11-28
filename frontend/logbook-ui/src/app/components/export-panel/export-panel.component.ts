import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Contest } from '../../models/station.model';

@Component({
  selector: 'app-export-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './export-panel.component.html',
  styleUrls: ['./export-panel.component.css']
})
export class ExportPanelComponent implements OnInit {
  contests: Contest[] = [];
  selectedContest: number | null = null;
  cabrilloOptions = {
    callsign: '',
    operators: '',
    category: ''
  };
  showCabrilloOptions = false;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadContests();
  }

  loadContests(): void {
    this.apiService.getContests().subscribe({
      next: (contests) => {
        this.contests = contests;
      },
      error: (err) => console.error('Error loading contests:', err)
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
    if (!this.selectedContest || !this.cabrilloOptions.callsign) {
      alert('Please select a contest and provide a callsign for Cabrillo export');
      return;
    }

    this.apiService.exportCabrillo(
      this.selectedContest,
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
    return contest ? contest.name : 'Unknown Contest';
  }
}
