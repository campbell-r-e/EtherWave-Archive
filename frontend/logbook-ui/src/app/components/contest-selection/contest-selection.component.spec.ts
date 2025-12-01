import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ContestSelectionComponent } from './contest-selection.component';
import { ApiService } from '../../services/api.service';
import { Contest } from '../../models/station.model';

describe('ContestSelectionComponent', () => {
  let component: ContestSelectionComponent;
  let fixture: ComponentFixture<ContestSelectionComponent>;
  let apiService: jasmine.SpyObj<ApiService>;

  const mockContests: Contest[] = [
    {
      contestCode: 'ARRL-FD',
      contestName: 'ARRL Field Day',
      description: 'Annual emergency preparedness exercise',
      startDate: '2025-06-28',
      endDate: '2025-06-29',
      isActive: false
    },
    {
      contestCode: 'WFD',
      contestName: 'Winter Field Day',
      description: 'Winter emergency preparedness contest',
      startDate: '2025-01-25',
      endDate: '2025-01-26',
      isActive: false
    },
    {
      contestCode: 'POTA',
      contestName: 'Parks on the Air',
      description: 'Amateur radio in parks',
      isActive: true
    }
  ];

  const mockActiveContests: Contest[] = [mockContests[2]];

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'getContests',
      'getActiveContests'
    ]);

    await TestBed.configureTestingModule({
      imports: [ContestSelectionComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;

    apiService.getContests.and.returnValue(of(mockContests));
    apiService.getActiveContests.and.returnValue(of(mockActiveContests));

    fixture = TestBed.createComponent(ContestSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load contests on init', () => {
    expect(apiService.getContests).toHaveBeenCalled();
    expect(component.contests.length).toBe(3);
  });

  it('should load active contests', () => {
    expect(apiService.getActiveContests).toHaveBeenCalled();
    expect(component.activeContests.length).toBe(1);
  });

  it('should handle API error gracefully', () => {
    apiService.getContests.and.returnValue(throwError(() => new Error('Network error')));

    component.loadContests();

    expect(component.loading).toBe(false);
  });

  it('should select contest', () => {
    spyOn(component.contestSelected, 'emit');

    component.selectContest(mockContests[0]);

    expect(component.selectedContest).toEqual(mockContests[0]);
    expect(component.contestSelected.emit).toHaveBeenCalledWith(mockContests[0]);
  });

  it('should detect if contest is active', () => {
    const activeContest: Contest = {
      contestCode: 'TEST',
      contestName: 'Test Contest',
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 86400000).toISOString().split('T')[0]
    };

    expect(component.isContestActive(activeContest)).toBeTruthy();
  });

  it('should detect if contest is upcoming', () => {
    const futureContest: Contest = {
      contestCode: 'FUTURE',
      contestName: 'Future Contest',
      startDate: new Date(Date.now() + 86400000 * 30).toISOString().split('T')[0],
      endDate: new Date(Date.now() + 86400000 * 31).toISOString().split('T')[0]
    };

    expect(component.isContestUpcoming(futureContest)).toBeTruthy();
  });

  it('should get contest date range', () => {
    const dateRange = component.getContestDateRange(mockContests[0]);
    expect(dateRange).toContain('Jun');
  });

  it('should return "Dates TBD" for contests without dates', () => {
    const noDateContest: Contest = {
      contestCode: 'TEST',
      contestName: 'Test'
    };

    expect(component.getContestDateRange(noDateContest)).toBe('Dates TBD');
  });

  it('should toggle show all contests', () => {
    expect(component.showAllContests).toBe(false);
    component.toggleShowAll();
    expect(component.showAllContests).toBe(true);
  });

  it('should get displayed contests based on showAllContests', () => {
    component.showAllContests = false;
    expect(component.getDisplayedContests()).toEqual(component.activeContests);

    component.showAllContests = true;
    expect(component.getDisplayedContests()).toEqual(component.contests);
  });

  it('should clear selection', () => {
    component.selectedContest = mockContests[0];
    component.clearSelection();
    expect(component.selectedContest).toBeNull();
  });

  it('should get correct badge class for active contest', () => {
    const activeContest: Contest = {
      contestCode: 'TEST',
      contestName: 'Test',
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 86400000).toISOString().split('T')[0]
    };

    expect(component.getContestBadgeClass(activeContest)).toBe('badge bg-success');
  });

  it('should get correct status text for active contest', () => {
    const activeContest: Contest = {
      contestCode: 'TEST',
      contestName: 'Test',
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 86400000).toISOString().split('T')[0]
    };

    expect(component.getContestStatusText(activeContest)).toBe('Active Now');
  });
});
