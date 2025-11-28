import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Contest } from '../../models/station.model';

@Component({
  selector: 'app-contest-selection',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contest-selection.component.html',
  styleUrls: ['./contest-selection.component.css']
})
export class ContestSelectionComponent implements OnInit {
  contests: Contest[] = [];
  activeContests: Contest[] = [];
  selectedContest: Contest | null = null;
  showAllContests = false;
  loading = true;

  @Output() contestSelected = new EventEmitter<Contest>();

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadContests();
  }

  loadContests(): void {
    this.loading = true;
    this.apiService.getContests().subscribe({
      next: (contests) => {
        this.contests = contests;
        this.loading = false;
        this.loadActiveContests();
      },
      error: (err) => {
        console.error('Error loading contests:', err);
        this.loading = false;
      }
    });
  }

  loadActiveContests(): void {
    this.apiService.getActiveContests().subscribe({
      next: (activeContests) => {
        this.activeContests = activeContests;
      },
      error: (err) => console.error('Error loading active contests:', err)
    });
  }

  getDisplayedContests(): Contest[] {
    return this.showAllContests ? this.contests : this.activeContests;
  }

  selectContest(contest: Contest): void {
    this.selectedContest = contest;
    this.contestSelected.emit(contest);
  }

  isContestActive(contest: Contest): boolean {
    if (!contest.startDate || !contest.endDate) return false;
    const now = new Date();
    const start = new Date(contest.startDate);
    const end = new Date(contest.endDate);
    return now >= start && now <= end;
  }

  getContestDateRange(contest: Contest): string {
    if (!contest.startDate || !contest.endDate) return 'Dates TBD';
    const start = new Date(contest.startDate);
    const end = new Date(contest.endDate);
    const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric', year: 'numeric' };
    return `${start.toLocaleDateString('en-US', options)} - ${end.toLocaleDateString('en-US', options)}`;
  }

  getContestBadgeClass(contest: Contest): string {
    if (this.isContestActive(contest)) return 'badge bg-success';
    if (this.isContestUpcoming(contest)) return 'badge bg-primary';
    return 'badge bg-secondary';
  }

  getContestStatusText(contest: Contest): string {
    if (this.isContestActive(contest)) return 'Active Now';
    if (this.isContestUpcoming(contest)) return 'Upcoming';
    return 'Past';
  }

  isContestUpcoming(contest: Contest): boolean {
    if (!contest.startDate) return false;
    const now = new Date();
    const start = new Date(contest.startDate);
    return start > now;
  }

  toggleShowAll(): void {
    this.showAllContests = !this.showAllContests;
  }

  clearSelection(): void {
    this.selectedContest = null;
  }
}
