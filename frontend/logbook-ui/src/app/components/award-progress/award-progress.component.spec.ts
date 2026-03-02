import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { AwardProgressComponent } from './award-progress.component';
import { LogService } from '../../services/log/log.service';
import { AwardService, AwardProgress } from '../../services/award/award.service';
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

const mockProgress: AwardProgress = {
  logId: 1,
  logName: 'Test Log',
  totalQsos: 50,
  dxcc: {
    workedCountries: [],
    confirmedCountries: [],
    workedCount: 170,
    confirmedCount: 85,
    totalEntities: 340,
  },
  was: {
    workedStates: [],
    confirmedStates: [],
    workedCount: 35,
    confirmedCount: 20,
    totalStates: 50,
  },
  vucc: {
    workedGrids: [],
    confirmedGrids: [],
    workedCount: 120,
    confirmedCount: 80,
    threshold: 100,
  },
};

describe('AwardProgressComponent', () => {
  let component: AwardProgressComponent;
  let fixture: ComponentFixture<AwardProgressComponent>;
  let logSubject: BehaviorSubject<Log | null>;
  let mockAwardService: { getProgress: jest.Mock };

  beforeEach(async () => {
    logSubject = new BehaviorSubject<Log | null>(null);
    mockAwardService = { getProgress: jest.fn(() => of(mockProgress)) };

    await TestBed.configureTestingModule({
      imports: [AwardProgressComponent],
      providers: [
        { provide: LogService, useValue: { currentLog$: logSubject.asObservable() } },
        { provide: AwardService, useValue: mockAwardService },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(AwardProgressComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  describe('pct()', () => {
    it('returns 0 when total is 0', () => {
      expect(component.pct(5, 0)).toBe(0);
    });

    it('returns 0 for worked=0', () => {
      expect(component.pct(0, 100)).toBe(0);
    });

    it('calculates mid-range percentage', () => {
      expect(component.pct(50, 100)).toBe(50);
    });

    it('returns 100 when worked equals total', () => {
      expect(component.pct(100, 100)).toBe(100);
    });

    it('caps at 100 when worked exceeds total', () => {
      expect(component.pct(110, 100)).toBe(100);
    });

    it('rounds to nearest integer', () => {
      expect(component.pct(1, 3)).toBe(33);
    });
  });

  describe('progressColor()', () => {
    it('returns bg-danger for 0%', () => {
      expect(component.progressColor(0)).toBe('bg-danger');
    });

    it('returns bg-warning for 33%', () => {
      expect(component.progressColor(33)).toBe('bg-warning');
    });

    it('returns bg-info for 66%', () => {
      expect(component.progressColor(66)).toBe('bg-info');
    });

    it('returns bg-success for 100%', () => {
      expect(component.progressColor(100)).toBe('bg-success');
    });

    it('returns bg-warning for 32%', () => {
      expect(component.progressColor(32)).toBe('bg-danger');
    });

    it('returns bg-info for 65%', () => {
      expect(component.progressColor(65)).toBe('bg-warning');
    });
  });

  describe('ngOnInit()', () => {
    it('loads progress when log is emitted', () => {
      logSubject.next(makeLog({ id: 1 }));
      expect(mockAwardService.getProgress).toHaveBeenCalledWith(1);
      expect(component.progress).toEqual(mockProgress);
    });

    it('clears progress when log becomes null', () => {
      logSubject.next(makeLog());
      logSubject.next(null);
      expect(component.progress).toBeNull();
    });

    it('shows error when service fails', () => {
      mockAwardService.getProgress.mockReturnValue(throwError(() => new Error('Network error')));
      logSubject.next(makeLog());
      expect(component.error).toBe('Failed to load award progress.');
      expect(component.loading).toBe(false);
    });

    it('sets loading to false on success', () => {
      logSubject.next(makeLog());
      expect(component.loading).toBe(false);
    });
  });
});
