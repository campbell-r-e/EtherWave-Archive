import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BehaviorSubject, of } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { AuthService } from '../../services/auth/auth.service';
import { LogService } from '../../services/log/log.service';
import { ThemeService } from '../../services/theme/theme.service';
import { PermissionsService } from '../../services/permissions/permissions.service';
import { Router } from '@angular/router';
import { User, UserRole } from '../../models/auth/user.model';
import { Log, LogType } from '../../models/log.model';

const makeUser = (): User => ({
  id: 1,
  username: 'testuser',
  roles: [UserRole.ROLE_USER],
  enabled: true,
  createdAt: '2024-01-01T00:00:00Z',
});

const makeLog = (): Log => ({
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
});

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let userSubject: BehaviorSubject<User | null>;
  let logSubject: BehaviorSubject<Log | null>;

  beforeEach(async () => {
    userSubject = new BehaviorSubject<User | null>(null);
    logSubject = new BehaviorSubject<Log | null>(null);

    const mockAuthService = {
      currentUser: userSubject.asObservable(),
      logout: jest.fn(),
    };
    const mockLogService = {
      currentLog$: logSubject.asObservable(),
      logs$: new BehaviorSubject<Log[]>([]).asObservable(),
      pendingInvitationsCount$: new BehaviorSubject(0).asObservable(),
      loadCurrentLogFromStorage: jest.fn(),
      getMyLogs: jest.fn(() => of([])),
      getPendingInvitations: jest.fn(() => of([])),
      getLogParticipants: jest.fn(() => of([])),
    };
    const mockPermissionsService = {
      getCurrentPermissions: jest.fn(() =>
        of({
          canEditLog: false,
          canDeleteLog: false,
          canInviteUsers: false,
          canManageStations: false,
          canManageContests: false,
          canCreateQSO: false,
          canEditQSO: false,
          canDeleteQSO: false,
          canViewContestOverlays: false,
          canManageContestData: false,
          canExportMapData: false,
          canImportData: false,
          canExportData: false,
          isLogCreator: false,
          isStationOperator: false,
          isViewer: false,
        })
      ),
    };
    const mockThemeService = { isDarkTheme: jest.fn(() => false), getCurrentTheme: jest.fn(() => 'light') };
    const mockRouter = { navigate: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: LogService, useValue: mockLogService },
        { provide: ThemeService, useValue: mockThemeService },
        { provide: PermissionsService, useValue: mockPermissionsService },
        { provide: Router, useValue: mockRouter },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    })
      .overrideComponent(DashboardComponent, { set: { imports: [] } })
      .compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('tracks current log from LogService', () => {
    const log = makeLog();
    logSubject.next(log);
    expect(component.currentLog?.id).toBe(1);
  });

  it('tracks current user from AuthService', () => {
    userSubject.next(makeUser());
    expect(component.currentUser?.username).toBe('testuser');
  });

  describe('getStationAssignmentText()', () => {
    it('returns empty string when no participant', () => {
      expect(component.getStationAssignmentText()).toBe('');
    });

    it('returns "GOTA" for GOTA participant', () => {
      component.currentParticipant = {
        id: 1, logId: 1, logName: 'Log', userId: 1, username: 'u',
        role: 'STATION' as any, isGota: true, active: true, joinedAt: '',
      };
      expect(component.getStationAssignmentText()).toBe('GOTA');
    });

    it('returns "Station N" for numbered station', () => {
      component.currentParticipant = {
        id: 1, logId: 1, logName: 'Log', userId: 1, username: 'u',
        role: 'STATION' as any, isGota: false, stationNumber: 3, active: true, joinedAt: '',
      };
      expect(component.getStationAssignmentText()).toBe('Station 3');
    });
  });

  describe('toggleMap() / closeMap()', () => {
    it('toggles isMapOpen', () => {
      expect(component.isMapOpen).toBe(false);
      component.toggleMap();
      expect(component.isMapOpen).toBe(true);
      component.toggleMap();
      expect(component.isMapOpen).toBe(false);
    });

    it('closeMap sets isMapOpen to false', () => {
      component.isMapOpen = true;
      component.closeMap();
      expect(component.isMapOpen).toBe(false);
    });
  });
});
