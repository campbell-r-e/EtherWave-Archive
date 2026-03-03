import { TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { PermissionsService } from './permissions.service';
import { AuthService } from '../auth/auth.service';
import { LogService } from '../log/log.service';
import { User, UserRole } from '../../models/auth/user.model';
import { Log, LogType, ParticipantRole } from '../../models/log.model';

const makeUser = (override: Partial<User> = {}): User => ({
  id: 1,
  username: 'testuser',
  roles: [UserRole.ROLE_USER],
  enabled: true,
  createdAt: '2024-01-01T00:00:00Z',
  ...override,
});

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

describe('PermissionsService', () => {
  let service: PermissionsService;
  let userSubject: BehaviorSubject<User | null>;
  let logSubject: BehaviorSubject<Log | null>;

  beforeEach(() => {
    userSubject = new BehaviorSubject<User | null>(null);
    logSubject = new BehaviorSubject<Log | null>(null);

    const mockAuthService = {
      currentUser: userSubject.asObservable(),
    };
    const mockLogService = {
      currentLog$: logSubject.asObservable(),
    };

    TestBed.configureTestingModule({
      providers: [
        PermissionsService,
        { provide: AuthService, useValue: mockAuthService },
        { provide: LogService, useValue: mockLogService },
      ],
    });

    service = TestBed.inject(PermissionsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('with null user and null log', () => {
    it('all permissions are false', (done) => {
      service.getCurrentPermissions().subscribe((perms) => {
        expect(perms.canEditLog).toBe(false);
        expect(perms.canDeleteLog).toBe(false);
        expect(perms.canCreateQSO).toBe(false);
        expect(perms.canExportData).toBe(false);
        expect(perms.isLogCreator).toBe(false);
        expect(perms.isStationOperator).toBe(false);
        expect(perms.isViewer).toBe(false);
        done();
      });
    });
  });

  describe('with user but null log', () => {
    it('all permissions are false', (done) => {
      userSubject.next(makeUser());

      service.getCurrentPermissions().subscribe((perms) => {
        expect(perms.canCreateQSO).toBe(false);
        expect(perms.isLogCreator).toBe(false);
        done();
      });
    });
  });

  describe('CREATOR role', () => {
    beforeEach(() => {
      userSubject.next(makeUser({ id: 1 }));
      logSubject.next(makeLog({ creatorId: 1, userRole: ParticipantRole.CREATOR }));
    });

    it('has full permissions', (done) => {
      service.getCurrentPermissions().subscribe((perms) => {
        expect(perms.canEditLog).toBe(true);
        expect(perms.canDeleteLog).toBe(true);
        expect(perms.canInviteUsers).toBe(true);
        expect(perms.canManageStations).toBe(true);
        expect(perms.canManageContests).toBe(true);
        expect(perms.canCreateQSO).toBe(true);
        expect(perms.canEditQSO).toBe(true);
        expect(perms.canDeleteQSO).toBe(true);
        expect(perms.canImportData).toBe(true);
        expect(perms.canExportData).toBe(true);
        expect(perms.isLogCreator).toBe(true);
        expect(perms.isStationOperator).toBe(true);
        expect(perms.isViewer).toBe(false);
        done();
      });
    });
  });

  describe('CREATOR determined by creatorId even if userRole is not set', () => {
    it('grants creator permissions via creatorId match', (done) => {
      userSubject.next(makeUser({ id: 1 }));
      logSubject.next(makeLog({ creatorId: 1, userRole: undefined }));

      service.getCurrentPermissions().subscribe((perms) => {
        expect(perms.isLogCreator).toBe(true);
        done();
      });
    });
  });

  describe('STATION role', () => {
    beforeEach(() => {
      userSubject.next(makeUser({ id: 2 }));
      logSubject.next(makeLog({ creatorId: 1, userRole: ParticipantRole.STATION }));
    });

    it('can create/edit/delete QSOs but not manage the log', (done) => {
      service.getCurrentPermissions().subscribe((perms) => {
        expect(perms.canCreateQSO).toBe(true);
        expect(perms.canEditQSO).toBe(true);
        expect(perms.canDeleteQSO).toBe(true);
        expect(perms.canExportData).toBe(true);
        expect(perms.canEditLog).toBe(false);
        expect(perms.canDeleteLog).toBe(false);
        expect(perms.canInviteUsers).toBe(false);
        expect(perms.canImportData).toBe(false);
        expect(perms.isLogCreator).toBe(false);
        expect(perms.isStationOperator).toBe(true);
        expect(perms.isViewer).toBe(false);
        done();
      });
    });
  });

  describe('VIEWER role', () => {
    beforeEach(() => {
      userSubject.next(makeUser({ id: 3 }));
      logSubject.next(makeLog({ creatorId: 1, userRole: ParticipantRole.VIEWER }));
    });

    it('can only export data', (done) => {
      service.getCurrentPermissions().subscribe((perms) => {
        expect(perms.canCreateQSO).toBe(false);
        expect(perms.canEditQSO).toBe(false);
        expect(perms.canDeleteQSO).toBe(false);
        expect(perms.canEditLog).toBe(false);
        expect(perms.canExportData).toBe(true);
        expect(perms.canImportData).toBe(false);
        expect(perms.isLogCreator).toBe(false);
        expect(perms.isStationOperator).toBe(false);
        expect(perms.isViewer).toBe(true);
        done();
      });
    });
  });

  describe('canPerformAction()', () => {
    it('returns observable boolean for a given permission', (done) => {
      userSubject.next(makeUser({ id: 1 }));
      logSubject.next(makeLog({ creatorId: 1, userRole: ParticipantRole.CREATOR }));

      service.canPerformAction('canCreateQSO').subscribe((result) => {
        expect(result).toBe(true);
        done();
      });
    });
  });

  describe('isLogCreator()', () => {
    it('returns true for creator', (done) => {
      userSubject.next(makeUser({ id: 1 }));
      logSubject.next(makeLog({ creatorId: 1, userRole: ParticipantRole.CREATOR }));

      service.isLogCreator().subscribe((result) => {
        expect(result).toBe(true);
        done();
      });
    });
  });

  describe('isStationOperator()', () => {
    it('returns true for station operator', (done) => {
      userSubject.next(makeUser({ id: 2 }));
      logSubject.next(makeLog({ creatorId: 1, userRole: ParticipantRole.STATION }));

      service.isStationOperator().subscribe((result) => {
        expect(result).toBe(true);
        done();
      });
    });
  });
});
