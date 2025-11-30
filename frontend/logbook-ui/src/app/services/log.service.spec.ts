import { TestBed } from '@angular/core/testing';
import { LogService } from './log.service';
import { BehaviorSubject } from 'rxjs';

describe('LogService', () => {
  let service: LogService;

  const mockLog = {
    id: 1,
    name: 'Field Day 2025',
    contestCode: 'ARRL-FD',
    createdAt: '2025-01-15T10:00:00',
    isFrozen: false,
    qsoCount: 150
  };

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LogService);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ==================== CURRENT LOG TESTS ====================

  it('should set current log', () => {
    service.setCurrentLog(mockLog);

    expect(service.getCurrentLog()).toEqual(mockLog);
  });

  it('should emit current log change', (done) => {
    service.currentLog$.subscribe(log => {
      if (log) {
        expect(log).toEqual(mockLog);
        done();
      }
    });

    service.setCurrentLog(mockLog);
  });

  it('should persist current log to localStorage', () => {
    service.setCurrentLog(mockLog);

    const stored = localStorage.getItem('currentLog');
    expect(stored).toBeTruthy();
    expect(JSON.parse(stored!)).toEqual(mockLog);
  });

  it('should load current log from localStorage', () => {
    localStorage.setItem('currentLog', JSON.stringify(mockLog));

    const log = service.getCurrentLog();

    expect(log).toEqual(mockLog);
  });

  it('should return null when no current log', () => {
    expect(service.getCurrentLog()).toBeNull();
  });

  it('should clear current log', () => {
    service.setCurrentLog(mockLog);
    service.clearCurrentLog();

    expect(service.getCurrentLog()).toBeNull();
    expect(localStorage.getItem('currentLog')).toBeNull();
  });

  // ==================== LOG ID TESTS ====================

  it('should get current log ID', () => {
    service.setCurrentLog(mockLog);

    expect(service.getCurrentLogId()).toBe(1);
  });

  it('should return null when no log ID', () => {
    expect(service.getCurrentLogId()).toBeNull();
  });

  // ==================== LOG NAME TESTS ====================

  it('should get current log name', () => {
    service.setCurrentLog(mockLog);

    expect(service.getCurrentLogName()).toBe('Field Day 2025');
  });

  it('should return empty string when no log name', () => {
    expect(service.getCurrentLogName()).toBe('');
  });

  // ==================== CONTEST MODE TESTS ====================

  it('should set contest mode', () => {
    service.setContestMode('ARRL-FD');

    expect(service.getContestMode()).toBe('ARRL-FD');
  });

  it('should emit contest mode change', (done) => {
    service.contestMode$.subscribe(mode => {
      if (mode) {
        expect(mode).toBe('ARRL-FD');
        done();
      }
    });

    service.setContestMode('ARRL-FD');
  });

  it('should persist contest mode to localStorage', () => {
    service.setContestMode('ARRL-FD');

    const stored = localStorage.getItem('contestMode');
    expect(stored).toBe('ARRL-FD');
  });

  it('should load contest mode from localStorage', () => {
    localStorage.setItem('contestMode', 'WFD');

    const mode = service.getContestMode();

    expect(mode).toBe('WFD');
  });

  it('should return null when no contest mode', () => {
    expect(service.getContestMode()).toBeNull();
  });

  it('should clear contest mode', () => {
    service.setContestMode('ARRL-FD');
    service.clearContestMode();

    expect(service.getContestMode()).toBeNull();
    expect(localStorage.getItem('contestMode')).toBeNull();
  });

  // ==================== CONTEST PARAMETERS TESTS ====================

  it('should set contest parameters', () => {
    const params = {
      contestCode: 'ARRL-FD',
      class: '2A',
      section: 'ORG',
      power: 'BATTERY'
    };

    service.setContestParameters(params);

    expect(service.getContestParameters()).toEqual(params);
  });

  it('should emit contest parameters change', (done) => {
    const params = {
      contestCode: 'ARRL-FD',
      class: '2A',
      section: 'ORG'
    };

    service.contestParameters$.subscribe(p => {
      if (p) {
        expect(p).toEqual(params);
        done();
      }
    });

    service.setContestParameters(params);
  });

  it('should persist contest parameters to localStorage', () => {
    const params = { contestCode: 'ARRL-FD', class: '2A', section: 'ORG' };

    service.setContestParameters(params);

    const stored = localStorage.getItem('contestParameters');
    expect(stored).toBeTruthy();
    expect(JSON.parse(stored!)).toEqual(params);
  });

  it('should load contest parameters from localStorage', () => {
    const params = { contestCode: 'ARRL-FD', class: '2A', section: 'ORG' };
    localStorage.setItem('contestParameters', JSON.stringify(params));

    const loaded = service.getContestParameters();

    expect(loaded).toEqual(params);
  });

  it('should return null when no contest parameters', () => {
    expect(service.getContestParameters()).toBeNull();
  });

  it('should clear contest parameters', () => {
    const params = { contestCode: 'ARRL-FD', class: '2A', section: 'ORG' };
    service.setContestParameters(params);
    service.clearContestParameters();

    expect(service.getContestParameters()).toBeNull();
    expect(localStorage.getItem('contestParameters')).toBeNull();
  });

  // ==================== FROZEN LOG TESTS ====================

  it('should detect frozen log', () => {
    service.setCurrentLog({ ...mockLog, isFrozen: true });

    expect(service.isLogFrozen()).toBeTruthy();
  });

  it('should detect unfrozen log', () => {
    service.setCurrentLog({ ...mockLog, isFrozen: false });

    expect(service.isLogFrozen()).toBeFalsy();
  });

  it('should return false when no log', () => {
    expect(service.isLogFrozen()).toBeFalsy();
  });

  // ==================== QSO COUNT TESTS ====================

  it('should get QSO count', () => {
    service.setCurrentLog(mockLog);

    expect(service.getQSOCount()).toBe(150);
  });

  it('should return 0 when no log', () => {
    expect(service.getQSOCount()).toBe(0);
  });

  it('should update QSO count', () => {
    service.setCurrentLog(mockLog);
    service.updateQSOCount(200);

    expect(service.getQSOCount()).toBe(200);
  });

  it('should increment QSO count', () => {
    service.setCurrentLog(mockLog);
    service.incrementQSOCount();

    expect(service.getQSOCount()).toBe(151);
  });

  it('should decrement QSO count', () => {
    service.setCurrentLog(mockLog);
    service.decrementQSOCount();

    expect(service.getQSOCount()).toBe(149);
  });

  it('should not decrement below zero', () => {
    service.setCurrentLog({ ...mockLog, qsoCount: 0 });
    service.decrementQSOCount();

    expect(service.getQSOCount()).toBe(0);
  });

  // ==================== STATION LOCATION TESTS ====================

  it('should set station location', () => {
    const location = { latitude: 40.0, longitude: -74.0, gridSquare: 'FN30' };

    service.setStationLocation(location);

    expect(service.getStationLocation()).toEqual(location);
  });

  it('should emit station location change', (done) => {
    const location = { latitude: 40.0, longitude: -74.0, gridSquare: 'FN30' };

    service.stationLocation$.subscribe(loc => {
      if (loc) {
        expect(loc).toEqual(location);
        done();
      }
    });

    service.setStationLocation(location);
  });

  it('should persist station location to localStorage', () => {
    const location = { latitude: 40.0, longitude: -74.0, gridSquare: 'FN30' };

    service.setStationLocation(location);

    const stored = localStorage.getItem('stationLocation');
    expect(stored).toBeTruthy();
    expect(JSON.parse(stored!)).toEqual(location);
  });

  it('should load station location from localStorage', () => {
    const location = { latitude: 40.0, longitude: -74.0, gridSquare: 'FN30' };
    localStorage.setItem('stationLocation', JSON.stringify(location));

    const loaded = service.getStationLocation();

    expect(loaded).toEqual(location);
  });

  it('should return null when no station location', () => {
    expect(service.getStationLocation()).toBeNull();
  });

  it('should clear station location', () => {
    const location = { latitude: 40.0, longitude: -74.0, gridSquare: 'FN30' };
    service.setStationLocation(location);
    service.clearStationLocation();

    expect(service.getStationLocation()).toBeNull();
    expect(localStorage.getItem('stationLocation')).toBeNull();
  });

  // ==================== QSO REFRESH TESTS ====================

  it('should trigger QSO refresh', (done) => {
    service.qsoRefresh$.subscribe(() => {
      expect(true).toBeTruthy();
      done();
    });

    service.refreshQSOs();
  });

  it('should notify multiple subscribers', () => {
    let count = 0;

    service.qsoRefresh$.subscribe(() => count++);
    service.qsoRefresh$.subscribe(() => count++);

    service.refreshQSOs();

    expect(count).toBe(2);
  });

  // ==================== LOG LIST TESTS ====================

  it('should set log list', () => {
    const logs = [mockLog, { ...mockLog, id: 2, name: 'Winter Field Day' }];

    service.setLogList(logs);

    expect(service.getLogList()).toEqual(logs);
  });

  it('should emit log list change', (done) => {
    const logs = [mockLog];

    service.logList$.subscribe(list => {
      if (list && list.length > 0) {
        expect(list).toEqual(logs);
        done();
      }
    });

    service.setLogList(logs);
  });

  it('should return empty array when no log list', () => {
    expect(service.getLogList()).toEqual([]);
  });

  it('should find log by ID', () => {
    const logs = [mockLog, { ...mockLog, id: 2, name: 'Winter Field Day' }];
    service.setLogList(logs);

    const log = service.findLogById(2);

    expect(log?.name).toBe('Winter Field Day');
  });

  it('should return undefined when log not found', () => {
    const logs = [mockLog];
    service.setLogList(logs);

    const log = service.findLogById(999);

    expect(log).toBeUndefined();
  });

  // ==================== PREFERENCES TESTS ====================

  it('should set user preference', () => {
    service.setPreference('theme', 'dark');

    expect(service.getPreference('theme')).toBe('dark');
  });

  it('should persist preference to localStorage', () => {
    service.setPreference('theme', 'dark');

    const stored = localStorage.getItem('preferences');
    expect(stored).toBeTruthy();
    const prefs = JSON.parse(stored!);
    expect(prefs.theme).toBe('dark');
  });

  it('should load preferences from localStorage', () => {
    localStorage.setItem('preferences', JSON.stringify({ theme: 'dark', compactMode: true }));

    expect(service.getPreference('theme')).toBe('dark');
    expect(service.getPreference('compactMode')).toBeTruthy();
  });

  it('should return null for non-existent preference', () => {
    expect(service.getPreference('nonexistent')).toBeNull();
  });

  it('should clear all preferences', () => {
    service.setPreference('theme', 'dark');
    service.setPreference('compactMode', true);
    service.clearPreferences();

    expect(service.getPreference('theme')).toBeNull();
    expect(localStorage.getItem('preferences')).toBeNull();
  });

  // ==================== VALIDATION TESTS ====================

  it('should validate log data', () => {
    const validLog = { id: 1, name: 'Test Log', contestCode: 'ARRL-FD' };

    expect(service.isValidLog(validLog)).toBeTruthy();
  });

  it('should reject invalid log data', () => {
    const invalidLog = { id: null, name: '', contestCode: null };

    expect(service.isValidLog(invalidLog)).toBeFalsy();
  });

  it('should validate contest code', () => {
    expect(service.isValidContestCode('ARRL-FD')).toBeTruthy();
    expect(service.isValidContestCode('WFD')).toBeTruthy();
    expect(service.isValidContestCode('')).toBeFalsy();
  });

  // ==================== RESET TESTS ====================

  it('should reset all state', () => {
    service.setCurrentLog(mockLog);
    service.setContestMode('ARRL-FD');
    service.setStationLocation({ latitude: 40.0, longitude: -74.0, gridSquare: 'FN30' });

    service.resetAllState();

    expect(service.getCurrentLog()).toBeNull();
    expect(service.getContestMode()).toBeNull();
    expect(service.getStationLocation()).toBeNull();
  });

  it('should clear all localStorage items', () => {
    service.setCurrentLog(mockLog);
    service.setContestMode('ARRL-FD');
    service.setPreference('theme', 'dark');

    service.resetAllState();

    expect(localStorage.getItem('currentLog')).toBeNull();
    expect(localStorage.getItem('contestMode')).toBeNull();
    expect(localStorage.getItem('preferences')).toBeNull();
  });

  // ==================== ERROR HANDLING TESTS ====================

  it('should handle corrupted localStorage data', () => {
    localStorage.setItem('currentLog', 'invalid-json');

    expect(() => service.getCurrentLog()).not.toThrow();
    expect(service.getCurrentLog()).toBeNull();
  });

  it('should handle missing localStorage keys', () => {
    expect(() => service.getCurrentLog()).not.toThrow();
    expect(service.getCurrentLog()).toBeNull();
  });

  it('should handle null values gracefully', () => {
    service.setCurrentLog(null as any);

    expect(service.getCurrentLog()).toBeNull();
  });

  // ==================== OBSERVABLE TESTS ====================

  it('should provide current log as observable', (done) => {
    service.currentLog$.subscribe(log => {
      if (log) {
        expect(log.id).toBe(1);
        done();
      }
    });

    service.setCurrentLog(mockLog);
  });

  it('should provide contest mode as observable', (done) => {
    service.contestMode$.subscribe(mode => {
      if (mode) {
        expect(mode).toBe('ARRL-FD');
        done();
      }
    });

    service.setContestMode('ARRL-FD');
  });

  it('should handle multiple observers', () => {
    let observer1Called = false;
    let observer2Called = false;

    service.currentLog$.subscribe(() => observer1Called = true);
    service.currentLog$.subscribe(() => observer2Called = true);

    service.setCurrentLog(mockLog);

    expect(observer1Called).toBeTruthy();
    expect(observer2Called).toBeTruthy();
  });

  // ==================== INITIALIZATION TESTS ====================

  it('should initialize from localStorage on creation', () => {
    localStorage.setItem('currentLog', JSON.stringify(mockLog));
    localStorage.setItem('contestMode', 'ARRL-FD');

    const newService = new LogService();

    expect(newService.getCurrentLog()).toEqual(mockLog);
    expect(newService.getContestMode()).toBe('ARRL-FD');
  });

  it('should handle empty localStorage on initialization', () => {
    const newService = new LogService();

    expect(newService.getCurrentLog()).toBeNull();
    expect(newService.getContestMode()).toBeNull();
  });
});
