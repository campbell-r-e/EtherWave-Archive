import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { OperatorManagementComponent } from './operator-management.component';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

describe('OperatorManagementComponent', () => {
  let component: OperatorManagementComponent;
  let fixture: ComponentFixture<OperatorManagementComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let authService: jasmine.SpyObj<AuthService>;

  const mockOperators = [
    {
      id: 1,
      username: 'w1aw',
      callsign: 'W1AW',
      role: 'ADMIN',
      email: 'w1aw@example.com',
      isActive: true,
      qsoCount: 150,
      lastActive: '2025-01-15T10:00:00'
    },
    {
      id: 2,
      username: 'k2abc',
      callsign: 'K2ABC',
      role: 'STATION',
      email: 'k2abc@example.com',
      isActive: true,
      qsoCount: 75,
      lastActive: '2025-01-14T15:30:00'
    },
    {
      id: 3,
      username: 'n3xyz',
      callsign: 'N3XYZ',
      role: 'LOGGER',
      email: 'n3xyz@example.com',
      isActive: false,
      qsoCount: 25,
      lastActive: '2025-01-10T08:00:00'
    }
  ];

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'getOperators',
      'addOperator',
      'updateOperator',
      'deleteOperator',
      'changeOperatorRole',
      'getOperatorStats',
      'deactivateOperator',
      'reactivateOperator'
    ]);
    const authServiceSpy = jasmine.createSpyObj('AuthService', [
      'getCurrentUser',
      'hasRole'
    ]);

    await TestBed.configureTestingModule({
      imports: [OperatorManagementComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;

    authService.getCurrentUser.and.returnValue({ id: 1, username: 'w1aw', role: 'ADMIN' });
    authService.hasRole.and.returnValue(true);
    apiService.getOperators.and.returnValue(of(mockOperators));

    fixture = TestBed.createComponent(OperatorManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== OPERATOR LOADING TESTS ====================

  it('should load operators on init', () => {
    expect(apiService.getOperators).toHaveBeenCalled();
    expect(component.operators.length).toBe(3);
  });

  it('should display all operators', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const operatorRows = compiled.querySelectorAll('.operator-row');
    expect(operatorRows.length).toBe(3);
  });

  it('should display operator callsigns', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('W1AW');
    expect(compiled.textContent).toContain('K2ABC');
    expect(compiled.textContent).toContain('N3XYZ');
  });

  it('should handle empty operator list', () => {
    apiService.getOperators.and.returnValue(of([]));
    component.ngOnInit();

    expect(component.operators.length).toBe(0);
    expect(component.hasOperators()).toBeFalsy();
  });

  it('should handle API error gracefully', () => {
    apiService.getOperators.and.returnValue(throwError(() => new Error('API error')));

    component.loadOperators();

    expect(component.errorMessage).toBeTruthy();
  });

  // ==================== OPERATOR ADDITION TESTS ====================

  it('should open add operator modal', () => {
    component.openAddModal();

    expect(component.showAddModal).toBeTruthy();
  });

  it('should add new operator', () => {
    const newOperator = {
      username: 've3abc',
      callsign: 'VE3ABC',
      email: 've3abc@example.com',
      role: 'STATION'
    };
    apiService.addOperator.and.returnValue(of({ id: 4, ...newOperator }));

    component.newOperator = newOperator;
    component.addOperator();

    expect(apiService.addOperator).toHaveBeenCalledWith(jasmine.objectContaining(newOperator));
  });

  it('should validate username before adding', () => {
    component.newOperator.username = '';
    component.addOperator();

    expect(apiService.addOperator).not.toHaveBeenCalled();
    expect(component.validationError).toBeTruthy();
  });

  it('should validate callsign format', () => {
    component.newOperator.username = 'testuser';
    component.newOperator.callsign = 'INVALID';

    component.addOperator();

    expect(component.validationError).toContain('callsign');
  });

  it('should validate email format', () => {
    component.newOperator.username = 'testuser';
    component.newOperator.callsign = 'W1AW';
    component.newOperator.email = 'invalid-email';

    component.addOperator();

    expect(component.validationError).toContain('email');
  });

  it('should close modal after adding', () => {
    apiService.addOperator.and.returnValue(of({ id: 4, username: 've3abc' }));

    component.newOperator = { username: 've3abc', callsign: 'VE3ABC', email: 'test@example.com', role: 'STATION' };
    component.addOperator();

    expect(component.showAddModal).toBeFalsy();
  });

  it('should refresh list after adding', () => {
    spyOn(component, 'loadOperators');
    apiService.addOperator.and.returnValue(of({ id: 4, username: 've3abc' }));

    component.newOperator = { username: 've3abc', callsign: 'VE3ABC', email: 'test@example.com', role: 'STATION' };
    component.addOperator();

    expect(component.loadOperators).toHaveBeenCalled();
  });

  // ==================== OPERATOR UPDATE TESTS ====================

  it('should open edit modal', () => {
    component.editOperator(mockOperators[0]);

    expect(component.showEditModal).toBeTruthy();
    expect(component.editingOperator).toEqual(mockOperators[0]);
  });

  it('should update operator', () => {
    const updated = { ...mockOperators[0], email: 'newemail@example.com' };
    apiService.updateOperator.and.returnValue(of(updated));

    component.editingOperator = updated;
    component.updateOperator();

    expect(apiService.updateOperator).toHaveBeenCalledWith(1, jasmine.objectContaining({ email: 'newemail@example.com' }));
  });

  it('should validate updates', () => {
    component.editingOperator = { ...mockOperators[0], callsign: '' };

    component.updateOperator();

    expect(apiService.updateOperator).not.toHaveBeenCalled();
    expect(component.validationError).toBeTruthy();
  });

  it('should close edit modal after update', () => {
    apiService.updateOperator.and.returnValue(of(mockOperators[0]));

    component.editingOperator = mockOperators[0];
    component.updateOperator();

    expect(component.showEditModal).toBeFalsy();
  });

  // ==================== OPERATOR DELETION TESTS ====================

  it('should delete operator after confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.deleteOperator.and.returnValue(of({}));

    component.deleteOperator(2);

    expect(apiService.deleteOperator).toHaveBeenCalledWith(2);
  });

  it('should not delete operator if cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.deleteOperator(2);

    expect(apiService.deleteOperator).not.toHaveBeenCalled();
  });

  it('should not allow deleting self', () => {
    component.deleteOperator(1); // Current user ID

    expect(component.errorMessage).toContain('yourself');
  });

  it('should refresh list after deletion', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(component, 'loadOperators');
    apiService.deleteOperator.and.returnValue(of({}));

    component.deleteOperator(2);

    expect(component.loadOperators).toHaveBeenCalled();
  });

  // ==================== ROLE MANAGEMENT TESTS ====================

  it('should change operator role', () => {
    apiService.changeOperatorRole.and.returnValue(of({ success: true }));

    component.changeRole(2, 'ADMIN');

    expect(apiService.changeOperatorRole).toHaveBeenCalledWith(2, 'ADMIN');
  });

  it('should display role badge', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.role-badge')).toBeTruthy();
  });

  it('should show role permissions', () => {
    const permissions = component.getRolePermissions('ADMIN');

    expect(permissions).toContain('Manage operators');
    expect(permissions).toContain('Delete logs');
  });

  it('should provide available roles', () => {
    const roles = component.getAvailableRoles();

    expect(roles).toContain('ADMIN');
    expect(roles).toContain('STATION');
    expect(roles).toContain('LOGGER');
    expect(roles).toContain('VIEWER');
  });

  it('should not allow changing own role', () => {
    component.changeRole(1, 'VIEWER');

    expect(component.errorMessage).toContain('own role');
  });

  it('should require admin permission to change roles', () => {
    authService.hasRole.and.returnValue(false);

    component.changeRole(2, 'ADMIN');

    expect(component.errorMessage).toContain('permission');
  });

  // ==================== OPERATOR STATISTICS TESTS ====================

  it('should load operator statistics', () => {
    const stats = {
      totalQSOs: 250,
      uniqueCallsigns: 150,
      activeOperators: 2,
      inactiveOperators: 1
    };
    apiService.getOperatorStats.and.returnValue(of(stats));

    component.loadStats();

    expect(component.stats).toEqual(stats);
  });

  it('should display QSO count per operator', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('150');
    expect(compiled.textContent).toContain('75');
  });

  it('should display last active time', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.last-active')).toBeTruthy();
  });

  it('should format last active time', () => {
    const formatted = component.formatLastActive('2025-01-15T10:00:00');

    expect(formatted).toBeTruthy();
  });

  // ==================== ACTIVE STATUS TESTS ====================

  it('should deactivate operator', () => {
    apiService.deactivateOperator.and.returnValue(of({ success: true }));

    component.deactivateOperator(2);

    expect(apiService.deactivateOperator).toHaveBeenCalledWith(2);
  });

  it('should reactivate operator', () => {
    apiService.reactivateOperator.and.returnValue(of({ success: true }));

    component.reactivateOperator(3);

    expect(apiService.reactivateOperator).toHaveBeenCalledWith(3);
  });

  it('should show inactive badge', () => {
    component.operators = [mockOperators[2]]; // Inactive operator
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.inactive-badge')).toBeTruthy();
  });

  it('should filter active operators', () => {
    component.showOnlyActive = true;
    const filtered = component.getFilteredOperators();

    expect(filtered.every(op => op.isActive)).toBeTruthy();
  });

  it('should filter inactive operators', () => {
    component.showOnlyInactive = true;
    const filtered = component.getFilteredOperators();

    expect(filtered.every(op => !op.isActive)).toBeTruthy();
  });

  // ==================== SEARCH AND FILTER TESTS ====================

  it('should filter operators by callsign', () => {
    component.searchTerm = 'W1AW';
    const filtered = component.getFilteredOperators();

    expect(filtered.length).toBe(1);
    expect(filtered[0].callsign).toBe('W1AW');
  });

  it('should filter by role', () => {
    component.filterRole = 'STATION';
    const filtered = component.getFilteredOperators();

    expect(filtered.every(op => op.role === 'STATION')).toBeTruthy();
  });

  it('should handle case-insensitive search', () => {
    component.searchTerm = 'w1aw';
    const filtered = component.getFilteredOperators();

    expect(filtered.length).toBe(1);
  });

  it('should search by email', () => {
    component.searchTerm = 'k2abc@example.com';
    const filtered = component.getFilteredOperators();

    expect(filtered.length).toBe(1);
    expect(filtered[0].email).toBe('k2abc@example.com');
  });

  // ==================== SORTING TESTS ====================

  it('should sort by callsign', () => {
    component.sortBy = 'callsign';
    component.sortDirection = 'asc';
    const sorted = component.getSortedOperators();

    expect(sorted[0].callsign).toBe('K2ABC');
  });

  it('should sort by QSO count', () => {
    component.sortBy = 'qsoCount';
    component.sortDirection = 'desc';
    const sorted = component.getSortedOperators();

    expect(sorted[0].qsoCount).toBe(150);
  });

  it('should sort by last active', () => {
    component.sortBy = 'lastActive';
    component.sortDirection = 'desc';
    const sorted = component.getSortedOperators();

    expect(sorted[0].lastActive).toBe('2025-01-15T10:00:00');
  });

  it('should toggle sort direction', () => {
    component.sortDirection = 'asc';
    component.toggleSortDirection();

    expect(component.sortDirection).toBe('desc');
  });

  // ==================== PERMISSION TESTS ====================

  it('should check if user can edit operator', () => {
    authService.hasRole.and.returnValue(true);

    expect(component.canEdit(mockOperators[1])).toBeTruthy();
  });

  it('should check if user can delete operator', () => {
    authService.hasRole.and.returnValue(true);

    expect(component.canDelete(mockOperators[1])).toBeTruthy();
  });

  it('should prevent non-admins from editing', () => {
    authService.hasRole.and.returnValue(false);

    expect(component.canEdit(mockOperators[1])).toBeFalsy();
  });

  it('should prevent editing own account by non-admins', () => {
    authService.hasRole.and.returnValue(false);
    authService.getCurrentUser.and.returnValue({ id: 2, username: 'k2abc' });

    expect(component.canEdit(mockOperators[1])).toBeTruthy();
  });

  // ==================== EXPORT TESTS ====================

  it('should export operator list as CSV', () => {
    spyOn(component, 'downloadFile');

    component.exportAsCSV();

    expect(component.downloadFile).toHaveBeenCalled();
  });

  it('should include all operator data in export', () => {
    const csv = component.generateCSV();

    expect(csv).toContain('W1AW');
    expect(csv).toContain('ADMIN');
    expect(csv).toContain('150');
  });

  // ==================== UI STATE TESTS ====================

  it('should show loading indicator', () => {
    component.loading = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.loading-spinner')).toBeTruthy();
  });

  it('should show empty state', () => {
    component.operators = [];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.empty-state')).toBeTruthy();
  });

  it('should close modal on cancel', () => {
    component.showAddModal = true;
    component.closeModal();

    expect(component.showAddModal).toBeFalsy();
  });

  it('should reset form on close', () => {
    component.newOperator = { username: 'test', callsign: 'W1AW', email: 'test@example.com', role: 'STATION' };
    component.closeModal();

    expect(component.newOperator.username).toBe('');
  });

  // ==================== VALIDATION TESTS ====================

  it('should validate callsign format', () => {
    expect(component.isValidCallsign('W1AW')).toBeTruthy();
    expect(component.isValidCallsign('K2ABC')).toBeTruthy();
    expect(component.isValidCallsign('INVALID')).toBeFalsy();
  });

  it('should validate email format', () => {
    expect(component.isValidEmail('test@example.com')).toBeTruthy();
    expect(component.isValidEmail('invalid')).toBeFalsy();
  });

  it('should validate username uniqueness', () => {
    component.newOperator.username = 'w1aw'; // Already exists

    component.addOperator();

    expect(component.validationError).toContain('username');
  });

  it('should validate callsign uniqueness', () => {
    component.newOperator.username = 'newuser';
    component.newOperator.callsign = 'W1AW'; // Already exists

    component.addOperator();

    expect(component.validationError).toContain('callsign');
  });

  // ==================== ACCESSIBILITY TESTS ====================

  it('should have accessible form labels', () => {
    component.showAddModal = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const usernameInput = compiled.querySelector('input[name="username"]');
    expect(usernameInput?.getAttribute('aria-label')).toBeTruthy();
  });

  it('should announce operator changes', () => {
    spyOn(component, 'announceToScreenReader');
    apiService.addOperator.and.returnValue(of({ id: 4, username: 'test' }));

    component.newOperator = { username: 'test', callsign: 'W1AW', email: 'test@example.com', role: 'STATION' };
    component.addOperator();

    expect(component.announceToScreenReader).toHaveBeenCalled();
  });

  it('should be keyboard navigable', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const firstRow = compiled.querySelector('.operator-row');

    firstRow?.focus();
    expect(document.activeElement).toBe(firstRow);
  });

  // ==================== REFRESH TESTS ====================

  it('should refresh operator list', () => {
    spyOn(component, 'loadOperators');
    spyOn(component, 'loadStats');

    component.refresh();

    expect(component.loadOperators).toHaveBeenCalled();
    expect(component.loadStats).toHaveBeenCalled();
  });

  it('should clear messages on refresh', () => {
    component.errorMessage = 'Error';
    component.successMessage = 'Success';

    component.refresh();

    expect(component.errorMessage).toBe('');
    expect(component.successMessage).toBe('');
  });

  // ==================== PASSWORD RESET TESTS ====================

  it('should send password reset email', () => {
    apiService.sendPasswordReset = jasmine.createSpy().and.returnValue(of({ success: true }));

    component.sendPasswordReset(mockOperators[1]);

    expect(apiService.sendPasswordReset).toHaveBeenCalledWith(mockOperators[1].email);
  });

  it('should require confirmation for password reset', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    apiService.sendPasswordReset = jasmine.createSpy();

    component.sendPasswordReset(mockOperators[1]);

    expect(apiService.sendPasswordReset).not.toHaveBeenCalled();
  });
});
