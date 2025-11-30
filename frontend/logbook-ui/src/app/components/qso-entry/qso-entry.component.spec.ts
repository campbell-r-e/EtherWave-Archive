import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { QsoEntryComponent } from './qso-entry.component';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log.service';

describe('QsoEntryComponent', () => {
  let component: QsoEntryComponent;
  let fixture: ComponentFixture<QsoEntryComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let logService: jasmine.SpyObj<LogService>;

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', ['createQSO', 'updateQSO']);
    const logServiceSpy = jasmine.createSpyObj('LogService', ['getCurrentLog']);

    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, QsoEntryComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: LogService, useValue: logServiceSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    logService = TestBed.inject(LogService) as jasmine.SpyObj<LogService>;

    logService.getCurrentLog.and.returnValue({ id: 1, name: 'Test Log' });

    fixture = TestBed.createComponent(QsoEntryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ==================== COMPONENT INITIALIZATION TESTS ====================

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize QSO form with UTC time', () => {
    const qsoDate = component.qsoForm.get('qsoDate')?.value;
    const timeOn = component.qsoForm.get('timeOn')?.value;

    expect(qsoDate).toBeTruthy();
    expect(timeOn).toBeTruthy();
  });

  // ==================== CALLSIGN VALIDATION TESTS ====================

  it('should require callsign', () => {
    const callsign = component.qsoForm.get('callsign');
    expect(callsign?.hasError('required')).toBeTruthy();
  });

  it('should validate callsign format', () => {
    const callsign = component.qsoForm.get('callsign');

    callsign?.setValue('W1AW');
    expect(callsign?.valid).toBeTruthy();

    callsign?.setValue('INVALID123');
    expect(callsign?.hasError('pattern')).toBeTruthy();
  });

  it('should convert callsign to uppercase', () => {
    component.qsoForm.get('callsign')?.setValue('w1aw');
    component.normalizeCallsign();

    expect(component.qsoForm.get('callsign')?.value).toBe('W1AW');
  });

  // ==================== FREQUENCY VALIDATION TESTS ====================

  it('should require frequency', () => {
    const frequency = component.qsoForm.get('frequencyKhz');
    expect(frequency?.hasError('required')).toBeTruthy();
  });

  it('should validate frequency range', () => {
    const frequency = component.qsoForm.get('frequencyKhz');

    frequency?.setValue(100); // Too low
    expect(frequency?.hasError('min')).toBeTruthy();

    frequency?.setValue(14250); // Valid 20m
    expect(frequency?.valid).toBeTruthy();

    frequency?.setValue(1000000); // Too high
    expect(frequency?.hasError('max')).toBeTruthy();
  });

  it('should auto-determine band from frequency', () => {
    component.qsoForm.get('frequencyKhz')?.setValue(14250);
    component.determineBand();

    expect(component.qsoForm.get('band')?.value).toBe('20m');

    component.qsoForm.get('frequencyKhz')?.setValue(7125);
    component.determineBand();

    expect(component.qsoForm.get('band')?.value).toBe('40m');
  });

  // ==================== MODE VALIDATION TESTS ====================

  it('should require mode', () => {
    const mode = component.qsoForm.get('mode');
    expect(mode?.hasError('required')).toBeTruthy();
  });

  it('should accept valid modes', () => {
    const mode = component.qsoForm.get('mode');

    const validModes = ['SSB', 'CW', 'FT8', 'FT4', 'RTTY', 'PSK31'];

    validModes.forEach(validMode => {
      mode?.setValue(validMode);
      expect(mode?.valid).toBeTruthy();
    });
  });

  // ==================== RST VALIDATION TESTS ====================

  it('should require RST sent and received', () => {
    const rstSent = component.qsoForm.get('rstSent');
    const rstRcvd = component.qsoForm.get('rstRcvd');

    expect(rstSent?.hasError('required')).toBeTruthy();
    expect(rstRcvd?.hasError('required')).toBeTruthy();
  });

  it('should validate RST format for SSB', () => {
    component.qsoForm.get('mode')?.setValue('SSB');
    const rstSent = component.qsoForm.get('rstSent');

    rstSent?.setValue('59');
    expect(rstSent?.valid).toBeTruthy();

    rstSent?.setValue('599');
    expect(rstSent?.hasError('pattern')).toBeTruthy();
  });

  it('should validate RST format for CW', () => {
    component.qsoForm.get('mode')?.setValue('CW');
    const rstSent = component.qsoForm.get('rstSent');

    rstSent?.setValue('599');
    expect(rstSent?.valid).toBeTruthy();

    rstSent?.setValue('59');
    expect(rstSent?.hasError('pattern')).toBeTruthy();
  });

  // ==================== DATE/TIME VALIDATION TESTS ====================

  it('should not allow future dates', () => {
    const qsoDate = component.qsoForm.get('qsoDate');
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    qsoDate?.setValue(tomorrow.toISOString().split('T')[0]);
    expect(qsoDate?.hasError('futureDate')).toBeTruthy();
  });

  it('should accept today\'s date', () => {
    const qsoDate = component.qsoForm.get('qsoDate');
    const today = new Date().toISOString().split('T')[0];

    qsoDate?.setValue(today);
    expect(qsoDate?.valid).toBeTruthy();
  });

  // ==================== QSO CREATION TESTS ====================

  it('should create QSO on valid form submission', () => {
    apiService.createQSO.and.returnValue(of({ id: 1, callsign: 'W1AW' }));

    component.qsoForm.patchValue({
      callsign: 'W1AW',
      frequencyKhz: 14250,
      mode: 'SSB',
      rstSent: '59',
      rstRcvd: '59',
      qsoDate: '2025-01-15',
      timeOn: '14:30'
    });

    component.onSubmit();

    expect(apiService.createQSO).toHaveBeenCalled();
    expect(component.successMessage).toBe('QSO logged successfully!');
  });

  it('should display error on failed QSO creation', () => {
    apiService.createQSO.and.returnValue(throwError(() => ({ error: { message: 'Duplicate QSO' } })));

    component.qsoForm.patchValue({
      callsign: 'W1AW',
      frequencyKhz: 14250,
      mode: 'SSB',
      rstSent: '59',
      rstRcvd: '59',
      qsoDate: '2025-01-15',
      timeOn: '14:30'
    });

    component.onSubmit();

    expect(component.errorMessage).toBe('Duplicate QSO');
  });

  it('should not submit if form is invalid', () => {
    component.qsoForm.patchValue({
      callsign: '',
      frequencyKhz: null,
      mode: '',
      rstSent: '',
      rstRcvd: ''
    });

    component.onSubmit();

    expect(apiService.createQSO).not.toHaveBeenCalled();
  });

  // ==================== RIG INTEGRATION TESTS ====================

  it('should populate frequency from rig', () => {
    const rigData = {
      frequencyHz: 14250000,
      mode: 'USB',
      power: 100
    };

    component.onRigDataReceived(rigData);

    expect(component.qsoForm.get('frequencyKhz')?.value).toBe(14250);
    expect(component.qsoForm.get('mode')?.value).toBe('USB');
  });

  it('should convert rig mode to standard mode', () => {
    const rigData = {
      frequencyHz: 14074000,
      mode: 'DIGI',
      power: 50
    };

    component.onRigDataReceived(rigData);

    expect(component.qsoForm.get('mode')?.value).toBe('FT8');
  });

  // ==================== CONTEST MODE TESTS ====================

  it('should show contest fields when contest is selected', () => {
    component.qsoForm.get('contestCode')?.setValue('ARRL-FD');

    expect(component.isContestMode()).toBeTruthy();
  });

  it('should validate Field Day exchange', () => {
    component.qsoForm.patchValue({
      contestCode: 'ARRL-FD',
      contestData: '{"class":"2A","section":"ORG"}'
    });

    expect(component.isValidContestData()).toBeTruthy();
  });

  // ==================== DUPLICATE CHECK TESTS ====================

  it('should warn on potential duplicate QSO', () => {
    const existingQSO = {
      callsign: 'W1AW',
      qsoDate: '2025-01-15',
      timeOn: '14:30'
    };

    component.checkDuplicate(existingQSO);

    expect(component.duplicateWarning).toBeTruthy();
  });

  // ==================== FORM RESET TESTS ====================

  it('should reset form after successful submission', () => {
    apiService.createQSO.and.returnValue(of({ id: 1, callsign: 'W1AW' }));

    component.qsoForm.patchValue({
      callsign: 'W1AW',
      frequencyKhz: 14250,
      mode: 'SSB',
      rstSent: '59',
      rstRcvd: '59',
      qsoDate: '2025-01-15',
      timeOn: '14:30'
    });

    component.onSubmit();

    expect(component.qsoForm.get('callsign')?.value).toBe('');
    expect(component.qsoForm.get('frequencyKhz')?.value).toBeNull();
  });

  it('should preserve frequency and mode after reset if option enabled', () => {
    component.preserveFrequencyMode = true;
    apiService.createQSO.and.returnValue(of({ id: 1, callsign: 'W1AW' }));

    component.qsoForm.patchValue({
      callsign: 'W1AW',
      frequencyKhz: 14250,
      mode: 'SSB',
      rstSent: '59',
      rstRcvd: '59'
    });

    component.onSubmit();

    expect(component.qsoForm.get('frequencyKhz')?.value).toBe(14250);
    expect(component.qsoForm.get('mode')?.value).toBe('SSB');
  });

  // ==================== QUICK LOG FEATURES ====================

  it('should update time to current UTC on time refresh', () => {
    component.refreshTime();

    const currentTime = new Date().toISOString().substring(11, 16);
    expect(component.qsoForm.get('timeOn')?.value).toEqual(currentTime);
  });

  it('should fill default RST based on mode', () => {
    component.qsoForm.get('mode')?.setValue('SSB');
    component.fillDefaultRST();

    expect(component.qsoForm.get('rstSent')?.value).toBe('59');
    expect(component.qsoForm.get('rstRcvd')?.value).toBe('59');

    component.qsoForm.get('mode')?.setValue('CW');
    component.fillDefaultRST();

    expect(component.qsoForm.get('rstSent')?.value).toBe('599');
    expect(component.qsoForm.get('rstRcvd')?.value).toBe('599');
  });

  // ==================== KEYBOARD SHORTCUTS TESTS ====================

  it('should submit form on Ctrl+Enter', () => {
    apiService.createQSO.and.returnValue(of({ id: 1, callsign: 'W1AW' }));

    component.qsoForm.patchValue({
      callsign: 'W1AW',
      frequencyKhz: 14250,
      mode: 'SSB',
      rstSent: '59',
      rstRcvd: '59',
      qsoDate: '2025-01-15',
      timeOn: '14:30'
    });

    const event = new KeyboardEvent('keydown', { key: 'Enter', ctrlKey: true });
    component.onKeyDown(event);

    expect(apiService.createQSO).toHaveBeenCalled();
  });

  // ==================== ACCESSIBILITY TESTS ====================

  it('should have proper ARIA labels', () => {
    const compiled = fixture.nativeElement;
    const callsignInput = compiled.querySelector('input[name="callsign"]');
    const frequencyInput = compiled.querySelector('input[name="frequencyKhz"]');

    expect(callsignInput.getAttribute('aria-label')).toBeTruthy();
    expect(frequencyInput.getAttribute('aria-label')).toBeTruthy();
  });
});
