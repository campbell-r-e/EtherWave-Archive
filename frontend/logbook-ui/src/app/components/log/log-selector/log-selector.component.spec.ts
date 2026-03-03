import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BehaviorSubject, of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { LogSelectorComponent } from './log-selector.component';
import { LogService } from '../../../services/log/log.service';
import { AuthService } from '../../../services/auth/auth.service';
import { Log, LogType, LogPurpose, ParticipantRole } from '../../../models/log.model';
import { User, UserRole } from '../../../models/auth/user.model';

const makeLog = (override: Partial<Log> = {}): Log => ({
  id: 1,
  name: 'Test Log',
  type: LogType.SHARED,
  creatorId: 1,
  creatorUsername: 'testuser',
  active: true,
  editable: true,
  isPublic: false,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  participantCount: 1,
  qsoCount: 0,
  ...override,
});

const makeUser = (): User => ({
  id: 1,
  username: 'testuser',
  roles: [UserRole.ROLE_USER],
  enabled: true,
  createdAt: '2024-01-01T00:00:00Z',
});

describe('LogSelectorComponent', () => {
  let component: LogSelectorComponent;
  let fixture: ComponentFixture<LogSelectorComponent>;
  let logsSubject: BehaviorSubject<Log[]>;
  let currentLogSubject: BehaviorSubject<Log | null>;
  let mockLogService: any;
  let mockAuthService: any;

  beforeEach(async () => {
    logsSubject = new BehaviorSubject<Log[]>([]);
    currentLogSubject = new BehaviorSubject<Log | null>(null);

    mockLogService = {
      logs$: logsSubject.asObservable(),
      currentLog$: currentLogSubject.asObservable(),
      pendingInvitationsCount$: new BehaviorSubject(0).asObservable(),
      getMyLogs: jest.fn(() => of([])),
      createLog: jest.fn(() => of(makeLog())),
      deleteLog: jest.fn(() => of(void 0)),
      leaveLog: jest.fn(() => of(void 0)),
      setCurrentLog: jest.fn(),
      getPendingInvitations: jest.fn(() => of([])),
    };

    mockAuthService = {
      currentUser: new BehaviorSubject<User | null>(makeUser()).asObservable(),
    };

    await TestBed.configureTestingModule({
      imports: [LogSelectorComponent],
      providers: [
        provideRouter([]),
        { provide: LogService, useValue: mockLogService },
        { provide: AuthService, useValue: mockAuthService },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(LogSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  describe('personalLogs getter', () => {
    it('returns only PERSONAL type logs', () => {
      logsSubject.next([
        makeLog({ id: 1, type: LogType.PERSONAL }),
        makeLog({ id: 2, type: LogType.SHARED }),
        makeLog({ id: 3, type: LogType.PERSONAL }),
      ]);
      expect(component.personalLogs.length).toBe(2);
      expect(component.personalLogs.every((l) => l.type === LogType.PERSONAL)).toBe(true);
    });

    it('returns empty array when no personal logs', () => {
      logsSubject.next([makeLog({ type: LogType.SHARED })]);
      expect(component.personalLogs).toEqual([]);
    });
  });

  describe('sharedLogs getter', () => {
    it('returns only SHARED type logs', () => {
      logsSubject.next([
        makeLog({ id: 1, type: LogType.PERSONAL }),
        makeLog({ id: 2, type: LogType.SHARED }),
      ]);
      expect(component.sharedLogs.length).toBe(1);
      expect(component.sharedLogs[0].type).toBe(LogType.SHARED);
    });
  });

  describe('openCreatePersonalLog()', () => {
    it('sets creatingType to PERSONAL', () => {
      component.openCreatePersonalLog();
      expect(component.creatingType).toBe(LogType.PERSONAL);
      expect(component.showCreateModal).toBe(true);
    });
  });

  describe('openCreateSharedLog()', () => {
    it('sets creatingType to SHARED', () => {
      component.openCreateSharedLog();
      expect(component.creatingType).toBe(LogType.SHARED);
      expect(component.showCreateModal).toBe(true);
    });
  });

  describe('createLog()', () => {
    it('forces isPublic=false for PERSONAL type', () => {
      component.creatingType = LogType.PERSONAL;
      component.createLogForm.patchValue({
        name: 'My Log',
        purpose: LogPurpose.GENERAL,
        isPublic: true, // user tries to set public but it should be forced false
      });

      component.createLog();

      expect(mockLogService.createLog).toHaveBeenCalledWith(
        expect.objectContaining({ type: LogType.PERSONAL, isPublic: false })
      );
    });

    it('allows isPublic=true for SHARED type', () => {
      component.creatingType = LogType.SHARED;
      component.createLogForm.patchValue({
        name: 'Shared Log',
        purpose: LogPurpose.GENERAL,
        isPublic: true,
      });

      component.createLog();

      expect(mockLogService.createLog).toHaveBeenCalledWith(
        expect.objectContaining({ type: LogType.SHARED, isPublic: true })
      );
    });

    it('does not call createLog when form is invalid', () => {
      component.createLogForm.reset(); // clears name, making form invalid
      component.createLog();
      expect(mockLogService.createLog).not.toHaveBeenCalled();
    });
  });

  describe('canDelete()', () => {
    it('returns true for CREATOR role', () => {
      expect(component.canDelete(makeLog({ userRole: ParticipantRole.CREATOR }))).toBe(true);
    });

    it('returns false for STATION role', () => {
      expect(component.canDelete(makeLog({ userRole: ParticipantRole.STATION }))).toBe(false);
    });

    it('returns false for VIEWER role', () => {
      expect(component.canDelete(makeLog({ userRole: ParticipantRole.VIEWER }))).toBe(false);
    });
  });

  describe('canLeave()', () => {
    it('returns false for CREATOR role', () => {
      expect(component.canLeave(makeLog({ userRole: ParticipantRole.CREATOR }))).toBe(false);
    });

    it('returns true for STATION role', () => {
      expect(component.canLeave(makeLog({ userRole: ParticipantRole.STATION }))).toBe(true);
    });
  });

  describe('getRoleBadgeClass()', () => {
    it('returns primary badge for CREATOR', () => {
      expect(component.getRoleBadgeClass('CREATOR')).toBe('badge bg-primary');
    });

    it('returns success badge for STATION', () => {
      expect(component.getRoleBadgeClass('STATION')).toBe('badge bg-success');
    });

    it('returns secondary badge for VIEWER', () => {
      expect(component.getRoleBadgeClass('VIEWER')).toBe('badge bg-secondary');
    });

    it('returns light badge for unknown role', () => {
      expect(component.getRoleBadgeClass('UNKNOWN')).toBe('badge bg-light');
    });
  });

  describe('getPurposeLabel()', () => {
    it('returns "General" for GENERAL', () => {
      expect(component.getPurposeLabel(LogPurpose.GENERAL)).toBe('General');
    });

    it('returns "Field Day" for FIELD_DAY', () => {
      expect(component.getPurposeLabel(LogPurpose.FIELD_DAY)).toBe('Field Day');
    });

    it('returns "POTA" for POTA', () => {
      expect(component.getPurposeLabel(LogPurpose.POTA)).toBe('POTA');
    });

    it('returns "SOTA" for SOTA', () => {
      expect(component.getPurposeLabel(LogPurpose.SOTA)).toBe('SOTA');
    });

    it('returns "General" when purpose is undefined', () => {
      expect(component.getPurposeLabel(undefined)).toBe('General');
    });

    it('returns "State QSO" for STATE_QSO_PARTY', () => {
      expect(component.getPurposeLabel(LogPurpose.STATE_QSO_PARTY)).toBe('State QSO');
    });
  });

  describe('purposeAutoLinksContest()', () => {
    it('returns contest name for FIELD_DAY', () => {
      expect(component.purposeAutoLinksContest(LogPurpose.FIELD_DAY)).toBe('ARRL Field Day');
    });

    it('returns "POTA" for POTA', () => {
      expect(component.purposeAutoLinksContest(LogPurpose.POTA)).toBe('POTA');
    });

    it('returns null for GENERAL', () => {
      expect(component.purposeAutoLinksContest(LogPurpose.GENERAL)).toBeNull();
    });

    it('returns null when purpose is null', () => {
      expect(component.purposeAutoLinksContest(null)).toBeNull();
    });

    it('returns "CQ WW" for CQ_WW', () => {
      expect(component.purposeAutoLinksContest(LogPurpose.CQ_WW)).toBe('CQ WW');
    });

    it('returns "ARRL Sweepstakes" for SWEEPSTAKES', () => {
      expect(component.purposeAutoLinksContest(LogPurpose.SWEEPSTAKES)).toBe('ARRL Sweepstakes');
    });

    it('returns "Winter Field Day" for WINTER_FIELD_DAY', () => {
      expect(component.purposeAutoLinksContest(LogPurpose.WINTER_FIELD_DAY)).toBe('Winter Field Day');
    });
  });
});
