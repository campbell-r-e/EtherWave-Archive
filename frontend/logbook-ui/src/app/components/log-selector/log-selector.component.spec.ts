import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { LogSelectorComponent } from './log-selector.component';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log.service';
import { Router } from '@angular/router';

describe('LogSelectorComponent', () => {
  let component: LogSelectorComponent;
  let fixture: ComponentFixture<LogSelectorComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let logService: jasmine.SpyObj<LogService>;
  let router: jasmine.SpyObj<Router>;

  const mockLogs = [
    { id: 1, name: 'Field Day 2025', contestCode: 'ARRL-FD', createdAt: '2025-01-15', qsoCount: 150 },
    { id: 2, name: 'Winter Field Day', contestCode: 'WFD', createdAt: '2025-01-10', qsoCount: 75 },
    { id: 3, name: 'POTA Activation', contestCode: 'POTA', createdAt: '2025-01-05', qsoCount: 25 }
  ];

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'getLogs', 'createLog', 'deleteLog', 'updateLog', 'getSharedLogs'
    ]);
    const logServiceSpy = jasmine.createSpyObj('LogService', ['setCurrentLog', 'getCurrentLog']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [LogSelectorComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: LogService, useValue: logServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    logService = TestBed.inject(LogService) as jasmine.SpyObj<LogService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    apiService.getLogs.and.returnValue(of(mockLogs));
    apiService.getSharedLogs.and.returnValue(of([]));

    fixture = TestBed.createComponent(LogSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== LOG LOADING TESTS ====================

  it('should load user logs on init', () => {
    expect(apiService.getLogs).toHaveBeenCalled();
    expect(component.logs.length).toBe(3);
  });

  it('should load shared logs on init', () => {
    expect(apiService.getSharedLogs).toHaveBeenCalled();
  });

  it('should display all logs in the list', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const logItems = compiled.querySelectorAll('.log-item');
    expect(logItems.length).toBe(3);
  });

  it('should display log names', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Field Day 2025');
    expect(compiled.textContent).toContain('Winter Field Day');
    expect(compiled.textContent).toContain('POTA Activation');
  });

  it('should display QSO counts', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('150');
    expect(compiled.textContent).toContain('75');
    expect(compiled.textContent).toContain('25');
  });

  it('should handle empty log list', () => {
    apiService.getLogs.and.returnValue(of([]));
    component.ngOnInit();

    expect(component.logs.length).toBe(0);
    expect(component.hasLogs()).toBeFalsy();
  });

  it('should handle API error gracefully', () => {
    apiService.getLogs.and.returnValue(throwError(() => new Error('Network error')));

    component.loadLogs();

    expect(component.errorMessage).toBeTruthy();
  });

  // ==================== LOG SELECTION TESTS ====================

  it('should select log and navigate', () => {
    const log = mockLogs[0];

    component.selectLog(log);

    expect(logService.setCurrentLog).toHaveBeenCalledWith(log);
    expect(router.navigate).toHaveBeenCalledWith(['/log', 1]);
  });

  it('should highlight selected log', () => {
    component.selectLog(mockLogs[0]);
    fixture.detectChanges();

    expect(component.selectedLog).toEqual(mockLogs[0]);
  });

  it('should emit log selection event', () => {
    spyOn(component.logSelected, 'emit');

    component.selectLog(mockLogs[0]);

    expect(component.logSelected.emit).toHaveBeenCalledWith(mockLogs[0]);
  });

  // ==================== LOG CREATION TESTS ====================

  it('should show create log modal', () => {
    component.openCreateModal();

    expect(component.showCreateModal).toBeTruthy();
  });

  it('should create new log', () => {
    const newLog = { name: 'New Log', contestCode: 'ARRL-FD' };
    apiService.createLog.and.returnValue(of({ id: 4, ...newLog }));

    component.newLogName = 'New Log';
    component.newLogContest = 'ARRL-FD';
    component.createLog();

    expect(apiService.createLog).toHaveBeenCalledWith(jasmine.objectContaining(newLog));
  });

  it('should close modal after successful creation', () => {
    apiService.createLog.and.returnValue(of({ id: 4, name: 'New Log' }));

    component.newLogName = 'New Log';
    component.createLog();

    expect(component.showCreateModal).toBeFalsy();
  });

  it('should refresh log list after creation', () => {
    spyOn(component, 'loadLogs');
    apiService.createLog.and.returnValue(of({ id: 4, name: 'New Log' }));

    component.newLogName = 'New Log';
    component.createLog();

    expect(component.loadLogs).toHaveBeenCalled();
  });

  it('should validate log name before creation', () => {
    component.newLogName = '';
    component.createLog();

    expect(apiService.createLog).not.toHaveBeenCalled();
    expect(component.validationError).toBeTruthy();
  });

  it('should handle creation error', () => {
    apiService.createLog.and.returnValue(throwError(() => new Error('Creation failed')));

    component.newLogName = 'New Log';
    component.createLog();

    expect(component.errorMessage).toContain('Creation failed');
  });

  // ==================== LOG DELETION TESTS ====================

  it('should delete log after confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.deleteLog.and.returnValue(of({}));

    component.deleteLog(1);

    expect(apiService.deleteLog).toHaveBeenCalledWith(1);
  });

  it('should not delete log if cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.deleteLog(1);

    expect(apiService.deleteLog).not.toHaveBeenCalled();
  });

  it('should refresh list after deletion', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(component, 'loadLogs');
    apiService.deleteLog.and.returnValue(of({}));

    component.deleteLog(1);

    expect(component.loadLogs).toHaveBeenCalled();
  });

  it('should handle deletion error', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.deleteLog.and.returnValue(throwError(() => new Error('Delete failed')));

    component.deleteLog(1);

    expect(component.errorMessage).toBeTruthy();
  });

  // ==================== LOG UPDATE TESTS ====================

  it('should show edit modal', () => {
    component.editLog(mockLogs[0]);

    expect(component.showEditModal).toBeTruthy();
    expect(component.editingLog).toEqual(mockLogs[0]);
  });

  it('should update log', () => {
    const updatedLog = { ...mockLogs[0], name: 'Updated Name' };
    apiService.updateLog.and.returnValue(of(updatedLog));

    component.editingLog = mockLogs[0];
    component.editingLog.name = 'Updated Name';
    component.updateLog();

    expect(apiService.updateLog).toHaveBeenCalledWith(1, jasmine.objectContaining({ name: 'Updated Name' }));
  });

  it('should close edit modal after update', () => {
    apiService.updateLog.and.returnValue(of(mockLogs[0]));

    component.editingLog = mockLogs[0];
    component.updateLog();

    expect(component.showEditModal).toBeFalsy();
  });

  it('should validate log name before update', () => {
    component.editingLog = { ...mockLogs[0], name: '' };
    component.updateLog();

    expect(apiService.updateLog).not.toHaveBeenCalled();
  });

  // ==================== SEARCH AND FILTER TESTS ====================

  it('should filter logs by name', () => {
    component.searchTerm = 'Field';
    const filtered = component.getFilteredLogs();

    expect(filtered.length).toBe(2);
    expect(filtered.every(log => log.name.includes('Field'))).toBeTruthy();
  });

  it('should filter logs by contest code', () => {
    component.filterContest = 'ARRL-FD';
    const filtered = component.getFilteredLogs();

    expect(filtered.length).toBe(1);
    expect(filtered[0].contestCode).toBe('ARRL-FD');
  });

  it('should return all logs when no filter', () => {
    component.searchTerm = '';
    component.filterContest = '';
    const filtered = component.getFilteredLogs();

    expect(filtered.length).toBe(3);
  });

  it('should handle case-insensitive search', () => {
    component.searchTerm = 'field';
    const filtered = component.getFilteredLogs();

    expect(filtered.length).toBe(2);
  });

  // ==================== SORTING TESTS ====================

  it('should sort logs by name ascending', () => {
    component.sortBy = 'name';
    component.sortDirection = 'asc';
    const sorted = component.getSortedLogs();

    expect(sorted[0].name).toBe('Field Day 2025');
  });

  it('should sort logs by date descending', () => {
    component.sortBy = 'date';
    component.sortDirection = 'desc';
    const sorted = component.getSortedLogs();

    expect(sorted[0].createdAt).toBe('2025-01-15');
  });

  it('should sort logs by QSO count', () => {
    component.sortBy = 'qsoCount';
    component.sortDirection = 'desc';
    const sorted = component.getSortedLogs();

    expect(sorted[0].qsoCount).toBe(150);
  });

  it('should toggle sort direction', () => {
    component.sortDirection = 'asc';
    component.toggleSortDirection();

    expect(component.sortDirection).toBe('desc');
  });

  // ==================== SHARED LOGS TESTS ====================

  it('should load shared logs separately', () => {
    const sharedLogs = [{ id: 10, name: 'Shared Log', isShared: true }];
    apiService.getSharedLogs.and.returnValue(of(sharedLogs));

    component.loadSharedLogs();

    expect(component.sharedLogs).toEqual(sharedLogs);
  });

  it('should display shared logs indicator', () => {
    component.sharedLogs = [{ id: 10, name: 'Shared Log', isShared: true }];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.shared-indicator')).toBeTruthy();
  });

  it('should differentiate shared logs from owned logs', () => {
    const owned = component.getOwnedLogs();
    const shared = component.getSharedLogs();

    expect(owned.length).toBe(3);
    expect(shared.length).toBe(0);
  });

  // ==================== UI STATE TESTS ====================

  it('should show loading indicator while fetching', () => {
    component.loading = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.loading-spinner')).toBeTruthy();
  });

  it('should show empty state when no logs', () => {
    component.logs = [];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.empty-state')).toBeTruthy();
  });

  it('should close modal on cancel', () => {
    component.showCreateModal = true;
    component.closeModal();

    expect(component.showCreateModal).toBeFalsy();
  });

  it('should reset form on modal close', () => {
    component.newLogName = 'Test';
    component.newLogContest = 'ARRL-FD';
    component.closeModal();

    expect(component.newLogName).toBe('');
    expect(component.newLogContest).toBe('');
  });

  // ==================== CONTEST CODE TESTS ====================

  it('should provide list of valid contest codes', () => {
    const contests = component.getContestCodes();

    expect(contests).toContain('ARRL-FD');
    expect(contests).toContain('WFD');
    expect(contests).toContain('POTA');
    expect(contests).toContain('SOTA');
  });

  it('should display contest badge', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.contest-badge')).toBeTruthy();
  });

  // ==================== ACCESSIBILITY TESTS ====================

  it('should have accessible labels', () => {
    const compiled = fixture.nativeElement;
    const searchInput = compiled.querySelector('input[type="search"]');
    expect(searchInput?.getAttribute('aria-label')).toBeTruthy();
  });

  it('should be keyboard navigable', async () => {
    const compiled = fixture.nativeElement;
    const firstLog = compiled.querySelector('.log-item');

    firstLog?.focus();
    expect(document.activeElement).toBe(firstLog);
  });

  // ==================== REFRESH TESTS ====================

  it('should refresh logs on manual refresh', () => {
    spyOn(component, 'loadLogs');
    spyOn(component, 'loadSharedLogs');

    component.refresh();

    expect(component.loadLogs).toHaveBeenCalled();
    expect(component.loadSharedLogs).toHaveBeenCalled();
  });

  it('should clear errors on refresh', () => {
    component.errorMessage = 'Error occurred';
    component.refresh();

    expect(component.errorMessage).toBe('');
  });
});
