import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';

import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Log, LogRequest, LogType, LogPurpose } from '../../../models/log.model';
import { LogService } from '../../../services/log/log.service';
import { AuthService } from '../../../services/auth/auth.service';
import { User } from '../../../models/auth/user.model';

@Component({
    selector: 'app-log-selector',
    imports: [RouterModule, ReactiveFormsModule],
    templateUrl: './log-selector.component.html',
    styleUrls: ['./log-selector.component.css']
})
export class LogSelectorComponent implements OnInit {
  logs: Log[] = [];
  currentLog: Log | null = null;
  showCreateModal = false;
  creatingType: LogType = LogType.PERSONAL;
  createLogForm: FormGroup;
  loading = false;
  error: string | null = null;
  pendingInvitationsCount = 0;
  currentUser: User | null = null;
  private modalTriggerElement: HTMLElement | null = null;

  @ViewChild('logNameInput') logNameInput!: ElementRef;

  LogType = LogType;
  LogPurpose = LogPurpose;

  constructor(
    private logService: LogService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {
    this.createLogForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
      purpose: [LogPurpose.GENERAL],
      isPublic: [false]
    });
  }

  ngOnInit(): void {
    // Subscribe to current user
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
    });

    // Subscribe to logs
    this.logService.logs$.subscribe(logs => {
      this.logs = logs;
    });

    // Subscribe to current log
    this.logService.currentLog$.subscribe(log => {
      this.currentLog = log;
    });

    // Subscribe to pending invitations count
    this.logService.pendingInvitationsCount$.subscribe(count => {
      this.pendingInvitationsCount = count;
    });

    // Load logs
    this.loadLogs();

    // Load pending invitations
    this.logService.getPendingInvitations().subscribe();
  }

  /**
   * Check if the log is owned by the current user
   */
  isOwnedLog(log: Log): boolean {
    if (!this.currentUser) return false;
    return log.creatorId === this.currentUser.id;
  }

  loadLogs(): void {
    this.loading = true;
    this.error = null;

    this.logService.getMyLogs().subscribe({
      next: (logs) => {
        this.loading = false;

        // If no log is selected and logs exist, select the first one
        if (!this.currentLog && logs.length > 0) {
          this.selectLog(logs[0]);
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Failed to load logs';
        console.error('Error loading logs:', err);
      }
    });
  }

  selectLog(log: Log): void {
    this.logService.setCurrentLog(log);
  }

  openCreatePersonalLog(): void {
    this.creatingType = LogType.PERSONAL;
    this.openCreateModal();
  }

  openCreateSharedLog(): void {
    this.creatingType = LogType.SHARED;
    this.openCreateModal();
  }

  private openCreateModal(): void {
    // Store the element that triggered the modal for focus return
    this.modalTriggerElement = document.activeElement as HTMLElement;

    this.showCreateModal = true;
    this.createLogForm.reset({
      purpose: LogPurpose.GENERAL,
      isPublic: false
    });
    this.error = null;

    // Focus first input when modal opens
    setTimeout(() => {
      if (this.logNameInput) {
        this.logNameInput.nativeElement.focus();
      }
    }, 100);
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.createLogForm.reset();
    this.error = null;

    // Return focus to trigger element
    setTimeout(() => {
      if (this.modalTriggerElement) {
        this.modalTriggerElement.focus();
        this.modalTriggerElement = null;
      }
    }, 100);
  }

  createLog(): void {
    if (this.createLogForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    const formValue = this.createLogForm.value;
    const request: LogRequest = {
      name: formValue.name,
      description: formValue.description,
      type: this.creatingType,
      purpose: formValue.purpose,
      isPublic: this.creatingType === LogType.PERSONAL ? false : formValue.isPublic
    };

    this.logService.createLog(request).subscribe({
      next: () => {
        this.loading = false;
        this.closeCreateModal();
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Failed to create log';
        console.error('Error creating log:', err);
      }
    });
  }

  deleteLog(log: Log, event: Event): void {
    event.stopPropagation();

    if (!confirm(`Are you sure you want to delete log "${log.name}"?`)) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.logService.deleteLog(log.id).subscribe({
      next: () => {
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Failed to delete log';
        console.error('Error deleting log:', err);
      }
    });
  }

  leaveLog(log: Log, event: Event): void {
    event.stopPropagation();

    if (!confirm(`Are you sure you want to leave log "${log.name}"?`)) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.logService.leaveLog(log.id).subscribe({
      next: () => {
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Failed to leave log';
        console.error('Error leaving log:', err);
      }
    });
  }

  canDelete(log: Log): boolean {
    return log.userRole === 'CREATOR';
  }

  canLeave(log: Log): boolean {
    return log.userRole !== 'CREATOR';
  }

  getRoleBadgeClass(role: string): string {
    switch (role) {
      case 'CREATOR':
        return 'badge bg-primary';
      case 'STATION':
        return 'badge bg-success';
      case 'VIEWER':
        return 'badge bg-secondary';
      default:
        return 'badge bg-light';
    }
  }

  getTypeBadgeClass(type: LogType): string {
    return type === LogType.PERSONAL ? 'badge bg-info' : 'badge bg-warning';
  }

  getPurposeLabel(purpose: LogPurpose | undefined): string {
    const labels: Record<LogPurpose, string> = {
      [LogPurpose.GENERAL]:          'General',
      [LogPurpose.FIELD_DAY]:        'Field Day',
      [LogPurpose.POTA]:             'POTA',
      [LogPurpose.SOTA]:             'SOTA',
      [LogPurpose.CQ_WW]:            'CQ WW',
      [LogPurpose.SWEEPSTAKES]:      'Sweepstakes',
      [LogPurpose.WINTER_FIELD_DAY]: 'Winter FD',
      [LogPurpose.STATE_QSO_PARTY]:  'State QSO',
      [LogPurpose.DX_EXPEDITION]:    'DX Expedition',
      [LogPurpose.SPECIAL_EVENT]:    'Special Event'
    };
    return purpose ? (labels[purpose] ?? 'General') : 'General';
  }

  purposeAutoLinksContest(purpose: LogPurpose | null): string | null {
    const map: Partial<Record<LogPurpose, string>> = {
      [LogPurpose.FIELD_DAY]:        'ARRL Field Day',
      [LogPurpose.POTA]:             'POTA',
      [LogPurpose.SOTA]:             'SOTA',
      [LogPurpose.CQ_WW]:            'CQ WW',
      [LogPurpose.SWEEPSTAKES]:      'ARRL Sweepstakes',
      [LogPurpose.WINTER_FIELD_DAY]: 'Winter Field Day',
      [LogPurpose.STATE_QSO_PARTY]:  'State QSO Party'
    };
    return purpose ? (map[purpose] ?? null) : null;
  }

  get selectedPurposeContest(): string | null {
    const purpose = this.createLogForm.get('purpose')?.value as LogPurpose | null;
    return this.purposeAutoLinksContest(purpose);
  }

  get personalLogs(): Log[] {
    return this.logs.filter(l => l.type === LogType.PERSONAL);
  }

  get sharedLogs(): Log[] {
    return this.logs.filter(l => l.type === LogType.SHARED);
  }
}
