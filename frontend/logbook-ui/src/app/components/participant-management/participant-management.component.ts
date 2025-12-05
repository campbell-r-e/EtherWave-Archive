import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LogService } from '../../services/log/log.service';
import { LogParticipant, LogType, ParticipantRole, StationAssignmentRequest } from '../../models/log.model';
import { getStationColor } from '../../config/station-colors';

@Component({
  selector: 'app-participant-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './participant-management.component.html',
  styleUrls: ['./participant-management.component.css']
})
export class ParticipantManagementComponent implements OnInit {
  participants: LogParticipant[] = [];
  loading = true;
  currentLog: any = null;
  isCreator = false;

  // For editing station assignments
  editingParticipantId: number | null = null;
  editStationNumber: number | null = null;
  editIsGota = false;

  constructor(private logService: LogService) {}

  ngOnInit(): void {
    // Subscribe to current log
    this.logService.currentLog$.subscribe(log => {
      this.currentLog = log;
      if (log) {
        this.isCreator = log.userRole === ParticipantRole.CREATOR;
        this.loadParticipants();
      }
    });
  }

  loadParticipants(): void {
    if (!this.currentLog) return;

    this.loading = true;
    this.logService.getLogParticipants(this.currentLog.id).subscribe({
      next: (participants) => {
        this.participants = participants;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading participants:', err);
        this.loading = false;
      }
    });
  }

  isSharedLog(): boolean {
    return this.currentLog?.type === LogType.SHARED;
  }

  shouldShowComponent(): boolean {
    return this.isSharedLog() && this.isCreator;
  }

  startEditing(participant: LogParticipant): void {
    this.editingParticipantId = participant.id;
    this.editStationNumber = participant.stationNumber || null;
    this.editIsGota = participant.isGota || false;
  }

  cancelEditing(): void {
    this.editingParticipantId = null;
    this.editStationNumber = null;
    this.editIsGota = false;
  }

  saveAssignment(participant: LogParticipant): void {
    if (!this.currentLog || this.editingParticipantId !== participant.id) return;

    // Validate: cannot have both station number and GOTA
    if (this.editStationNumber && this.editIsGota) {
      alert('A participant cannot be both assigned to a station and designated as GOTA');
      return;
    }

    // Validate station number range
    if (this.editStationNumber !== null && (this.editStationNumber < 1 || this.editStationNumber > 1000)) {
      alert('Station number must be between 1 and 1000');
      return;
    }

    const request: StationAssignmentRequest = {
      stationNumber: this.editStationNumber || undefined,
      isGota: this.editIsGota
    };

    this.logService.updateParticipantStation(this.currentLog.id, participant.id, request).subscribe({
      next: (updatedParticipant) => {
        // Update in local list
        const index = this.participants.findIndex(p => p.id === participant.id);
        if (index !== -1) {
          this.participants[index] = updatedParticipant;
        }
        this.cancelEditing();
      },
      error: (err) => {
        console.error('Error updating station assignment:', err);
        alert('Error updating station assignment: ' + (err.error?.message || err.message));
      }
    });
  }

  getAssignmentBadge(participant: LogParticipant): string {
    if (participant.isGota) {
      return 'GOTA';
    }
    if (participant.stationNumber) {
      return `Station ${participant.stationNumber}`;
    }
    return 'Unassigned';
  }

  getAssignmentColor(participant: LogParticipant): string {
    if (participant.isGota) {
      return getStationColor('gota', 'primary');
    }
    if (participant.stationNumber) {
      return getStationColor(participant.stationNumber, 'primary');
    }
    return '#9E9E9E'; // Gray for unassigned
  }

  getRoleBadge(role: ParticipantRole): string {
    switch (role) {
      case ParticipantRole.CREATOR:
        return 'Creator';
      case ParticipantRole.STATION:
        return 'Station Operator';
      case ParticipantRole.VIEWER:
        return 'Viewer';
      default:
        return role;
    }
  }
}
