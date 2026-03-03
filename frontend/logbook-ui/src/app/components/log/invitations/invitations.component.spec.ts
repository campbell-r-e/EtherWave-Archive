import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { InvitationsComponent } from './invitations.component';
import { LogService } from '../../../services/log/log.service';
import { AuthService } from '../../../services/auth/auth.service';
import { ThemeService } from '../../../services/theme/theme.service';
import { provideRouter } from '@angular/router';
import { Invitation, InvitationStatus, ParticipantRole } from '../../../models/log.model';
import { User, UserRole } from '../../../models/auth/user.model';

const makeInvitation = (override: Partial<Invitation> = {}): Invitation => ({
  id: 10,
  logId: 1,
  logName: 'Test Log',
  inviterId: 1,
  inviterUsername: 'testuser',
  inviteeId: 2,
  inviteeUsername: 'invitee',
  proposedRole: ParticipantRole.STATION,
  status: InvitationStatus.PENDING,
  createdAt: '2024-01-01T00:00:00Z',
  canRespond: true,
  ...override,
});

describe('InvitationsComponent', () => {
  let component: InvitationsComponent;
  let fixture: ComponentFixture<InvitationsComponent>;
  let mockLogService: any;
  beforeEach(async () => {
    mockLogService = {
      logs$: new BehaviorSubject([]).asObservable(),
      getPendingInvitations: jest.fn(() => of([makeInvitation()])),
      getSentInvitations: jest.fn(() => of([])),
      getMyLogs: jest.fn(() => of([])),
      acceptInvitation: jest.fn(() => of(makeInvitation({ status: InvitationStatus.ACCEPTED }))),
      declineInvitation: jest.fn(() => of(makeInvitation({ status: InvitationStatus.DECLINED }))),
      cancelInvitation: jest.fn(() => of(makeInvitation({ status: InvitationStatus.CANCELLED }))),
      createInvitation: jest.fn(() => of(makeInvitation())),
      convertToShared: jest.fn(() => of({})),
    };
    const mockUser: User = { id: 1, username: 'testuser', roles: [UserRole.ROLE_USER], enabled: true, createdAt: '' };
    const mockAuthService = {
      currentUser: new BehaviorSubject<User | null>(mockUser).asObservable(),
      logout: jest.fn(),
    };
    const mockThemeService = { isDarkTheme: jest.fn(() => false) };

    await TestBed.configureTestingModule({
      imports: [InvitationsComponent],
      providers: [
        provideRouter([]),
        { provide: LogService, useValue: mockLogService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: ThemeService, useValue: mockThemeService },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(InvitationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('loads pending invitations on init', () => {
    expect(mockLogService.getPendingInvitations).toHaveBeenCalled();
    expect(component.pendingInvitations.length).toBe(1);
  });

  describe('acceptInvitation()', () => {
    beforeEach(() => {
      jest.spyOn(window, 'confirm').mockReturnValue(true);
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('calls logService.acceptInvitation on confirm', () => {
      const inv = makeInvitation();
      component.acceptInvitation(inv);
      expect(mockLogService.acceptInvitation).toHaveBeenCalledWith(10);
    });

    it('does not call service when user declines confirm', () => {
      jest.spyOn(window, 'confirm').mockReturnValue(false);
      component.acceptInvitation(makeInvitation());
      expect(mockLogService.acceptInvitation).not.toHaveBeenCalled();
    });

    it('sets error message on service failure', () => {
      mockLogService.acceptInvitation.mockReturnValue(
        throwError(() => ({ error: { message: 'Already accepted' } }))
      );
      component.acceptInvitation(makeInvitation());
      expect(component.error).toBe('Already accepted');
    });
  });

  describe('declineInvitation()', () => {
    beforeEach(() => {
      jest.spyOn(window, 'confirm').mockReturnValue(true);
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('calls logService.declineInvitation on confirm', () => {
      component.declineInvitation(makeInvitation());
      expect(mockLogService.declineInvitation).toHaveBeenCalledWith(10);
    });

    it('does not call service when user cancels', () => {
      jest.spyOn(window, 'confirm').mockReturnValue(false);
      component.declineInvitation(makeInvitation());
      expect(mockLogService.declineInvitation).not.toHaveBeenCalled();
    });
  });

  describe('getStatusBadgeClass()', () => {
    it('returns warning badge for PENDING', () => {
      expect(component.getStatusBadgeClass(InvitationStatus.PENDING)).toBe('badge bg-warning');
    });

    it('returns success badge for ACCEPTED', () => {
      expect(component.getStatusBadgeClass(InvitationStatus.ACCEPTED)).toBe('badge bg-success');
    });

    it('returns danger badge for DECLINED', () => {
      expect(component.getStatusBadgeClass(InvitationStatus.DECLINED)).toBe('badge bg-danger');
    });

    it('returns secondary badge for CANCELLED', () => {
      expect(component.getStatusBadgeClass(InvitationStatus.CANCELLED)).toBe('badge bg-secondary');
    });

    it('returns dark badge for EXPIRED', () => {
      expect(component.getStatusBadgeClass(InvitationStatus.EXPIRED)).toBe('badge bg-dark');
    });
  });

  describe('getRoleBadgeClass()', () => {
    it('returns primary badge for CREATOR', () => {
      expect(component.getRoleBadgeClass(ParticipantRole.CREATOR)).toBe('badge bg-primary');
    });

    it('returns success badge for STATION', () => {
      expect(component.getRoleBadgeClass(ParticipantRole.STATION)).toBe('badge bg-success');
    });

    it('returns secondary badge for VIEWER', () => {
      expect(component.getRoleBadgeClass(ParticipantRole.VIEWER)).toBe('badge bg-secondary');
    });
  });

  describe('setActiveTab()', () => {
    it('switches to sent tab', () => {
      component.setActiveTab('sent');
      expect(component.activeTab).toBe('sent');
    });

    it('switches back to received tab', () => {
      component.setActiveTab('sent');
      component.setActiveTab('received');
      expect(component.activeTab).toBe('received');
    });
  });
});
