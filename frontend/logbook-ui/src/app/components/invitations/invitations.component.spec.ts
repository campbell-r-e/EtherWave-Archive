import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { InvitationsComponent } from './invitations.component';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

describe('InvitationsComponent', () => {
  let component: InvitationsComponent;
  let fixture: ComponentFixture<InvitationsComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  const mockInvitations = [
    {
      id: 1,
      logId: 10,
      logName: 'Field Day 2025',
      inviterUsername: 'W1AW',
      inviteeUsername: 'K2ABC',
      role: 'STATION',
      status: 'PENDING',
      createdAt: '2025-01-15T10:00:00',
      expiresAt: '2025-01-22T10:00:00'
    },
    {
      id: 2,
      logId: 11,
      logName: 'Winter Field Day',
      inviterUsername: 'N3XYZ',
      inviteeUsername: 'K2ABC',
      role: 'LOGGER',
      status: 'PENDING',
      createdAt: '2025-01-14T15:30:00',
      expiresAt: '2025-01-21T15:30:00'
    }
  ];

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'getPendingInvitations',
      'acceptInvitation',
      'rejectInvitation',
      'sendInvitation',
      'cancelInvitation',
      'getInvitationHistory'
    ]);
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getCurrentUser']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [InvitationsComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    authService.getCurrentUser.and.returnValue({ id: 1, username: 'K2ABC' });
    apiService.getPendingInvitations.and.returnValue(of(mockInvitations));

    fixture = TestBed.createComponent(InvitationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== INVITATION LOADING TESTS ====================

  it('should load pending invitations on init', () => {
    expect(apiService.getPendingInvitations).toHaveBeenCalled();
    expect(component.invitations.length).toBe(2);
  });

  it('should display invitation count badge', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const badge = compiled.querySelector('.invitation-count-badge');
    expect(badge?.textContent).toContain('2');
  });

  it('should display invitation details', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Field Day 2025');
    expect(compiled.textContent).toContain('W1AW');
    expect(compiled.textContent).toContain('STATION');
  });

  it('should handle empty invitations list', () => {
    apiService.getPendingInvitations.and.returnValue(of([]));
    component.ngOnInit();

    expect(component.invitations.length).toBe(0);
    expect(component.hasInvitations()).toBeFalsy();
  });

  it('should handle API error gracefully', () => {
    apiService.getPendingInvitations.and.returnValue(throwError(() => new Error('Network error')));

    component.loadInvitations();

    expect(component.errorMessage).toBeTruthy();
  });

  // ==================== ACCEPT INVITATION TESTS ====================

  it('should accept invitation', () => {
    apiService.acceptInvitation.and.returnValue(of({ success: true }));

    component.acceptInvitation(1);

    expect(apiService.acceptInvitation).toHaveBeenCalledWith(1);
  });

  it('should show success message on accept', () => {
    apiService.acceptInvitation.and.returnValue(of({ success: true }));

    component.acceptInvitation(1);

    expect(component.successMessage).toContain('accepted');
  });

  it('should remove accepted invitation from list', () => {
    apiService.acceptInvitation.and.returnValue(of({ success: true }));
    const initialCount = component.invitations.length;

    component.acceptInvitation(1);

    expect(component.invitations.length).toBe(initialCount - 1);
  });

  it('should navigate to log after accepting', () => {
    apiService.acceptInvitation.and.returnValue(of({ success: true, logId: 10 }));

    component.acceptInvitation(1);

    expect(router.navigate).toHaveBeenCalledWith(['/log', 10]);
  });

  it('should handle accept error', () => {
    apiService.acceptInvitation.and.returnValue(throwError(() => new Error('Accept failed')));

    component.acceptInvitation(1);

    expect(component.errorMessage).toBeTruthy();
  });

  // ==================== REJECT INVITATION TESTS ====================

  it('should reject invitation', () => {
    apiService.rejectInvitation.and.returnValue(of({ success: true }));

    component.rejectInvitation(1);

    expect(apiService.rejectInvitation).toHaveBeenCalledWith(1);
  });

  it('should show confirmation before rejecting', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.rejectInvitation.and.returnValue(of({ success: true }));

    component.rejectInvitation(1);

    expect(window.confirm).toHaveBeenCalled();
  });

  it('should not reject if cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.rejectInvitation(1);

    expect(apiService.rejectInvitation).not.toHaveBeenCalled();
  });

  it('should remove rejected invitation from list', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.rejectInvitation.and.returnValue(of({ success: true }));
    const initialCount = component.invitations.length;

    component.rejectInvitation(1);

    expect(component.invitations.length).toBe(initialCount - 1);
  });

  it('should handle reject error', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.rejectInvitation.and.returnValue(throwError(() => new Error('Reject failed')));

    component.rejectInvitation(1);

    expect(component.errorMessage).toBeTruthy();
  });

  // ==================== SEND INVITATION TESTS ====================

  it('should send new invitation', () => {
    const invitation = {
      logId: 10,
      inviteeUsername: 'VE3ABC',
      role: 'STATION'
    };
    apiService.sendInvitation.and.returnValue(of({ success: true }));

    component.sendInvitation(invitation);

    expect(apiService.sendInvitation).toHaveBeenCalledWith(jasmine.objectContaining(invitation));
  });

  it('should validate username before sending', () => {
    component.newInviteUsername = '';
    component.sendNewInvitation();

    expect(apiService.sendInvitation).not.toHaveBeenCalled();
    expect(component.validationError).toBeTruthy();
  });

  it('should validate role selection', () => {
    component.newInviteUsername = 'VE3ABC';
    component.newInviteRole = '';
    component.sendNewInvitation();

    expect(apiService.sendInvitation).not.toHaveBeenCalled();
  });

  it('should show success message after sending', () => {
    apiService.sendInvitation.and.returnValue(of({ success: true }));

    component.newInviteUsername = 'VE3ABC';
    component.newInviteRole = 'STATION';
    component.sendNewInvitation();

    expect(component.successMessage).toContain('Invitation sent');
  });

  it('should reset form after sending', () => {
    apiService.sendInvitation.and.returnValue(of({ success: true }));

    component.newInviteUsername = 'VE3ABC';
    component.newInviteRole = 'STATION';
    component.sendNewInvitation();

    expect(component.newInviteUsername).toBe('');
    expect(component.newInviteRole).toBe('');
  });

  // ==================== INVITATION EXPIRATION TESTS ====================

  it('should detect expired invitations', () => {
    const expiredInvitation = {
      ...mockInvitations[0],
      expiresAt: '2025-01-01T00:00:00' // Past date
    };

    const isExpired = component.isExpired(expiredInvitation);

    expect(isExpired).toBeTruthy();
  });

  it('should not show accept button for expired invitations', () => {
    component.invitations = [{
      ...mockInvitations[0],
      expiresAt: '2025-01-01T00:00:00'
    }];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const acceptButton = compiled.querySelector('.accept-button');
    expect(acceptButton).toBeNull();
  });

  it('should display expiration warning', () => {
    const soonExpiring = {
      ...mockInvitations[0],
      expiresAt: new Date(Date.now() + 86400000).toISOString() // 1 day from now
    };

    component.invitations = [soonExpiring];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.expiration-warning')).toBeTruthy();
  });

  it('should format expiration time', () => {
    const invitation = mockInvitations[0];
    const formatted = component.formatExpirationTime(invitation.expiresAt);

    expect(formatted).toBeTruthy();
  });

  // ==================== ROLE MANAGEMENT TESTS ====================

  it('should display role badge with correct styling', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const roleBadges = compiled.querySelectorAll('.role-badge');

    expect(roleBadges.length).toBeGreaterThan(0);
  });

  it('should show role permissions tooltip', () => {
    const permissions = component.getRolePermissions('STATION');

    expect(permissions).toContain('Log QSOs');
    expect(permissions).toContain('Edit own QSOs');
  });

  it('should differentiate role levels', () => {
    const adminRole = component.getRoleLevel('ADMIN');
    const stationRole = component.getRoleLevel('STATION');

    expect(adminRole).toBeGreaterThan(stationRole);
  });

  it('should provide available roles list', () => {
    const roles = component.getAvailableRoles();

    expect(roles).toContain('ADMIN');
    expect(roles).toContain('STATION');
    expect(roles).toContain('LOGGER');
    expect(roles).toContain('VIEWER');
  });

  // ==================== FILTER AND SORT TESTS ====================

  it('should filter invitations by status', () => {
    component.filterStatus = 'PENDING';
    const filtered = component.getFilteredInvitations();

    expect(filtered.every(inv => inv.status === 'PENDING')).toBeTruthy();
  });

  it('should sort invitations by date', () => {
    component.sortBy = 'date';
    component.sortDirection = 'desc';
    const sorted = component.getSortedInvitations();

    expect(sorted[0].createdAt).toBe('2025-01-15T10:00:00');
  });

  it('should sort invitations by log name', () => {
    component.sortBy = 'logName';
    component.sortDirection = 'asc';
    const sorted = component.getSortedInvitations();

    expect(sorted[0].logName).toBe('Field Day 2025');
  });

  // ==================== INVITATION HISTORY TESTS ====================

  it('should load invitation history', () => {
    const history = [
      { ...mockInvitations[0], status: 'ACCEPTED' },
      { ...mockInvitations[1], status: 'REJECTED' }
    ];
    apiService.getInvitationHistory.and.returnValue(of(history));

    component.loadHistory();

    expect(component.history).toEqual(history);
  });

  it('should show history in separate tab', () => {
    component.activeTab = 'history';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.history-tab')).toBeTruthy();
  });

  it('should display accepted invitations in history', () => {
    component.history = [{ ...mockInvitations[0], status: 'ACCEPTED' }];
    component.activeTab = 'history';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('ACCEPTED');
  });

  // ==================== CANCEL INVITATION TESTS ====================

  it('should cancel sent invitation', () => {
    apiService.cancelInvitation.and.returnValue(of({ success: true }));

    component.cancelInvitation(1);

    expect(apiService.cancelInvitation).toHaveBeenCalledWith(1);
  });

  it('should confirm before cancelling', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.cancelInvitation.and.returnValue(of({ success: true }));

    component.cancelInvitation(1);

    expect(window.confirm).toHaveBeenCalled();
  });

  // ==================== NOTIFICATION TESTS ====================

  it('should show notification count in header', () => {
    component.unreadCount = 2;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.notification-badge')?.textContent).toContain('2');
  });

  it('should mark invitation as read', () => {
    const invitation = mockInvitations[0];
    component.markAsRead(invitation);

    expect(component.isRead(invitation)).toBeTruthy();
  });

  it('should decrease unread count when marked as read', () => {
    component.unreadCount = 2;
    component.markAsRead(mockInvitations[0]);

    expect(component.unreadCount).toBe(1);
  });

  // ==================== UI STATE TESTS ====================

  it('should show loading indicator', () => {
    component.loading = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.loading-spinner')).toBeTruthy();
  });

  it('should show empty state when no invitations', () => {
    component.invitations = [];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.empty-state')).toBeTruthy();
  });

  it('should toggle invitation details panel', () => {
    component.toggleDetails(mockInvitations[0]);

    expect(component.expandedInvitation).toEqual(mockInvitations[0]);
  });

  // ==================== ACCESSIBILITY TESTS ====================

  it('should have accessible buttons', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const acceptButton = compiled.querySelector('button.accept-button');

    expect(acceptButton?.getAttribute('aria-label')).toBeTruthy();
  });

  it('should announce status changes', () => {
    spyOn(component, 'announceToScreenReader');
    apiService.acceptInvitation.and.returnValue(of({ success: true }));

    component.acceptInvitation(1);

    expect(component.announceToScreenReader).toHaveBeenCalled();
  });

  // ==================== REFRESH TESTS ====================

  it('should refresh invitations', () => {
    spyOn(component, 'loadInvitations');

    component.refresh();

    expect(component.loadInvitations).toHaveBeenCalled();
  });

  it('should clear messages on refresh', () => {
    component.errorMessage = 'Error';
    component.successMessage = 'Success';

    component.refresh();

    expect(component.errorMessage).toBe('');
    expect(component.successMessage).toBe('');
  });
});
