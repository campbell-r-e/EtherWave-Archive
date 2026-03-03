import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LogService } from '../../services/log/log.service';
import { AwardService, AwardProgress } from '../../services/award/award.service';
import { Log } from '../../models/log.model';

@Component({
  selector: 'app-award-progress',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './award-progress.component.html',
  styleUrls: ['./award-progress.component.css']
})
export class AwardProgressComponent implements OnInit {
  currentLog: Log | null = null;
  progress: AwardProgress | null = null;
  loading = false;
  error: string | null = null;

  constructor(
    private logService: LogService,
    private awardService: AwardService
  ) {}

  ngOnInit(): void {
    this.logService.currentLog$.subscribe({
      next: (log) => {
        this.currentLog = log;
        if (log) {
          this.loadProgress();
        } else {
          this.progress = null;
        }
      }
    });
  }

  loadProgress(): void {
    if (!this.currentLog) return;

    this.loading = true;
    this.error = null;

    this.awardService.getProgress(this.currentLog.id).subscribe({
      next: (progress) => {
        this.progress = progress;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading award progress:', err);
        this.error = 'Failed to load award progress.';
        this.loading = false;
      }
    });
  }

  /** Percentage for a progress bar, capped at 100 */
  pct(worked: number, total: number): number {
    if (total <= 0) return 0;
    return Math.min(100, Math.round((worked / total) * 100));
  }

  /** Bootstrap color class based on progress percentage */
  progressColor(pct: number): string {
    if (pct >= 100) return 'bg-success';
    if (pct >= 66) return 'bg-info';
    if (pct >= 33) return 'bg-warning';
    return 'bg-danger';
  }
}
