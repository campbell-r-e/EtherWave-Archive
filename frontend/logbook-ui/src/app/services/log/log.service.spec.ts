import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { LogService } from './log.service';
import { Log, LogRequest, LogType, LogPurpose, Invitation, InvitationStatus, ParticipantRole } from '../../models/log.model';

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

describe('LogService', () => {
  let service: LogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ===========================
  // Log CRUD
  // ===========================

  describe('getMyLogs()', () => {
    it('fetches logs and updates logs$', (done) => {
      const logs = [makeLog({ id: 1 }), makeLog({ id: 2, name: 'Log 2' })];

      service.getMyLogs().subscribe((result) => {
        expect(result.length).toBe(2);
      });

      service.logs$.subscribe((l) => {
        if (l.length === 2) {
          done();
        }
      });

      httpMock.expectOne((r) => r.url.includes('/logs')).flush(logs);
    });
  });

  describe('createLog()', () => {
    it('posts and appends to logs list', () => {
      const request: LogRequest = { name: 'New Log', type: LogType.SHARED };
      const created = makeLog({ id: 99, name: 'New Log' });

      let emittedLogs: Log[] = [];
      service.logs$.subscribe((l) => (emittedLogs = l));

      service.createLog(request).subscribe();
      httpMock.expectOne((r) => r.method === 'POST').flush(created);

      expect(emittedLogs.some((l) => l.id === 99)).toBe(true);
    });

    it('auto-selects the created log', () => {
      const created = makeLog({ id: 5 });
      service.createLog({ name: 'X', type: LogType.SHARED }).subscribe();
      httpMock.expectOne((r) => r.method === 'POST').flush(created);

      expect(service.getCurrentLog()?.id).toBe(5);
    });
  });

  describe('updateLog()', () => {
    it('updates log in list', () => {
      const log = makeLog({ id: 1, name: 'Old Name' });
      (service as any).logsSubject.next([log]);

      const updated = makeLog({ id: 1, name: 'New Name' });
      service.updateLog(1, { name: 'New Name', type: LogType.SHARED }).subscribe();
      httpMock.expectOne((r) => r.method === 'PUT').flush(updated);

      const logs: Log[] = (service as any).logsSubject.value;
      expect(logs[0].name).toBe('New Name');
    });

    it('updates currentLog$ if it is the one being updated', () => {
      const log = makeLog({ id: 1 });
      service.setCurrentLog(log);

      const updated = makeLog({ id: 1, name: 'Renamed' });
      service.updateLog(1, { name: 'Renamed', type: LogType.SHARED }).subscribe();
      httpMock.expectOne((r) => r.method === 'PUT').flush(updated);

      expect(service.getCurrentLog()?.name).toBe('Renamed');
    });
  });

  describe('deleteLog()', () => {
    it('removes log from list', () => {
      const log1 = makeLog({ id: 1 });
      const log2 = makeLog({ id: 2, name: 'Log 2' });
      (service as any).logsSubject.next([log1, log2]);

      service.deleteLog(1).subscribe();
      httpMock.expectOne((r) => r.method === 'DELETE').flush(null);

      const logs: Log[] = (service as any).logsSubject.value;
      expect(logs.length).toBe(1);
      expect(logs[0].id).toBe(2);
    });

    it('clears currentLog when the deleted log was current', () => {
      const log = makeLog({ id: 1 });
      service.setCurrentLog(log);
      (service as any).logsSubject.next([log]);

      service.deleteLog(1).subscribe();
      httpMock.expectOne((r) => r.method === 'DELETE').flush(null);

      expect(service.getCurrentLog()).toBeNull();
    });
  });

  describe('freezeLog() / unfreezeLog()', () => {
    it('freezeLog updates log in state', () => {
      const log = makeLog({ id: 1, editable: true });
      (service as any).logsSubject.next([log]);
      service.setCurrentLog(log);

      const frozen = makeLog({ id: 1, editable: false });
      service.freezeLog(1).subscribe();
      httpMock.expectOne((r) => r.url.includes('/freeze')).flush(frozen);

      expect(service.getCurrentLog()?.editable).toBe(false);
    });

    it('unfreezeLog updates log in state', () => {
      const log = makeLog({ id: 1, editable: false });
      (service as any).logsSubject.next([log]);
      service.setCurrentLog(log);

      const unfrozen = makeLog({ id: 1, editable: true });
      service.unfreezeLog(1).subscribe();
      httpMock.expectOne((r) => r.url.includes('/unfreeze')).flush(unfrozen);

      expect(service.getCurrentLog()?.editable).toBe(true);
    });
  });

  describe('convertToShared()', () => {
    it('posts to convert-to-shared and updates state', () => {
      const log = makeLog({ id: 1, type: LogType.PERSONAL });
      (service as any).logsSubject.next([log]);

      const shared = makeLog({ id: 1, type: LogType.SHARED });
      service.convertToShared(1).subscribe();
      httpMock.expectOne((r) => r.url.includes('/convert-to-shared')).flush(shared);

      const logs: Log[] = (service as any).logsSubject.value;
      expect(logs[0].type).toBe(LogType.SHARED);
    });
  });

  describe('leaveLog()', () => {
    it('removes log from list', () => {
      const log = makeLog({ id: 1 });
      (service as any).logsSubject.next([log]);

      service.leaveLog(1).subscribe();
      httpMock.expectOne((r) => r.url.includes('/leave')).flush(null);

      expect((service as any).logsSubject.value.length).toBe(0);
    });

    it('clears currentLog when leaving it', () => {
      const log = makeLog({ id: 1 });
      service.setCurrentLog(log);
      (service as any).logsSubject.next([log]);

      service.leaveLog(1).subscribe();
      httpMock.expectOne((r) => r.url.includes('/leave')).flush(null);

      expect(service.getCurrentLog()).toBeNull();
    });
  });

  // ===========================
  // State management
  // ===========================

  describe('setCurrentLog() / getCurrentLog()', () => {
    it('sets and returns current log', () => {
      const log = makeLog();
      service.setCurrentLog(log);
      expect(service.getCurrentLog()?.id).toBe(1);
    });

    it('persists logId to localStorage', () => {
      service.setCurrentLog(makeLog({ id: 7 }));
      expect(localStorage.getItem('currentLogId')).toBe('7');
    });

    it('removes logId from localStorage when set to null', () => {
      localStorage.setItem('currentLogId', '7');
      service.setCurrentLog(null);
      expect(localStorage.getItem('currentLogId')).toBeNull();
    });

    it('emits on currentLog$', (done) => {
      const log = makeLog({ id: 3 });
      service.currentLog$.subscribe((l) => {
        if (l?.id === 3) done();
      });
      service.setCurrentLog(log);
    });
  });

  describe('loadCurrentLogFromStorage()', () => {
    it('fetches log by id from localStorage', () => {
      localStorage.setItem('currentLogId', '42');
      const log = makeLog({ id: 42 });

      service.loadCurrentLogFromStorage();
      const req = httpMock.expectOne((r) => r.url.includes('/logs/42'));
      req.flush(log);

      expect(service.getCurrentLog()?.id).toBe(42);
    });

    it('clears currentLogId on error', () => {
      localStorage.setItem('currentLogId', '99');

      service.loadCurrentLogFromStorage();
      const req = httpMock.expectOne((r) => r.url.includes('/logs/99'));
      req.flush('Not found', { status: 404, statusText: 'Not Found' });

      expect(localStorage.getItem('currentLogId')).toBeNull();
    });

    it('does nothing when no currentLogId in localStorage', () => {
      service.loadCurrentLogFromStorage();
      httpMock.expectNone(() => true);
    });
  });

  // ===========================
  // Invitation management
  // ===========================

  describe('getPendingInvitations()', () => {
    it('fetches pending invitations and updates count', (done) => {
      const invitations = [makeInvitation(), makeInvitation({ id: 11 })];

      service.pendingInvitationsCount$.subscribe((count) => {
        if (count === 2) done();
      });

      service.getPendingInvitations().subscribe();
      httpMock.expectOne((r) => r.url.includes('/invitations/pending')).flush(invitations);
    });
  });

  describe('getSentInvitations()', () => {
    it('fetches sent invitations', () => {
      const invitations = [makeInvitation({ status: InvitationStatus.PENDING })];
      let result: Invitation[] = [];

      service.getSentInvitations().subscribe((inv) => (result = inv));
      httpMock.expectOne((r) => r.url.includes('/invitations/sent')).flush(invitations);

      expect(result.length).toBe(1);
    });
  });

  describe('acceptInvitation()', () => {
    it('posts accept and refreshes logs and pending count', () => {
      const accepted = makeInvitation({ status: InvitationStatus.ACCEPTED });

      service.acceptInvitation(10).subscribe();

      const acceptReq = httpMock.expectOne((r) => r.url.includes('/10/accept'));
      acceptReq.flush(accepted);

      // tap triggers getMyLogs and getPendingInvitations
      httpMock.expectOne((r) => r.url.endsWith('/logs')).flush([]);
      httpMock.expectOne((r) => r.url.includes('/invitations/pending')).flush([]);
    });
  });

  describe('declineInvitation()', () => {
    it('posts decline and refreshes pending count', () => {
      const declined = makeInvitation({ status: InvitationStatus.DECLINED });

      service.declineInvitation(10).subscribe();

      const req = httpMock.expectOne((r) => r.url.includes('/10/decline'));
      req.flush(declined);

      httpMock.expectOne((r) => r.url.includes('/invitations/pending')).flush([]);
    });
  });

  describe('createInvitation()', () => {
    it('posts to invitations endpoint', () => {
      const request = {
        logId: 1,
        inviteeUsername: 'other',
        proposedRole: ParticipantRole.STATION,
      };

      service.createInvitation(request).subscribe();
      const req = httpMock.expectOne((r) => r.method === 'POST' && r.url.includes('/invitations'));
      expect(req.request.body).toEqual(request);
      req.flush(makeInvitation());
    });
  });

  describe('cancelInvitation()', () => {
    it('posts to cancel endpoint', () => {
      service.cancelInvitation(10).subscribe();
      const req = httpMock.expectOne((r) => r.url.includes('/10/cancel'));
      expect(req.request.method).toBe('POST');
      req.flush(makeInvitation({ status: InvitationStatus.CANCELLED }));
    });
  });
});
