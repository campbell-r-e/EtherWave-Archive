import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import {
  Log,
  LogRequest,
  LogParticipant,
  Invitation,
  InvitationRequest
} from '../../models/log.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LogService {
  private baseUrl = `${environment.apiUrl}/logs`;
  private invitationsUrl = `${environment.apiUrl}/invitations`;

  // Current selected log state
  private currentLogSubject = new BehaviorSubject<Log | null>(null);
  public currentLog$ = this.currentLogSubject.asObservable();

  // All user's logs
  private logsSubject = new BehaviorSubject<Log[]>([]);
  public logs$ = this.logsSubject.asObservable();

  // Pending invitations count
  private pendingInvitationsCountSubject = new BehaviorSubject<number>(0);
  public pendingInvitationsCount$ = this.pendingInvitationsCountSubject.asObservable();

  constructor(private http: HttpClient) {}

  // ===========================
  // Log Management
  // ===========================

  /**
   * Get all logs for the current user
   */
  getMyLogs(): Observable<Log[]> {
    return this.http.get<Log[]>(this.baseUrl).pipe(
      tap(logs => this.logsSubject.next(logs))
    );
  }

  /**
   * Get a specific log by ID
   */
  getLog(logId: number): Observable<Log> {
    return this.http.get<Log>(`${this.baseUrl}/${logId}`);
  }

  /**
   * Create a new log
   */
  createLog(request: LogRequest): Observable<Log> {
    return this.http.post<Log>(this.baseUrl, request).pipe(
      tap(log => {
        // Add to logs list
        const currentLogs = this.logsSubject.value;
        this.logsSubject.next([...currentLogs, log]);

        // Auto-select the newly created log
        this.setCurrentLog(log);
      })
    );
  }

  /**
   * Update an existing log
   */
  updateLog(logId: number, request: LogRequest): Observable<Log> {
    return this.http.put<Log>(`${this.baseUrl}/${logId}`, request).pipe(
      tap(updatedLog => {
        // Update in logs list
        const currentLogs = this.logsSubject.value;
        const index = currentLogs.findIndex(l => l.id === logId);
        if (index !== -1) {
          currentLogs[index] = updatedLog;
          this.logsSubject.next([...currentLogs]);
        }

        // Update current log if it's the one being updated
        if (this.currentLogSubject.value?.id === logId) {
          this.currentLogSubject.next(updatedLog);
        }
      })
    );
  }

  /**
   * Delete a log
   */
  deleteLog(logId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${logId}`).pipe(
      tap(() => {
        // Remove from logs list
        const currentLogs = this.logsSubject.value.filter(l => l.id !== logId);
        this.logsSubject.next(currentLogs);

        // Clear current log if it was deleted
        if (this.currentLogSubject.value?.id === logId) {
          this.currentLogSubject.next(null);
        }
      })
    );
  }

  /**
   * Freeze a log
   */
  freezeLog(logId: number): Observable<Log> {
    return this.http.post<Log>(`${this.baseUrl}/${logId}/freeze`, {}).pipe(
      tap(frozenLog => this.updateLogInState(frozenLog))
    );
  }

  /**
   * Unfreeze a log
   */
  unfreezeLog(logId: number): Observable<Log> {
    return this.http.post<Log>(`${this.baseUrl}/${logId}/unfreeze`, {}).pipe(
      tap(unfrozenLog => this.updateLogInState(unfrozenLog))
    );
  }

  /**
   * Get all participants for a log
   */
  getLogParticipants(logId: number): Observable<LogParticipant[]> {
    return this.http.get<LogParticipant[]>(`${this.baseUrl}/${logId}/participants`);
  }

  /**
   * Remove a participant from a log
   */
  removeParticipant(logId: number, participantId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${logId}/participants/${participantId}`);
  }

  /**
   * Leave a log
   */
  leaveLog(logId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${logId}/leave`, {}).pipe(
      tap(() => {
        // Remove from logs list
        const currentLogs = this.logsSubject.value.filter(l => l.id !== logId);
        this.logsSubject.next(currentLogs);

        // Clear current log if leaving it
        if (this.currentLogSubject.value?.id === logId) {
          this.currentLogSubject.next(null);
        }
      })
    );
  }

  // ===========================
  // Invitation Management
  // ===========================

  /**
   * Get pending invitations for current user
   */
  getPendingInvitations(): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.invitationsUrl}/pending`).pipe(
      tap(invitations => this.pendingInvitationsCountSubject.next(invitations.length))
    );
  }

  /**
   * Get invitations sent by current user
   */
  getSentInvitations(): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.invitationsUrl}/sent`);
  }

  /**
   * Get all invitations for a log
   */
  getInvitationsForLog(logId: number): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.invitationsUrl}/log/${logId}`);
  }

  /**
   * Get a specific invitation
   */
  getInvitation(invitationId: number): Observable<Invitation> {
    return this.http.get<Invitation>(`${this.invitationsUrl}/${invitationId}`);
  }

  /**
   * Create and send an invitation
   */
  createInvitation(request: InvitationRequest): Observable<Invitation> {
    return this.http.post<Invitation>(this.invitationsUrl, request);
  }

  /**
   * Accept an invitation
   */
  acceptInvitation(invitationId: number): Observable<Invitation> {
    return this.http.post<Invitation>(`${this.invitationsUrl}/${invitationId}/accept`, {}).pipe(
      tap(() => {
        // Refresh logs to include newly joined log
        this.getMyLogs().subscribe();

        // Refresh pending invitations count
        this.getPendingInvitations().subscribe();
      })
    );
  }

  /**
   * Decline an invitation
   */
  declineInvitation(invitationId: number): Observable<Invitation> {
    return this.http.post<Invitation>(`${this.invitationsUrl}/${invitationId}/decline`, {}).pipe(
      tap(() => {
        // Refresh pending invitations count
        this.getPendingInvitations().subscribe();
      })
    );
  }

  /**
   * Cancel an invitation
   */
  cancelInvitation(invitationId: number): Observable<Invitation> {
    return this.http.post<Invitation>(`${this.invitationsUrl}/${invitationId}/cancel`, {});
  }

  // ===========================
  // State Management
  // ===========================

  /**
   * Set the current active log
   */
  setCurrentLog(log: Log | null): void {
    this.currentLogSubject.next(log);
    if (log) {
      localStorage.setItem('currentLogId', log.id.toString());
    } else {
      localStorage.removeItem('currentLogId');
    }
  }

  /**
   * Get the current active log
   */
  getCurrentLog(): Log | null {
    return this.currentLogSubject.value;
  }

  /**
   * Load current log from localStorage on app startup
   */
  loadCurrentLogFromStorage(): void {
    const logId = localStorage.getItem('currentLogId');
    if (logId) {
      this.getLog(parseInt(logId, 10)).subscribe({
        next: (log) => this.setCurrentLog(log),
        error: () => {
          // Log not found or no access, clear storage
          localStorage.removeItem('currentLogId');
        }
      });
    }
  }

  /**
   * Refresh current log data
   */
  refreshCurrentLog(): void {
    const currentLog = this.getCurrentLog();
    if (currentLog) {
      this.getLog(currentLog.id).subscribe({
        next: (log) => this.setCurrentLog(log)
      });
    }
  }

  /**
   * Update log in state (helper method)
   */
  private updateLogInState(updatedLog: Log): void {
    // Update in logs list
    const currentLogs = this.logsSubject.value;
    const index = currentLogs.findIndex(l => l.id === updatedLog.id);
    if (index !== -1) {
      currentLogs[index] = updatedLog;
      this.logsSubject.next([...currentLogs]);
    }

    // Update current log if it's the one being updated
    if (this.currentLogSubject.value?.id === updatedLog.id) {
      this.currentLogSubject.next(updatedLog);
    }
  }
}
