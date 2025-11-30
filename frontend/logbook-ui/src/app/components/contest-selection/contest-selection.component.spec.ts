import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ContestSelectionComponent } from './contest-selection.component';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log.service';

describe('ContestSelectionComponent', () => {
  let component: ContestSelectionComponent;
  let fixture: ComponentFixture<ContestSelectionComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let logService: jasmine.SpyObj<LogService>;

  const mockContests = [
    {
      code: 'ARRL-FD',
      name: 'ARRL Field Day',
      description: 'Annual emergency preparedness exercise',
      validClasses: ['1A', '2A', '3A', '1B', '2B'],
      requiresSection: true,
      startDate: '2025-06-28',
      endDate: '2025-06-29'
    },
    {
      code: 'WFD',
      name: 'Winter Field Day',
      description: 'Winter emergency preparedness contest',
      validClasses: ['1O', '2O', '3O', '1H', '2H'],
      requiresSection: true,
      startDate: '2025-01-25',
      endDate: '2025-01-26'
    },
    {
      code: 'POTA',
      name: 'Parks on the Air',
      description: 'Amateur radio in parks',
      requiresParkNumber: true,
      ongoing: true
    }
  ];

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'getAvailableContests',
      'getContestRules',
      'validateContestEntry',
      'getARRLSections'
    ]);
    const logServiceSpy = jasmine.createSpyObj('LogService', [
      'setContestMode',
      'getContestMode',
      'setContestParameters'
    ]);

    await TestBed.configureTestingModule({
      imports: [ContestSelectionComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: LogService, useValue: logServiceSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    logService = TestBed.inject(LogService) as jasmine.SpyObj<LogService>;

    apiService.getAvailableContests.and.returnValue(of(mockContests));
    apiService.getARRLSections.and.returnValue(of(['ORG', 'SCV', 'LAX', 'NFL']));

    fixture = TestBed.createComponent(ContestSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== CONTEST LOADING TESTS ====================

  it('should load available contests on init', () => {
    expect(apiService.getAvailableContests).toHaveBeenCalled();
    expect(component.contests.length).toBe(3);
  });

  it('should display all contests', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const contestCards = compiled.querySelectorAll('.contest-card');
    expect(contestCards.length).toBe(3);
  });

  it('should display contest names', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('ARRL Field Day');
    expect(compiled.textContent).toContain('Winter Field Day');
    expect(compiled.textContent).toContain('Parks on the Air');
  });

  it('should display contest descriptions', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('emergency preparedness');
  });

  it('should handle API error gracefully', () => {
    apiService.getAvailableContests.and.returnValue(throwError(() => new Error('Network error')));

    component.loadContests();

    expect(component.errorMessage).toBeTruthy();
  });

  // ==================== CONTEST SELECTION TESTS ====================

  it('should select contest', () => {
    component.selectContest(mockContests[0]);

    expect(component.selectedContest).toEqual(mockContests[0]);
    expect(logService.setContestMode).toHaveBeenCalledWith(mockContests[0].code);
  });

  it('should highlight selected contest', () => {
    component.selectContest(mockContests[0]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.contest-card.selected')).toBeTruthy();
  });

  it('should emit contest selection event', () => {
    spyOn(component.contestSelected, 'emit');

    component.selectContest(mockContests[0]);

    expect(component.contestSelected.emit).toHaveBeenCalledWith(mockContests[0]);
  });

  it('should load contest rules on selection', () => {
    const mockRules = { scoring: 'Points per contact', multipliers: ['States', 'Sections'] };
    apiService.getContestRules.and.returnValue(of(mockRules));

    component.selectContest(mockContests[0]);

    expect(apiService.getContestRules).toHaveBeenCalledWith('ARRL-FD');
  });

  // ==================== FIELD DAY CLASS TESTS ====================

  it('should display Field Day classes', () => {
    component.selectContest(mockContests[0]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('1A');
    expect(compiled.textContent).toContain('2A');
  });

  it('should validate Field Day class format', () => {
    expect(component.isValidFieldDayClass('1A')).toBeTruthy();
    expect(component.isValidFieldDayClass('15F')).toBeTruthy();
    expect(component.isValidFieldDayClass('INVALID')).toBeFalsy();
  });

  it('should parse transmitter count from class', () => {
    expect(component.getTransmitterCount('3A')).toBe(3);
    expect(component.getTransmitterCount('10B')).toBe(10);
  });

  it('should parse class category', () => {
    expect(component.getClassCategory('3A')).toBe('A');
    expect(component.getClassCategory('5B')).toBe('B');
  });

  it('should provide class descriptions', () => {
    const desc = component.getClassDescription('1A');
    expect(desc).toContain('1 transmitter');
  });

  // ==================== ARRL SECTION TESTS ====================

  it('should load ARRL sections', () => {
    component.selectContest(mockContests[0]);

    expect(apiService.getARRLSections).toHaveBeenCalled();
    expect(component.arrlSections.length).toBeGreaterThan(0);
  });

  it('should display section dropdown for Field Day', () => {
    component.selectContest(mockContests[0]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('select[name="section"]')).toBeTruthy();
  });

  it('should validate section selection', () => {
    component.selectedContest = mockContests[0];
    component.selectedSection = '';

    const isValid = component.validateSelection();

    expect(isValid).toBeFalsy();
    expect(component.validationError).toContain('section');
  });

  it('should accept valid ARRL section', () => {
    component.selectedSection = 'ORG';

    expect(component.isValidSection('ORG')).toBeTruthy();
  });

  // ==================== WINTER FIELD DAY TESTS ====================

  it('should display Winter Field Day classes', () => {
    component.selectContest(mockContests[1]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('1O');
    expect(compiled.textContent).toContain('2H');
  });

  it('should validate Winter Field Day class', () => {
    expect(component.isValidWinterFieldDayClass('1O')).toBeTruthy();
    expect(component.isValidWinterFieldDayClass('3H')).toBeTruthy();
    expect(component.isValidWinterFieldDayClass('1A')).toBeFalsy();
  });

  it('should distinguish outdoor vs home categories', () => {
    expect(component.isOutdoorCategory('2O')).toBeTruthy();
    expect(component.isOutdoorCategory('2H')).toBeFalsy();
  });

  // ==================== POTA TESTS ====================

  it('should require park number for POTA', () => {
    component.selectContest(mockContests[2]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('input[name="parkNumber"]')).toBeTruthy();
  });

  it('should validate POTA park number format', () => {
    expect(component.isValidPOTAPark('K-0817')).toBeTruthy();
    expect(component.isValidPOTAPark('US-1234')).toBeTruthy();
    expect(component.isValidPOTAPark('INVALID')).toBeFalsy();
  });

  it('should not show date restrictions for POTA', () => {
    component.selectContest(mockContests[2]);

    expect(component.hasDateRestrictions()).toBeFalsy();
  });

  // ==================== CONTEST DATES TESTS ====================

  it('should display contest dates', () => {
    component.selectContest(mockContests[0]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('2025-06-28');
    expect(compiled.textContent).toContain('2025-06-29');
  });

  it('should detect if contest is active', () => {
    const contest = {
      ...mockContests[0],
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 86400000).toISOString().split('T')[0]
    };

    expect(component.isContestActive(contest)).toBeTruthy();
  });

  it('should detect upcoming contests', () => {
    const futureContest = {
      ...mockContests[0],
      startDate: new Date(Date.now() + 86400000 * 30).toISOString().split('T')[0]
    };

    expect(component.isUpcoming(futureContest)).toBeTruthy();
  });

  it('should detect past contests', () => {
    const pastContest = {
      ...mockContests[0],
      endDate: '2024-01-01'
    };

    expect(component.isPast(pastContest)).toBeTruthy();
  });

  // ==================== EXCHANGE VALIDATION TESTS ====================

  it('should validate Field Day exchange', () => {
    const exchange = {
      class: '2A',
      section: 'ORG'
    };

    apiService.validateContestEntry.and.returnValue(of({ valid: true }));

    component.validateExchange(exchange);

    expect(apiService.validateContestEntry).toHaveBeenCalled();
  });

  it('should show validation error for invalid exchange', () => {
    const exchange = {
      class: 'INVALID',
      section: 'XXX'
    };

    apiService.validateContestEntry.and.returnValue(of({ valid: false, errors: ['Invalid class'] }));

    component.validateExchange(exchange);

    expect(component.validationError).toBeTruthy();
  });

  // ==================== CONTEST PARAMETERS TESTS ====================

  it('should set contest parameters', () => {
    component.selectedContest = mockContests[0];
    component.selectedClass = '2A';
    component.selectedSection = 'ORG';

    component.applyContestSettings();

    expect(logService.setContestParameters).toHaveBeenCalledWith(
      jasmine.objectContaining({
        contestCode: 'ARRL-FD',
        class: '2A',
        section: 'ORG'
      })
    );
  });

  it('should validate all required parameters before applying', () => {
    component.selectedContest = mockContests[0];
    component.selectedClass = '';
    component.selectedSection = 'ORG';

    component.applyContestSettings();

    expect(logService.setContestParameters).not.toHaveBeenCalled();
    expect(component.validationError).toBeTruthy();
  });

  // ==================== SEARCH AND FILTER TESTS ====================

  it('should filter contests by name', () => {
    component.searchTerm = 'Field';
    const filtered = component.getFilteredContests();

    expect(filtered.length).toBe(2);
    expect(filtered.every(c => c.name.includes('Field'))).toBeTruthy();
  });

  it('should filter by contest status', () => {
    component.filterStatus = 'active';
    const filtered = component.getFilteredContests();

    expect(filtered.every(c => component.isContestActive(c))).toBeTruthy();
  });

  it('should filter by ongoing contests', () => {
    component.filterOngoing = true;
    const filtered = component.getFilteredContests();

    expect(filtered.some(c => c.ongoing)).toBeTruthy();
  });

  // ==================== CONTEST RULES DISPLAY TESTS ====================

  it('should display contest rules', () => {
    const rules = {
      scoring: 'Points per contact',
      multipliers: ['States', 'Sections'],
      bonusPoints: ['Emergency power', 'Natural power']
    };
    apiService.getContestRules.and.returnValue(of(rules));

    component.selectContest(mockContests[0]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Points per contact');
  });

  it('should show bonus points for Field Day', () => {
    component.selectedContest = mockContests[0];
    const bonuses = component.getAvailableBonuses();

    expect(bonuses).toContain('100% Emergency Power');
    expect(bonuses).toContain('Media Publicity');
  });

  it('should calculate bonus points', () => {
    component.selectedBonuses = ['100% Emergency Power', 'Media Publicity'];
    const total = component.calculateBonusPoints();

    expect(total).toBeGreaterThan(0);
  });

  // ==================== UI STATE TESTS ====================

  it('should show loading indicator', () => {
    component.loading = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.loading-spinner')).toBeTruthy();
  });

  it('should show empty state when no contests', () => {
    component.contests = [];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.empty-state')).toBeTruthy();
  });

  it('should disable selection for past contests', () => {
    const pastContest = {
      ...mockContests[0],
      endDate: '2024-01-01'
    };
    component.contests = [pastContest];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.contest-card.disabled')).toBeTruthy();
  });

  // ==================== CONTEST INFO MODAL TESTS ====================

  it('should open contest info modal', () => {
    component.showContestInfo(mockContests[0]);

    expect(component.showInfoModal).toBeTruthy();
    expect(component.infoContest).toEqual(mockContests[0]);
  });

  it('should display detailed rules in modal', () => {
    component.showInfoModal = true;
    component.infoContest = mockContests[0];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.contest-info-modal')).toBeTruthy();
  });

  it('should close info modal', () => {
    component.showInfoModal = true;
    component.closeInfoModal();

    expect(component.showInfoModal).toBeFalsy();
  });

  // ==================== CONTEST TEMPLATES TESTS ====================

  it('should load contest template settings', () => {
    component.loadTemplate('ARRL-FD');

    expect(component.selectedClass).toBeTruthy();
    expect(component.selectedSection).toBeTruthy();
  });

  it('should save contest template', () => {
    component.selectedContest = mockContests[0];
    component.selectedClass = '2A';
    component.selectedSection = 'ORG';

    component.saveAsTemplate('My Field Day Setup');

    expect(component.templates.length).toBeGreaterThan(0);
  });

  // ==================== ACCESSIBILITY TESTS ====================

  it('should have accessible contest cards', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const card = compiled.querySelector('.contest-card');

    expect(card?.getAttribute('role')).toBe('button');
    expect(card?.getAttribute('tabindex')).toBe('0');
  });

  it('should be keyboard navigable', async () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const firstCard = compiled.querySelector('.contest-card');

    firstCard?.focus();
    expect(document.activeElement).toBe(firstCard);
  });

  // ==================== REFRESH TESTS ====================

  it('should refresh contest list', () => {
    spyOn(component, 'loadContests');

    component.refresh();

    expect(component.loadContests).toHaveBeenCalled();
  });

  it('should clear errors on refresh', () => {
    component.errorMessage = 'Error';
    component.refresh();

    expect(component.errorMessage).toBe('');
  });

  // ==================== RESET TESTS ====================

  it('should reset contest selection', () => {
    component.selectedContest = mockContests[0];
    component.selectedClass = '2A';
    component.selectedSection = 'ORG';

    component.resetSelection();

    expect(component.selectedContest).toBeNull();
    expect(component.selectedClass).toBe('');
    expect(component.selectedSection).toBe('');
  });
});
