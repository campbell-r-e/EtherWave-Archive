import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ImportPanelComponent } from './import-panel.component';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log.service';

describe('ImportPanelComponent', () => {
  let component: ImportPanelComponent;
  let fixture: ComponentFixture<ImportPanelComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let logService: jasmine.SpyObj<LogService>;

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'importAdif',
      'importCabrillo',
      'validateAdifFile',
      'validateCabrilloFile',
      'previewImport'
    ]);
    const logServiceSpy = jasmine.createSpyObj('LogService', ['getCurrentLog', 'refreshQSOs']);

    await TestBed.configureTestingModule({
      imports: [ImportPanelComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: LogService, useValue: logServiceSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    logService = TestBed.inject(LogService) as jasmine.SpyObj<LogService>;

    logService.getCurrentLog.and.returnValue({ id: 1, name: 'Test Log' });

    fixture = TestBed.createComponent(ImportPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== FILE SELECTION TESTS ====================

  it('should handle file selection', () => {
    const file = new File(['test content'], 'test.adi', { type: 'text/plain' });
    const event = { target: { files: [file] } };

    component.onFileSelected(event);

    expect(component.selectedFile).toEqual(file);
    expect(component.fileName).toBe('test.adi');
  });

  it('should validate file extension for ADIF', () => {
    const file = new File(['test'], 'test.adi', { type: 'text/plain' });
    const event = { target: { files: [file] } };

    component.importFormat = 'ADIF';
    component.onFileSelected(event);

    expect(component.isValidFileType()).toBeTruthy();
  });

  it('should reject invalid file type', () => {
    const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
    const event = { target: { files: [file] } };

    component.importFormat = 'ADIF';
    component.onFileSelected(event);

    expect(component.validationError).toContain('file type');
  });

  it('should accept .adi and .adif extensions', () => {
    expect(component.isValidExtension('test.adi', 'ADIF')).toBeTruthy();
    expect(component.isValidExtension('test.adif', 'ADIF')).toBeTruthy();
    expect(component.isValidExtension('test.ADI', 'ADIF')).toBeTruthy();
  });

  it('should accept .cbr extension for Cabrillo', () => {
    expect(component.isValidExtension('test.cbr', 'Cabrillo')).toBeTruthy();
    expect(component.isValidExtension('test.log', 'Cabrillo')).toBeTruthy();
  });

  it('should validate file size', () => {
    const largeContent = 'x'.repeat(11 * 1024 * 1024); // 11 MB
    const file = new File([largeContent], 'large.adi', { type: 'text/plain' });

    expect(component.isValidFileSize(file)).toBeFalsy();
  });

  it('should clear file selection', () => {
    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    component.fileName = 'test.adi';

    component.clearFile();

    expect(component.selectedFile).toBeNull();
    expect(component.fileName).toBe('');
  });

  // ==================== ADIF IMPORT TESTS ====================

  it('should import ADIF file', () => {
    const file = new File(['<ADIF_VER:5>3.1.4<EOH>'], 'test.adi', { type: 'text/plain' });
    const response = { success: true, imported: 10, duplicates: 0 };
    apiService.importAdif.and.returnValue(of(response));

    component.selectedFile = file;
    component.importFormat = 'ADIF';
    component.importFile();

    expect(apiService.importAdif).toHaveBeenCalled();
  });

  it('should show import success message', () => {
    const file = new File(['<ADIF_VER:5>3.1.4<EOH>'], 'test.adi', { type: 'text/plain' });
    const response = { success: true, imported: 10, duplicates: 0 };
    apiService.importAdif.and.returnValue(of(response));

    component.selectedFile = file;
    component.importFormat = 'ADIF';
    component.importFile();

    expect(component.successMessage).toContain('10 QSOs imported');
  });

  it('should show duplicate warning', () => {
    const file = new File(['<ADIF_VER:5>3.1.4<EOH>'], 'test.adi', { type: 'text/plain' });
    const response = { success: true, imported: 8, duplicates: 2 };
    apiService.importAdif.and.returnValue(of(response));

    component.selectedFile = file;
    component.importFormat = 'ADIF';
    component.importFile();

    expect(component.warningMessage).toContain('2 duplicates');
  });

  it('should handle import error', () => {
    const file = new File(['invalid'], 'test.adi', { type: 'text/plain' });
    apiService.importAdif.and.returnValue(throwError(() => new Error('Invalid ADIF')));

    component.selectedFile = file;
    component.importFormat = 'ADIF';
    component.importFile();

    expect(component.errorMessage).toBeTruthy();
  });

  it('should validate ADIF format before import', () => {
    const file = new File(['<ADIF_VER:5>3.1.4<EOH>'], 'test.adi', { type: 'text/plain' });
    apiService.validateAdifFile.and.returnValue(of({ valid: true }));

    component.selectedFile = file;
    component.validateFile();

    expect(apiService.validateAdifFile).toHaveBeenCalled();
  });

  it('should show validation errors', () => {
    const file = new File(['invalid'], 'test.adi', { type: 'text/plain' });
    apiService.validateAdifFile.and.returnValue(of({
      valid: false,
      errors: ['Missing ADIF header', 'Invalid field']
    }));

    component.selectedFile = file;
    component.validateFile();

    expect(component.validationErrors.length).toBe(2);
  });

  // ==================== CABRILLO IMPORT TESTS ====================

  it('should import Cabrillo file', () => {
    const file = new File(['START-OF-LOG: 3.0'], 'test.cbr', { type: 'text/plain' });
    const response = { success: true, imported: 15, duplicates: 0 };
    apiService.importCabrillo.and.returnValue(of(response));

    component.selectedFile = file;
    component.importFormat = 'Cabrillo';
    component.importFile();

    expect(apiService.importCabrillo).toHaveBeenCalled();
  });

  it('should validate Cabrillo format', () => {
    const file = new File(['START-OF-LOG: 3.0'], 'test.cbr', { type: 'text/plain' });
    apiService.validateCabrilloFile.and.returnValue(of({ valid: true }));

    component.selectedFile = file;
    component.importFormat = 'Cabrillo';
    component.validateFile();

    expect(apiService.validateCabrilloFile).toHaveBeenCalled();
  });

  // ==================== IMPORT PREVIEW TESTS ====================

  it('should show import preview', () => {
    const file = new File(['<ADIF_VER:5>3.1.4<EOH>'], 'test.adi', { type: 'text/plain' });
    const previewData = {
      totalRecords: 10,
      sampleQSOs: [
        { callsign: 'W1AW', frequencyKhz: 14250, mode: 'SSB' },
        { callsign: 'K2ABC', frequencyKhz: 7125, mode: 'CW' }
      ]
    };
    apiService.previewImport.and.returnValue(of(previewData));

    component.selectedFile = file;
    component.previewImport();

    expect(component.previewData).toEqual(previewData);
    expect(component.showPreview).toBeTruthy();
  });

  it('should display preview summary', () => {
    component.previewData = {
      totalRecords: 10,
      sampleQSOs: [{ callsign: 'W1AW', frequencyKhz: 14250, mode: 'SSB' }]
    };
    component.showPreview = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('10 records');
  });

  it('should show sample QSOs in preview', () => {
    component.previewData = {
      totalRecords: 10,
      sampleQSOs: [
        { callsign: 'W1AW', frequencyKhz: 14250, mode: 'SSB' },
        { callsign: 'K2ABC', frequencyKhz: 7125, mode: 'CW' }
      ]
    };
    component.showPreview = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('W1AW');
    expect(compiled.textContent).toContain('K2ABC');
  });

  it('should allow confirming import after preview', () => {
    component.showPreview = true;
    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    apiService.importAdif.and.returnValue(of({ success: true, imported: 10 }));

    component.confirmImport();

    expect(apiService.importAdif).toHaveBeenCalled();
  });

  it('should allow canceling preview', () => {
    component.showPreview = true;
    component.cancelPreview();

    expect(component.showPreview).toBeFalsy();
  });

  // ==================== DUPLICATE HANDLING TESTS ====================

  it('should show duplicate handling options', () => {
    component.hasDuplicates = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.duplicate-options')).toBeTruthy();
  });

  it('should skip duplicates on import', () => {
    component.duplicateHandling = 'skip';
    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    apiService.importAdif.and.returnValue(of({ success: true, imported: 8, skipped: 2 }));

    component.importFile();

    expect(component.successMessage).toContain('2 skipped');
  });

  it('should update duplicates on import', () => {
    component.duplicateHandling = 'update';
    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    apiService.importAdif.and.returnValue(of({ success: true, imported: 10, updated: 2 }));

    component.importFile();

    expect(component.successMessage).toContain('2 updated');
  });

  it('should create duplicates with suffix', () => {
    component.duplicateHandling = 'create';
    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    apiService.importAdif.and.returnValue(of({ success: true, imported: 12 }));

    component.importFile();

    expect(apiService.importAdif).toHaveBeenCalled();
  });

  // ==================== PROGRESS TRACKING TESTS ====================

  it('should show import progress', () => {
    component.importing = true;
    component.importProgress = 45;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.progress-bar')).toBeTruthy();
  });

  it('should update progress during import', () => {
    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    apiService.importAdif.and.returnValue(of({ success: true, imported: 10 }));

    component.importFile();

    expect(component.importing).toBeTruthy();
  });

  it('should reset progress after completion', () => {
    component.importing = true;
    component.importProgress = 100;
    apiService.importAdif.and.returnValue(of({ success: true, imported: 10 }));

    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    component.importFile();

    setTimeout(() => {
      expect(component.importProgress).toBe(0);
    }, 1000);
  });

  // ==================== FORMAT DETECTION TESTS ====================

  it('should auto-detect ADIF format', () => {
    const file = new File(['<ADIF_VER:5>3.1.4<EOH>'], 'test.adi', { type: 'text/plain' });

    const format = component.detectFormat(file);

    expect(format).toBe('ADIF');
  });

  it('should auto-detect Cabrillo format', () => {
    const file = new File(['START-OF-LOG: 3.0'], 'test.cbr', { type: 'text/plain' });

    const format = component.detectFormat(file);

    expect(format).toBe('Cabrillo');
  });

  it('should set format on file selection', () => {
    const file = new File(['<ADIF_VER:5>3.1.4<EOH>'], 'test.adi', { type: 'text/plain' });
    const event = { target: { files: [file] } };

    component.onFileSelected(event);

    expect(component.importFormat).toBe('ADIF');
  });

  // ==================== ERROR HANDLING TESTS ====================

  it('should show parsing errors', () => {
    const file = new File(['invalid data'], 'test.adi', { type: 'text/plain' });
    apiService.importAdif.and.returnValue(of({
      success: false,
      errors: ['Line 1: Invalid ADIF tag', 'Line 5: Missing required field']
    }));

    component.selectedFile = file;
    component.importFile();

    expect(component.importErrors.length).toBe(2);
  });

  it('should show line numbers for errors', () => {
    component.importErrors = ['Line 1: Invalid tag', 'Line 5: Missing field'];
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Line 1');
    expect(compiled.textContent).toContain('Line 5');
  });

  it('should allow retry after error', () => {
    component.errorMessage = 'Import failed';
    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    apiService.importAdif.and.returnValue(of({ success: true, imported: 10 }));

    component.retry();

    expect(apiService.importAdif).toHaveBeenCalled();
    expect(component.errorMessage).toBe('');
  });

  // ==================== REFRESH TESTS ====================

  it('should refresh QSO list after import', () => {
    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    apiService.importAdif.and.returnValue(of({ success: true, imported: 10 }));

    component.importFile();

    expect(logService.refreshQSOs).toHaveBeenCalled();
  });

  // ==================== UI STATE TESTS ====================

  it('should show loading state during import', () => {
    component.importing = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.loading-spinner')).toBeTruthy();
  });

  it('should disable import button when no file', () => {
    component.selectedFile = null;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const importBtn = compiled.querySelector('.import-button');
    expect(importBtn?.getAttribute('disabled')).toBeTruthy();
  });

  it('should disable import button during import', () => {
    component.importing = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const importBtn = compiled.querySelector('.import-button');
    expect(importBtn?.getAttribute('disabled')).toBeTruthy();
  });

  it('should show success state after import', () => {
    component.importComplete = true;
    component.successMessage = '10 QSOs imported';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.success-message')).toBeTruthy();
  });

  // ==================== DRAG AND DROP TESTS ====================

  it('should handle drag over event', () => {
    const event = new DragEvent('dragover');
    spyOn(event, 'preventDefault');

    component.onDragOver(event);

    expect(event.preventDefault).toHaveBeenCalled();
    expect(component.dragging).toBeTruthy();
  });

  it('should handle drag leave event', () => {
    component.dragging = true;

    component.onDragLeave();

    expect(component.dragging).toBeFalsy();
  });

  it('should handle file drop', () => {
    const file = new File(['test'], 'test.adi', { type: 'text/plain' });
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    const event = new DragEvent('drop', { dataTransfer });
    spyOn(event, 'preventDefault');

    component.onDrop(event);

    expect(event.preventDefault).toHaveBeenCalled();
  });

  // ==================== ACCESSIBILITY TESTS ====================

  it('should have accessible file input', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const fileInput = compiled.querySelector('input[type="file"]');

    expect(fileInput?.getAttribute('aria-label')).toBeTruthy();
  });

  it('should announce import completion', () => {
    spyOn(component, 'announceToScreenReader');
    apiService.importAdif.and.returnValue(of({ success: true, imported: 10 }));

    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    component.importFile();

    expect(component.announceToScreenReader).toHaveBeenCalled();
  });

  // ==================== RESET TESTS ====================

  it('should reset component state', () => {
    component.selectedFile = new File(['test'], 'test.adi', { type: 'text/plain' });
    component.importProgress = 50;
    component.successMessage = 'Imported';

    component.reset();

    expect(component.selectedFile).toBeNull();
    expect(component.importProgress).toBe(0);
    expect(component.successMessage).toBe('');
  });
});
