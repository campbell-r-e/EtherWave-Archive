import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { QsoListComponent } from './qso-list.component';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log.service';

describe('QsoListComponent', () => {
  let component: QsoListComponent;
  let fixture: ComponentFixture<QsoListComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let logService: jasmine.SpyObj<LogService>;

  const mockQSOs = [
    { id: 1, callsign: 'W1AW', frequencyKhz: 14250, mode: 'SSB', qsoDate: '2025-01-15', timeOn: '14:30' },
    { id: 2, callsign: 'K2ABC', frequencyKhz: 7125, mode: 'CW', qsoDate: '2025-01-15', timeOn: '15:00' },
    { id: 3, callsign: 'N3XYZ', frequencyKhz: 21200, mode: 'FT8', qsoDate: '2025-01-14', timeOn: '16:00' }
  ];

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'getQSOs', 'deleteQSO', 'searchQSOs', 'exportAdif', 'exportCabrillo'
    ]);
    const logServiceSpy = jasmine.createSpyObj('LogService', ['getCurrentLog']);

    await TestBed.configureTestingModule({
      imports: [QsoListComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: LogService, useValue: logServiceSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    logService = TestBed.inject(LogService) as jasmine.SpyObj<LogService>;

    logService.getCurrentLog.and.returnValue({ id: 1, name: 'Test Log' });
    apiService.getQSOs.and.returnValue(of(mockQSOs));

    fixture = TestBed.createComponent(QsoListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load QSOs on init', () => {
    expect(apiService.getQSOs).toHaveBeenCalledWith(1);
    expect(component.qsos.length).toBe(3);
  });

  it('should display all QSOs in table', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const rows = compiled.querySelectorAll('tr.qso-row');
    expect(rows.length).toBe(3);
  });

  it('should display callsign in uppercase', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('W1AW');
    expect(compiled.textContent).toContain('K2ABC');
  });

  it('should filter QSOs by callsign', () => {
    component.filterCallsign = 'W1AW';
    component.applyFilters();

    expect(component.filteredQSOs.length).toBe(1);
    expect(component.filteredQSOs[0].callsign).toBe('W1AW');
  });

  it('should filter QSOs by band', () => {
    component.filterBand = '20m';
    component.applyFilters();

    const result = component.filteredQSOs.filter(q =>
      q.frequencyKhz >= 14000 && q.frequencyKhz <= 14350
    );
    expect(result.length).toBeGreaterThan(0);
  });

  it('should filter QSOs by mode', () => {
    component.filterMode = 'SSB';
    component.applyFilters();

    expect(component.filteredQSOs.every(q => q.mode === 'SSB')).toBeTruthy();
  });

  it('should filter QSOs by date range', () => {
    component.filterStartDate = '2025-01-15';
    component.filterEndDate = '2025-01-15';
    component.applyFilters();

    expect(component.filteredQSOs.every(q => q.qsoDate === '2025-01-15')).toBeTruthy();
  });

  it('should sort QSOs by date descending', () => {
    component.sortBy = 'date';
    component.sortDirection = 'desc';
    component.sortQSOs();

    expect(component.filteredQSOs[0].qsoDate).toBe('2025-01-15');
  });

  it('should sort QSOs by callsign ascending', () => {
    component.sortBy = 'callsign';
    component.sortDirection = 'asc';
    component.sortQSOs();

    expect(component.filteredQSOs[0].callsign).toBe('K2ABC');
  });

  it('should sort QSOs by frequency', () => {
    component.sortBy = 'frequency';
    component.sortDirection = 'asc';
    component.sortQSOs();

    expect(component.filteredQSOs[0].frequencyKhz).toBe(7125);
  });

  it('should toggle sort direction', () => {
    component.sortDirection = 'asc';
    component.toggleSortDirection();
    expect(component.sortDirection).toBe('desc');

    component.toggleSortDirection();
    expect(component.sortDirection).toBe('asc');
  });

  it('should paginate QSOs', () => {
    component.pageSize = 2;
    component.currentPage = 0;

    const paginated = component.getPaginatedQSOs();

    expect(paginated.length).toBe(2);
  });

  it('should navigate to next page', () => {
    component.pageSize = 2;
    component.currentPage = 0;
    component.totalPages = 2;

    component.nextPage();

    expect(component.currentPage).toBe(1);
  });

  it('should navigate to previous page', () => {
    component.pageSize = 2;
    component.currentPage = 1;

    component.previousPage();

    expect(component.currentPage).toBe(0);
  });

  it('should not go beyond last page', () => {
    component.pageSize = 2;
    component.currentPage = 1;
    component.totalPages = 2;

    component.nextPage();

    expect(component.currentPage).toBe(1);
  });

  it('should select QSO for editing', () => {
    const qso = mockQSOs[0];

    component.selectQSO(qso);

    expect(component.selectedQSO).toEqual(qso);
    expect(component.editMode).toBeTruthy();
  });

  it('should delete QSO after confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.deleteQSO.and.returnValue(of({}));

    component.deleteQSO(1);

    expect(apiService.deleteQSO).toHaveBeenCalledWith(1);
  });

  it('should not delete QSO if cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.deleteQSO(1);

    expect(apiService.deleteQSO).not.toHaveBeenCalled();
  });

  it('should refresh QSO list after deletion', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.deleteQSO.and.returnValue(of({}));
    spyOn(component, 'loadQSOs');

    component.deleteQSO(1);

    expect(component.loadQSOs).toHaveBeenCalled();
  });

  it('should search QSOs', () => {
    apiService.searchQSOs.and.returnValue(of([mockQSOs[0]]));

    component.searchTerm = 'W1AW';
    component.search();

    expect(apiService.searchQSOs).toHaveBeenCalledWith(1, 'W1AW');
    expect(component.qsos.length).toBe(1);
  });

  it('should clear search and reload all QSOs', () => {
    component.searchTerm = 'W1AW';
    spyOn(component, 'loadQSOs');

    component.clearSearch();

    expect(component.searchTerm).toBe('');
    expect(component.loadQSOs).toHaveBeenCalled();
  });

  it('should export QSOs as ADIF', () => {
    const adifData = '<ADIF_VER:5>3.1.4<EOH>';
    apiService.exportAdif.and.returnValue(of(adifData));
    spyOn(component, 'downloadFile');

    component.exportAsAdif();

    expect(apiService.exportAdif).toHaveBeenCalledWith(1);
    expect(component.downloadFile).toHaveBeenCalled();
  });

  it('should export QSOs as Cabrillo', () => {
    const cabrilloData = 'START-OF-LOG: 3.0';
    apiService.exportCabrillo.and.returnValue(of(cabrilloData));
    spyOn(component, 'downloadFile');

    component.exportAsCabrillo();

    expect(apiService.exportCabrillo).toHaveBeenCalledWith(1);
    expect(component.downloadFile).toHaveBeenCalled();
  });

  it('should select all QSOs', () => {
    component.selectAll();

    expect(component.selectedQSOs.length).toBe(3);
    expect(component.allSelected).toBeTruthy();
  });

  it('should deselect all QSOs', () => {
    component.selectAll();
    component.deselectAll();

    expect(component.selectedQSOs.length).toBe(0);
    expect(component.allSelected).toBeFalsy();
  });

  it('should toggle individual QSO selection', () => {
    const qso = mockQSOs[0];

    component.toggleSelection(qso);
    expect(component.isSelected(qso)).toBeTruthy();

    component.toggleSelection(qso);
    expect(component.isSelected(qso)).toBeFalsy();
  });

  it('should delete multiple selected QSOs', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.deleteQSO.and.returnValue(of({}));
    component.selectedQSOs = [mockQSOs[0], mockQSOs[1]];

    component.deleteSelected();

    expect(apiService.deleteQSO).toHaveBeenCalledTimes(2);
  });

  it('should display QSO count', () => {
    expect(component.getTotalQSOs()).toBe(3);
  });

  it('should display unique callsign count', () => {
    expect(component.getUniqueCallsigns()).toBe(3);
  });

  it('should calculate band distribution', () => {
    const distribution = component.getBandDistribution();

    expect(distribution['20m']).toBe(1);
    expect(distribution['40m']).toBe(1);
    expect(distribution['15m']).toBe(1);
  });

  it('should calculate mode distribution', () => {
    const distribution = component.getModeDistribution();

    expect(distribution['SSB']).toBe(1);
    expect(distribution['CW']).toBe(1);
    expect(distribution['FT8']).toBe(1);
  });

  it('should highlight duplicate QSOs', () => {
    component.qsos = [
      { id: 1, callsign: 'W1AW', qsoDate: '2025-01-15', timeOn: '14:30' },
      { id: 2, callsign: 'W1AW', qsoDate: '2025-01-15', timeOn: '14:30' }
    ];

    const isDuplicate = component.isPossibleDuplicate(component.qsos[1]);

    expect(isDuplicate).toBeTruthy();
  });

  it('should handle empty QSO list', () => {
    apiService.getQSOs.and.returnValue(of([]));

    component.loadQSOs();

    expect(component.qsos.length).toBe(0);
    expect(component.hasQSOs()).toBeFalsy();
  });

  it('should handle API errors gracefully', () => {
    apiService.getQSOs.and.returnValue(throwError(() => new Error('Network error')));

    component.loadQSOs();

    expect(component.errorMessage).toBeTruthy();
  });

  it('should format frequency for display', () => {
    const formatted = component.formatFrequency(14250);
    expect(formatted).toBe('14.250 MHz');
  });

  it('should determine band from frequency', () => {
    expect(component.getBand(14250)).toBe('20m');
    expect(component.getBand(7125)).toBe('40m');
    expect(component.getBand(21200)).toBe('15m');
  });

  it('should show loading indicator while fetching', () => {
    component.loading = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const spinner = compiled.querySelector('.loading-spinner');
    expect(spinner).toBeTruthy();
  });

  it('should show empty state when no QSOs', () => {
    component.qsos = [];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const emptyState = compiled.querySelector('.empty-state');
    expect(emptyState).toBeTruthy();
  });

  it('should reset filters', () => {
    component.filterCallsign = 'W1AW';
    component.filterBand = '20m';
    component.filterMode = 'SSB';

    component.resetFilters();

    expect(component.filterCallsign).toBe('');
    expect(component.filterBand).toBe('');
    expect(component.filterMode).toBe('');
  });

  it('should apply multiple filters simultaneously', () => {
    component.filterMode = 'SSB';
    component.filterStartDate = '2025-01-15';
    component.applyFilters();

    expect(component.filteredQSOs.every(q =>
      q.mode === 'SSB' && q.qsoDate === '2025-01-15'
    )).toBeTruthy();
  });

  it('should update page count when page size changes', () => {
    component.pageSize = 10;
    component.updatePagination();

    const expectedPages = Math.ceil(mockQSOs.length / 10);
    expect(component.totalPages).toBe(expectedPages);
  });
});
