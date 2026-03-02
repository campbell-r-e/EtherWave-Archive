import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { ImportPanelComponent } from './import-panel.component';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log/log.service';
import { LotwService, LotwSyncResult } from '../../services/lotw/lotw.service';
import { Log, LogType } from '../../models/log.model';

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

describe('ImportPanelComponent', () => {
  let component: ImportPanelComponent;
  let fixture: ComponentFixture<ImportPanelComponent>;
  let logSubject: BehaviorSubject<Log | null>;
  let mockApiService: { getStations: jest.Mock; importAdif: jest.Mock; previewAdifFile: jest.Mock; importAdifWithMapping: jest.Mock };
  let mockLotwService: { sync: jest.Mock };

  beforeEach(async () => {
    logSubject = new BehaviorSubject<Log | null>(null);
    mockApiService = {
      getStations: jest.fn(() => of([])),
      importAdif: jest.fn(() => of({})),
      previewAdifFile: jest.fn(() => of({})),
      importAdifWithMapping: jest.fn(() => of({})),
    };
    mockLotwService = {
      sync: jest.fn(() =>
        of<LotwSyncResult>({ downloaded: 50, matched: 30, updated: 25, message: 'Sync complete' })
      ),
    };

    await TestBed.configureTestingModule({
      imports: [ImportPanelComponent],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        { provide: LogService, useValue: { currentLog$: logSubject.asObservable() } },
        { provide: LotwService, useValue: mockLotwService },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(ImportPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  describe('syncLotw()', () => {
    it('sets lotwError when no log is selected', () => {
      component.lotwCallsign = 'W1AW';
      component.lotwPassword = 'secret';
      component.syncLotw();
      expect(component.lotwError).toBe('Select a log first.');
      expect(mockLotwService.sync).not.toHaveBeenCalled();
    });

    it('sets lotwError when callsign is empty', () => {
      logSubject.next(makeLog());
      component.lotwCallsign = '';
      component.lotwPassword = 'secret';
      component.syncLotw();
      expect(component.lotwError).toBe('LoTW callsign is required.');
    });

    it('sets lotwError when password is empty', () => {
      logSubject.next(makeLog());
      component.lotwCallsign = 'W1AW';
      component.lotwPassword = '';
      component.syncLotw();
      expect(component.lotwError).toBe('LoTW password is required.');
    });

    it('calls sync with correct params on valid input', () => {
      logSubject.next(makeLog({ id: 7 }));
      component.lotwCallsign = 'w1aw';
      component.lotwPassword = 'secret';
      component.lotwSince = '2024-01-01';

      component.syncLotw();

      expect(mockLotwService.sync).toHaveBeenCalledWith(7, {
        lotwCallsign: 'W1AW', // uppercased
        lotwPassword: 'secret',
        since: '2024-01-01',
      });
    });

    it('clears password after successful sync', () => {
      logSubject.next(makeLog());
      component.lotwCallsign = 'W1AW';
      component.lotwPassword = 'secret';
      component.syncLotw();
      expect(component.lotwPassword).toBe('');
    });

    it('stores sync result', () => {
      logSubject.next(makeLog());
      component.lotwCallsign = 'W1AW';
      component.lotwPassword = 'secret';
      component.syncLotw();
      expect(component.lotwResult?.matched).toBe(30);
    });

    it('clears password and sets error on failure', () => {
      mockLotwService.sync.mockReturnValue(
        throwError(() => ({ error: { message: 'Auth failed' } }))
      );
      logSubject.next(makeLog());
      component.lotwCallsign = 'W1AW';
      component.lotwPassword = 'secret';
      component.syncLotw();
      expect(component.lotwPassword).toBe('');
      expect(component.lotwError).toBe('Auth failed');
    });

    it('omits "since" when lotwSince is empty', () => {
      logSubject.next(makeLog({ id: 1 }));
      component.lotwCallsign = 'W1AW';
      component.lotwPassword = 'pw';
      component.lotwSince = '';
      component.syncLotw();
      expect(mockLotwService.sync).toHaveBeenCalledWith(
        1,
        expect.objectContaining({ since: undefined })
      );
    });
  });

  describe('cancelLotwSync()', () => {
    it('resets all LoTW state', () => {
      component.showLotwSync = true;
      component.lotwCallsign = 'W1AW';
      component.lotwPassword = 'secret';
      component.lotwSince = '2024-01-01';
      component.lotwResult = { downloaded: 5, matched: 5, updated: 5, message: 'ok' };
      component.lotwError = 'some error';

      component.cancelLotwSync();

      expect(component.showLotwSync).toBe(false);
      expect(component.lotwCallsign).toBe('');
      expect(component.lotwPassword).toBe('');
      expect(component.lotwSince).toBe('');
      expect(component.lotwResult).toBeNull();
      expect(component.lotwError).toBeNull();
    });
  });

  describe('resetImport()', () => {
    it('clears file and result state', () => {
      component.selectedFile = new File([''], 'test.adif');
      component.importResult = { total: 10 };
      component.errorMessage = 'some error';

      component.resetImport();

      expect(component.selectedFile).toBeNull();
      expect(component.importResult).toBeNull();
      expect(component.errorMessage).toBeNull();
    });
  });

  describe('getLogName()', () => {
    it('returns "No log selected" when no log', () => {
      expect(component.getLogName()).toBe('No log selected');
    });

    it('returns contestName when set', () => {
      logSubject.next(makeLog({ contestName: 'ARRL Field Day' }));
      expect(component.getLogName()).toBe('ARRL Field Day');
    });

    it('returns "Personal Log" when no contestName', () => {
      logSubject.next(makeLog({ contestName: undefined }));
      expect(component.getLogName()).toBe('Personal Log');
    });
  });
});
