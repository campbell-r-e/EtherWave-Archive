import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Invitation, InvitationRequest, InvitationStatus, ParticipantRole, Log } from '../../../models/log.model';
import { LogService } from '../../../services/log/log.service';

@Component({
  selector: 'app-invitations',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './invitations.component.html',
  styleUrls: ['./invitations.component.css']
})
export class InvitationsComponent implements OnInit {
  pendingInvitations: Invitation[] = [];
  sentInvitations: Invitation[] = [];
  logs: Log[] = [];
  showCreateModal = false;
  createInvitationForm: FormGroup;
  loading = false;
  error: string | null = null;
  activeTab: 'received' | 'sent' = 'received';

  ParticipantRole = ParticipantRole;
  InvitationStatus = InvitationStatus;

  constructor(
    private logService: LogService,
    private fb: FormBuilder
  ) {
    this.createInvitationForm = this.fb.group({
      logId: ['', Validators.required],
      inviteeUsername: ['', Validators.required],
      proposedRole: [ParticipantRole.STATION, Validators.required],
      stationCallsign: ['', Validators.maxLength(20)],
      message: ['', Validators.maxLength(500)]
    });
  }

  ngOnInit(): void {
    this.loadData();

    // Subscribe to logs
    this.logService.logs$.subscribe(logs => {
      this.logs = logs.filter(log => log.userRole === 'CREATOR');
    });
  }

  loadData(): void {
    this.loadPendingInvitations();
    this.loadSentInvitations();
    this.logService.getMyLogs().subscribe();
  }

  loadPendingInvitations(): void {
    this.loading = true;
    this.logService.getPendingInvitations().subscribe({
      next: (invitations) => {
        this.pendingInvitations = invitations;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load pending invitations';
        this.loading = false;
        console.error('Error loading pending invitations:', err);
      }
    });
  }

  loadSentInvitations(): void {
    this.loading = true;
    this.logService.getSentInvitations().subscribe({
      next: (invitations) => {
        this.sentInvitations = invitations;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load sent invitations';
        this.loading = false;
        console.error('Error loading sent invitations:', err);
      }
    });
  }

  acceptInvitation(invitation: Invitation): void {
    if (!confirm(`Accept invitation to join "${invitation.logName}" as ${invitation.proposedRole}?`)) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.logService.acceptInvitation(invitation.id).subscribe({
      next: () => {
        this.loading = false;
        this.loadPendingInvitations();
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Failed to accept invitation';
        console.error('Error accepting invitation:', err);
      }
    });
  }

  declineInvitation(invitation: Invitation): void {
    if (!confirm(`Decline invitation to join "${invitation.logName}"?`)) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.logService.declineInvitation(invitation.id).subscribe({
      next: () => {
        this.loading = false;
        this.loadPendingInvitations();
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Failed to decline invitation';
        console.error('Error declining invitation:', err);
      }
    });
  }

  cancelInvitation(invitation: Invitation): void {
    if (!confirm(`Cancel invitation to "${invitation.inviteeUsername}"?`)) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.logService.cancelInvitation(invitation.id).subscribe({
      next: () => {
        this.loading = false;
        this.loadSentInvitations();
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Failed to cancel invitation';
        console.error('Error canceling invitation:', err);
      }
    });
  }

  openCreateModal(): void {
    this.showCreateModal = true;
    this.createInvitationForm.reset({
      proposedRole: ParticipantRole.STATION
    });
    this.error = null;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.createInvitationForm.reset();
    this.error = null;
  }

  createInvitation(): void {
    if (this.createInvitationForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    const request: InvitationRequest = {
      logId: parseInt(this.createInvitationForm.value.logId, 10),
      inviteeUsername: this.createInvitationForm.value.inviteeUsername,
      proposedRole: this.createInvitationForm.value.proposedRole,
      stationCallsign: this.createInvitationForm.value.stationCallsign || undefined,
      message: this.createInvitationForm.value.message || undefined
    };

    this.logService.createInvitation(request).subscribe({
      next: () => {
        this.loading = false;
        this.closeCreateModal();
        this.loadSentInvitations();
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Failed to send invitation';
        console.error('Error creating invitation:', err);
      }
    });
  }

  setActiveTab(tab: 'received' | 'sent'): void {
    this.activeTab = tab;
  }

  getStatusBadgeClass(status: InvitationStatus): string {
    switch (status) {
      case InvitationStatus.PENDING:
        return 'badge bg-warning';
      case InvitationStatus.ACCEPTED:
        return 'badge bg-success';
      case InvitationStatus.DECLINED:
        return 'badge bg-danger';
      case InvitationStatus.CANCELLED:
        return 'badge bg-secondary';
      case InvitationStatus.EXPIRED:
        return 'badge bg-dark';
      default:
        return 'badge bg-light';
    }
  }

  getRoleBadgeClass(role: ParticipantRole): string {
    switch (role) {
      case ParticipantRole.CREATOR:
        return 'badge bg-primary';
      case ParticipantRole.STATION:
        return 'badge bg-success';
      case ParticipantRole.VIEWER:
        return 'badge bg-secondary';
      default:
        return 'badge bg-light';
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
  }
}
