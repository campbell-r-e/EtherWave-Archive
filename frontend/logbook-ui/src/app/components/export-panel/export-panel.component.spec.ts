import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ExportPanelComponent } from './export-panel.component';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log.service';

describe('ExportPanelComponent', () => {
  let component: ExportPanelComponent;
  let fixture: ComponentFixture<ExportPanelComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let logService: jasmine.SpyObj<LogService>;

  const mockQSOs = [
    { id: 1, callsign: 'W1AW', frequencyKhz: 14250, mode: 'SSB', qsoDate: '2025-01-15' },
    { id: 2, callsign: 'K2ABC', frequencyKhz: 7125, mode: 'CW', qsoDate: '2025-01-15' },
    { id: 3, callsign: 'N3XYZ', frequencyKhz: 21200, mode: 'FT8', qsoDate: '2025-01-14' }
  ];

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'exportAdif',
      'exportCabrillo',
      'exportCSV',
      'exportJSON',
      'getQSOs'
    ]);
    const logServiceSpy = jasmine.createSpyObj('LogService', ['getCurrentLog', 'getQSOCount']);

    await TestBed.configureTestingModule({
      imports: [ExportPanelComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: LogService, useValue: logServiceSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    logService = TestBed.inject(LogService) as jasmine.SpyObj<LogService>;

    logService.getCurrentLog.and.returnValue({ id: 1, name: 'Test Log', contestCode: 'ARRL-FD' });
    logService.getQSOCount.and.returnValue(3);
    apiService.getQSOs.and.returnValue(of(mockQSOs));

    fixture = TestBed.createComponent(ExportPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== ADIF EXPORT TESTS ====================

  it('should export as ADIF', () => {
    const adifData = '<ADIF_VER:5>3.1.4<EOH><CALL:4>W1AW<EOR>';
    apiService.exportAdif.and.returnValue(of(adifData));
    spyOn(component, 'downloadFile');

    component.exportFormat = 'ADIF';
    component.exportLog();

    expect(apiService.exportAdif).toHaveBeenCalledWith(1);
    expect(component.downloadFile).toHaveBeenCalled();
  });

  it('should generate correct ADIF filename', () => {
    const filename = component.generateFilename('ADIF');

    expect(filename).toContain('Test_Log');
    expect(filename).toContain('.adi');
  });

  it('should include date in ADIF filename', () => {
    const filename = component.generateFilename('ADIF');
    const today = new Date().toISOString().split('T')[0];

    expect(filename).toContain(today.replace(/-/g, ''));
  });

  it('should handle ADIF export error', () => {
    apiService.exportAdif.and.returnValue(throwError(() => new Error('Export failed')));

    component.exportFormat = 'ADIF';
    component.exportLog();

    expect(component.errorMessage).toBeTruthy();
  });

  it('should show ADIF version selector', () => {
    component.exportFormat = 'ADIF';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('select[name="adifVersion"]')).toBeTruthy();
  });

  it('should support ADIF 3.1.4', () => {
    component.adifVersion = '3.1.4';
    apiService.exportAdif.and.returnValue(of('<ADIF_VER:5>3.1.4<EOH>'));
    spyOn(component, 'downloadFile');

    component.exportFormat = 'ADIF';
    component.exportLog();

    expect(apiService.exportAdif).toHaveBeenCalled();
  });

  // ==================== CABRILLO EXPORT TESTS ====================

  it('should export as Cabrillo', () => {
    const cabrilloData = 'START-OF-LOG: 3.0\nCONTEST: ARRL-FD\nEND-OF-LOG:';
    apiService.exportCabrillo.and.returnValue(of(cabrilloData));
    spyOn(component, 'downloadFile');

    component.exportFormat = 'Cabrillo';
    component.exportLog();

    expect(apiService.exportCabrillo).toHaveBeenCalledWith(1);
    expect(component.downloadFile).toHaveBeenCalled();
  });

  it('should generate correct Cabrillo filename', () => {
    const filename = component.generateFilename('Cabrillo');

    expect(filename).toContain('.cbr');
  });

  it('should require contest for Cabrillo export', () => {
    logService.getCurrentLog.and.returnValue({ id: 1, name: 'Test Log', contestCode: null });

    component.exportFormat = 'Cabrillo';
    component.exportLog();

    expect(component.validationError).toContain('contest');
  });

  it('should show Cabrillo header fields', () => {
    component.exportFormat = 'Cabrillo';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('input[name="operatorName"]')).toBeTruthy();
    expect(compiled.querySelector('input[name="stationCallsign"]')).toBeTruthy();
  });

  it('should validate Cabrillo required fields', () => {
    component.exportFormat = 'Cabrillo';
    component.operatorName = '';

    component.exportLog();

    expect(component.validationError).toBeTruthy();
  });

  // ==================== CSV EXPORT TESTS ====================

  it('should export as CSV', () => {
    const csvData = 'Callsign,Frequency,Mode,Date\nW1AW,14250,SSB,2025-01-15';
    apiService.exportCSV.and.returnValue(of(csvData));
    spyOn(component, 'downloadFile');

    component.exportFormat = 'CSV';
    component.exportLog();

    expect(apiService.exportCSV).toHaveBeenCalledWith(1);
    expect(component.downloadFile).toHaveBeenCalled();
  });

  it('should generate correct CSV filename', () => {
    const filename = component.generateFilename('CSV');

    expect(filename).toContain('.csv');
  });

  it('should allow custom CSV field selection', () => {
    component.exportFormat = 'CSV';
    component.selectedFields = ['callsign', 'frequency', 'mode'];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.field-selector')).toBeTruthy();
  });

  it('should show CSV delimiter options', () => {
    component.exportFormat = 'CSV';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('select[name="csvDelimiter"]')).toBeTruthy();
  });

  it('should support comma and tab delimiters', () => {
    expect(component.getAvailableDelimiters()).toContain(',');
    expect(component.getAvailableDelimiters()).toContain('\\t');
  });

  // ==================== JSON EXPORT TESTS ====================

  it('should export as JSON', () => {
    const jsonData = JSON.stringify(mockQSOs);
    apiService.exportJSON.and.returnValue(of(jsonData));
    spyOn(component, 'downloadFile');

    component.exportFormat = 'JSON';
    component.exportLog();

    expect(apiService.exportJSON).toHaveBeenCalledWith(1);
    expect(component.downloadFile).toHaveBeenCalled();
  });

  it('should generate correct JSON filename', () => {
    const filename = component.generateFilename('JSON');

    expect(filename).toContain('.json');
  });

  it('should support pretty-print JSON', () => {
    component.jsonPrettyPrint = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('input[name="prettyPrint"]')).toBeTruthy();
  });

  // ==================== DATE RANGE FILTER TESTS ====================

  it('should filter by date range', () => {
    component.startDate = '2025-01-15';
    component.endDate = '2025-01-15';
    component.applyDateFilter = true;

    const filtered = component.getFilteredQSOs();

    expect(filtered.every(q => q.qsoDate === '2025-01-15')).toBeTruthy();
  });

  it('should show date range inputs', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;

    expect(compiled.querySelector('input[name="startDate"]')).toBeTruthy();
    expect(compiled.querySelector('input[name="endDate"]')).toBeTruthy();
  });

  it('should validate date range', () => {
    component.startDate = '2025-01-20';
    component.endDate = '2025-01-10';

    const isValid = component.isValidDateRange();

    expect(isValid).toBeFalsy();
    expect(component.validationError).toContain('date range');
  });

  it('should export all QSOs when no date filter', () => {
    component.applyDateFilter = false;

    const filtered = component.getFilteredQSOs();

    expect(filtered.length).toBe(3);
  });

  // ==================== BAND FILTER TESTS ====================

  it('should filter by band', () => {
    component.filterBand = '20m';
    component.applyBandFilter = true;

    const filtered = component.getFilteredQSOs();

    expect(filtered.every(q => component.getBand(q.frequencyKhz) === '20m')).toBeTruthy();
  });

  it('should show band selector', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;

    expect(compiled.querySelector('select[name="bandFilter"]')).toBeTruthy();
  });

  it('should provide list of bands', () => {
    const bands = component.getAvailableBands();

    expect(bands).toContain('20m');
    expect(bands).toContain('40m');
    expect(bands).toContain('15m');
  });

  // ==================== MODE FILTER TESTS ====================

  it('should filter by mode', () => {
    component.filterMode = 'SSB';
    component.applyModeFilter = true;

    const filtered = component.getFilteredQSOs();

    expect(filtered.every(q => q.mode === 'SSB')).toBeTruthy();
  });

  it('should show mode selector', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;

    expect(compiled.querySelector('select[name="modeFilter"]')).toBeTruthy();
  });

  // ==================== EXPORT PREVIEW TESTS ====================

  it('should show export preview', () => {
    component.showPreview = true;
    component.previewData = 'Sample export data...';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.preview-panel')).toBeTruthy();
  });

  it('should generate preview for ADIF', () => {
    apiService.exportAdif.and.returnValue(of('<ADIF_VER:5>3.1.4<EOH>'));

    component.exportFormat = 'ADIF';
    component.generatePreview();

    expect(component.previewData).toBeTruthy();
  });

  it('should limit preview to first 100 lines', () => {
    const largeData = Array(200).fill('test').join('\n');
    apiService.exportAdif.and.returnValue(of(largeData));

    component.exportFormat = 'ADIF';
    component.generatePreview();

    expect(component.previewData.split('\n').length).toBeLessThanOrEqual(100);
  });

  it('should close preview', () => {
    component.showPreview = true;
    component.closePreview();

    expect(component.showPreview).toBeFalsy();
  });

  // ==================== DOWNLOAD TESTS ====================

  it('should trigger file download', () => {
    const blob = new Blob(['test data'], { type: 'text/plain' });
    spyOn(document, 'createElement').and.returnValue(document.createElement('a'));

    component.downloadFile(blob, 'test.adi');

    expect(document.createElement).toHaveBeenCalledWith('a');
  });

  it('should set correct MIME type for ADIF', () => {
    const mimeType = component.getMimeType('ADIF');

    expect(mimeType).toBe('text/plain');
  });

  it('should set correct MIME type for CSV', () => {
    const mimeType = component.getMimeType('CSV');

    expect(mimeType).toBe('text/csv');
  });

  it('should set correct MIME type for JSON', () => {
    const mimeType = component.getMimeType('JSON');

    expect(mimeType).toBe('application/json');
  });

  // ==================== PROGRESS TRACKING TESTS ====================

  it('should show export progress', () => {
    component.exporting = true;
    component.exportProgress = 50;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.progress-bar')).toBeTruthy();
  });

  it('should update progress during export', () => {
    apiService.exportAdif.and.returnValue(of('<ADIF_VER:5>3.1.4<EOH>'));

    component.exportFormat = 'ADIF';
    component.exportLog();

    expect(component.exporting).toBeTruthy();
  });

  it('should reset progress after completion', () => {
    component.exporting = true;
    component.exportProgress = 100;

    setTimeout(() => {
      expect(component.exportProgress).toBe(0);
    }, 1000);
  });

  // ==================== QSO COUNT TESTS ====================

  it('should display QSO count to be exported', () => {
    component.qsoCount = 3;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('3 QSOs');
  });

  it('should update count when filters change', () => {
    component.filterBand = '20m';
    component.applyBandFilter = true;

    component.updateQSOCount();

    expect(component.qsoCount).toBeLessThan(3);
  });

  it('should warn when no QSOs to export', () => {
    apiService.getQSOs.and.returnValue(of([]));

    component.updateQSOCount();

    expect(component.warningMessage).toContain('No QSOs');
  });

  // ==================== FORMAT VALIDATION TESTS ====================

  it('should validate export format selection', () => {
    component.exportFormat = '';

    component.exportLog();

    expect(component.validationError).toContain('format');
  });

  it('should provide list of export formats', () => {
    const formats = component.getAvailableFormats();

    expect(formats).toContain('ADIF');
    expect(formats).toContain('Cabrillo');
    expect(formats).toContain('CSV');
    expect(formats).toContain('JSON');
  });

  // ==================== FIELD SELECTION TESTS ====================

  it('should show field selection for CSV', () => {
    component.exportFormat = 'CSV';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.field-selection')).toBeTruthy();
  });

  it('should provide list of available fields', () => {
    const fields = component.getAvailableFields();

    expect(fields).toContain('callsign');
    expect(fields).toContain('frequency');
    expect(fields).toContain('mode');
    expect(fields).toContain('qsoDate');
  });

  it('should select all fields by default', () => {
    component.selectAllFields();

    expect(component.selectedFields.length).toBeGreaterThan(0);
  });

  it('should deselect all fields', () => {
    component.deselectAllFields();

    expect(component.selectedFields.length).toBe(0);
  });

  it('should toggle field selection', () => {
    component.selectedFields = ['callsign'];
    component.toggleField('frequency');

    expect(component.selectedFields).toContain('frequency');

    component.toggleField('frequency');

    expect(component.selectedFields).not.toContain('frequency');
  });

  // ==================== UI STATE TESTS ====================

  it('should show loading state during export', () => {
    component.exporting = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.loading-spinner')).toBeTruthy();
  });

  it('should disable export button during export', () => {
    component.exporting = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const exportBtn = compiled.querySelector('.export-button');
    expect(exportBtn?.getAttribute('disabled')).toBeTruthy();
  });

  it('should show success message after export', () => {
    component.exportComplete = true;
    component.successMessage = 'Export completed';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.success-message')).toBeTruthy();
  });

  // ==================== ERROR HANDLING TESTS ====================

  it('should handle export timeout', () => {
    jasmine.clock().install();

    apiService.exportAdif.and.returnValue(of('<ADIF_VER:5>3.1.4<EOH>').pipe());
    component.exportFormat = 'ADIF';
    component.exportLog();

    jasmine.clock().tick(30000);

    expect(component.timeoutWarning).toBeTruthy();

    jasmine.clock().uninstall();
  });

  it('should allow retry after error', () => {
    component.errorMessage = 'Export failed';
    apiService.exportAdif.and.returnValue(of('<ADIF_VER:5>3.1.4<EOH>'));
    spyOn(component, 'downloadFile');

    component.exportFormat = 'ADIF';
    component.retry();

    expect(component.errorMessage).toBe('');
    expect(apiService.exportAdif).toHaveBeenCalled();
  });

  // ==================== ACCESSIBILITY TESTS ====================

  it('should have accessible format selector', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const formatSelect = compiled.querySelector('select[name="exportFormat"]');

    expect(formatSelect?.getAttribute('aria-label')).toBeTruthy();
  });

  it('should announce export completion', () => {
    spyOn(component, 'announceToScreenReader');
    apiService.exportAdif.and.returnValue(of('<ADIF_VER:5>3.1.4<EOH>'));
    spyOn(component, 'downloadFile');

    component.exportFormat = 'ADIF';
    component.exportLog();

    expect(component.announceToScreenReader).toHaveBeenCalled();
  });

  // ==================== RESET TESTS ====================

  it('should reset export settings', () => {
    component.exportFormat = 'ADIF';
    component.startDate = '2025-01-01';
    component.filterBand = '20m';

    component.resetSettings();

    expect(component.exportFormat).toBe('');
    expect(component.startDate).toBe('');
    expect(component.filterBand).toBe('');
  });

  it('should clear messages on reset', () => {
    component.successMessage = 'Success';
    component.errorMessage = 'Error';

    component.resetSettings();

    expect(component.successMessage).toBe('');
    expect(component.errorMessage).toBe('');
  });
});
