import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { SettingsComponent } from './settings.component';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

describe('SettingsComponent', () => {
  let component: SettingsComponent;
  let fixture: ComponentFixture<SettingsComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let authService: jasmine.SpyObj<AuthService>;

  const mockSettings = {
    general: {
      timezone: 'America/New_York',
      dateFormat: 'YYYY-MM-DD',
      timeFormat: '24h',
      language: 'en'
    },
    qsoDefaults: {
      defaultMode: 'SSB',
      defaultPower: 100,
      defaultRSTSent: '59',
      defaultRSTRcvd: '59'
    },
    notifications: {
      emailNotifications: true,
      duplicateQSOWarning: true,
      newInvitationAlert: true,
      soundEnabled: false
    },
    display: {
      theme: 'light',
      compactMode: false,
      showGridLines: true,
      fontSize: 'medium'
    },
    integrations: {
      qrzApiKey: '',
      hamQTHApiKey: '',
      clublogApiKey: '',
      lotWEnabled: false
    }
  };

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'getSettings',
      'updateSettings',
      'resetSettings',
      'validateApiKey',
      'testIntegration'
    ]);
    const authServiceSpy = jasmine.createSpyObj('AuthService', [
      'getCurrentUser',
      'changePassword'
    ]);

    await TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;

    authService.getCurrentUser.and.returnValue({ id: 1, username: 'testuser' });
    apiService.getSettings.and.returnValue(of(mockSettings));

    fixture = TestBed.createComponent(SettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== SETTINGS LOADING TESTS ====================

  it('should load settings on init', () => {
    expect(apiService.getSettings).toHaveBeenCalled();
    expect(component.settings).toEqual(mockSettings);
  });

  it('should handle API error gracefully', () => {
    apiService.getSettings.and.returnValue(throwError(() => new Error('API error')));

    component.loadSettings();

    expect(component.errorMessage).toBeTruthy();
  });

  // ==================== GENERAL SETTINGS TESTS ====================

  it('should display timezone selector', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('select[name="timezone"]')).toBeTruthy();
  });

  it('should provide list of timezones', () => {
    const timezones = component.getTimezones();

    expect(timezones).toContain('America/New_York');
    expect(timezones).toContain('America/Chicago');
    expect(timezones).toContain('UTC');
  });

  it('should change timezone', () => {
    component.settings.general.timezone = 'UTC';
    apiService.updateSettings.and.returnValue(of(component.settings));

    component.saveSettings();

    expect(apiService.updateSettings).toHaveBeenCalledWith(jasmine.objectContaining({
      general: jasmine.objectContaining({ timezone: 'UTC' })
    }));
  });

  it('should display date format options', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('select[name="dateFormat"]')).toBeTruthy();
  });

  it('should provide date format options', () => {
    const formats = component.getDateFormats();

    expect(formats).toContain('YYYY-MM-DD');
    expect(formats).toContain('MM/DD/YYYY');
    expect(formats).toContain('DD/MM/YYYY');
  });

  it('should toggle time format', () => {
    component.settings.general.timeFormat = '12h';
    component.toggleTimeFormat();

    expect(component.settings.general.timeFormat).toBe('24h');
  });

  it('should change language', () => {
    component.settings.general.language = 'es';
    apiService.updateSettings.and.returnValue(of(component.settings));

    component.saveSettings();

    expect(apiService.updateSettings).toHaveBeenCalled();
  });

  it('should provide available languages', () => {
    const languages = component.getLanguages();

    expect(languages).toContain('en');
    expect(languages).toContain('es');
    expect(languages).toContain('de');
  });

  // ==================== QSO DEFAULT TESTS ====================

  it('should display QSO default settings', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('select[name="defaultMode"]')).toBeTruthy();
  });

  it('should set default mode', () => {
    component.settings.qsoDefaults.defaultMode = 'CW';

    expect(component.settings.qsoDefaults.defaultMode).toBe('CW');
  });

  it('should set default power', () => {
    component.settings.qsoDefaults.defaultPower = 50;

    expect(component.settings.qsoDefaults.defaultPower).toBe(50);
  });

  it('should validate power range', () => {
    component.settings.qsoDefaults.defaultPower = 2000; // Too high

    component.saveSettings();

    expect(component.validationError).toContain('power');
  });

  it('should set default RST values', () => {
    component.settings.qsoDefaults.defaultRSTSent = '599';
    component.settings.qsoDefaults.defaultRSTRcvd = '599';

    expect(component.settings.qsoDefaults.defaultRSTSent).toBe('599');
    expect(component.settings.qsoDefaults.defaultRSTRcvd).toBe('599');
  });

  // ==================== NOTIFICATION TESTS ====================

  it('should display notification settings', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('input[name="emailNotifications"]')).toBeTruthy();
  });

  it('should toggle email notifications', () => {
    component.settings.notifications.emailNotifications = false;
    component.toggleEmailNotifications();

    expect(component.settings.notifications.emailNotifications).toBeTruthy();
  });

  it('should toggle duplicate QSO warning', () => {
    component.settings.notifications.duplicateQSOWarning = false;
    component.toggleDuplicateWarning();

    expect(component.settings.notifications.duplicateQSOWarning).toBeTruthy();
  });

  it('should toggle new invitation alerts', () => {
    component.settings.notifications.newInvitationAlert = false;
    component.toggleInvitationAlert();

    expect(component.settings.notifications.newInvitationAlert).toBeTruthy();
  });

  it('should toggle sound notifications', () => {
    component.settings.notifications.soundEnabled = false;
    component.toggleSound();

    expect(component.settings.notifications.soundEnabled).toBeTruthy();
  });

  it('should test sound notification', () => {
    spyOn(component, 'playNotificationSound');

    component.testSound();

    expect(component.playNotificationSound).toHaveBeenCalled();
  });

  // ==================== DISPLAY TESTS ====================

  it('should display theme selector', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('select[name="theme"]')).toBeTruthy();
  });

  it('should change theme', () => {
    component.settings.display.theme = 'dark';
    component.applyTheme();

    expect(document.body.classList.contains('dark-theme')).toBeTruthy();
  });

  it('should provide theme options', () => {
    const themes = component.getThemes();

    expect(themes).toContain('light');
    expect(themes).toContain('dark');
    expect(themes).toContain('auto');
  });

  it('should toggle compact mode', () => {
    component.settings.display.compactMode = false;
    component.toggleCompactMode();

    expect(component.settings.display.compactMode).toBeTruthy();
  });

  it('should toggle grid lines', () => {
    component.settings.display.showGridLines = false;
    component.toggleGridLines();

    expect(component.settings.display.showGridLines).toBeTruthy();
  });

  it('should change font size', () => {
    component.settings.display.fontSize = 'large';

    expect(component.settings.display.fontSize).toBe('large');
  });

  it('should provide font size options', () => {
    const sizes = component.getFontSizes();

    expect(sizes).toContain('small');
    expect(sizes).toContain('medium');
    expect(sizes).toContain('large');
  });

  // ==================== INTEGRATION TESTS ====================

  it('should display API key inputs', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('input[name="qrzApiKey"]')).toBeTruthy();
  });

  it('should mask API key values', () => {
    component.settings.integrations.qrzApiKey = 'secret-key';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const input = compiled.querySelector('input[name="qrzApiKey"]');
    expect(input?.getAttribute('type')).toBe('password');
  });

  it('should toggle API key visibility', () => {
    component.showQrzKey = false;
    component.toggleKeyVisibility('qrz');

    expect(component.showQrzKey).toBeTruthy();
  });

  it('should validate API key', () => {
    apiService.validateApiKey.and.returnValue(of({ valid: true }));

    component.validateApiKey('qrz', 'test-key');

    expect(apiService.validateApiKey).toHaveBeenCalledWith('qrz', 'test-key');
  });

  it('should show validation success', () => {
    apiService.validateApiKey.and.returnValue(of({ valid: true }));

    component.validateApiKey('qrz', 'test-key');

    expect(component.apiKeyStatus['qrz']).toBe('valid');
  });

  it('should show validation failure', () => {
    apiService.validateApiKey.and.returnValue(of({ valid: false }));

    component.validateApiKey('qrz', 'test-key');

    expect(component.apiKeyStatus['qrz']).toBe('invalid');
  });

  it('should test integration', () => {
    apiService.testIntegration.and.returnValue(of({ success: true }));

    component.testIntegration('qrz');

    expect(apiService.testIntegration).toHaveBeenCalledWith('qrz');
  });

  it('should toggle LoTW integration', () => {
    component.settings.integrations.lotWEnabled = false;
    component.toggleLoTW();

    expect(component.settings.integrations.lotWEnabled).toBeTruthy();
  });

  // ==================== PASSWORD CHANGE TESTS ====================

  it('should show password change section', () => {
    component.showPasswordSection = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.password-section')).toBeTruthy();
  });

  it('should change password', () => {
    authService.changePassword.and.returnValue(of({ success: true }));

    component.currentPassword = 'oldpass';
    component.newPassword = 'newpass';
    component.confirmPassword = 'newpass';
    component.changePassword();

    expect(authService.changePassword).toHaveBeenCalledWith('oldpass', 'newpass');
  });

  it('should validate password match', () => {
    component.newPassword = 'newpass';
    component.confirmPassword = 'different';

    component.changePassword();

    expect(component.validationError).toContain('match');
  });

  it('should validate password strength', () => {
    component.newPassword = '123'; // Too weak

    const isStrong = component.isStrongPassword(component.newPassword);

    expect(isStrong).toBeFalsy();
  });

  it('should clear password fields after success', () => {
    authService.changePassword.and.returnValue(of({ success: true }));

    component.currentPassword = 'oldpass';
    component.newPassword = 'newpass';
    component.confirmPassword = 'newpass';
    component.changePassword();

    expect(component.currentPassword).toBe('');
    expect(component.newPassword).toBe('');
    expect(component.confirmPassword).toBe('');
  });

  // ==================== SAVE SETTINGS TESTS ====================

  it('should save settings', () => {
    apiService.updateSettings.and.returnValue(of(mockSettings));

    component.saveSettings();

    expect(apiService.updateSettings).toHaveBeenCalledWith(component.settings);
  });

  it('should show success message after save', () => {
    apiService.updateSettings.and.returnValue(of(mockSettings));

    component.saveSettings();

    expect(component.successMessage).toContain('saved');
  });

  it('should handle save error', () => {
    apiService.updateSettings.and.returnValue(throwError(() => new Error('Save failed')));

    component.saveSettings();

    expect(component.errorMessage).toBeTruthy();
  });

  it('should mark settings as unsaved on change', () => {
    component.hasUnsavedChanges = false;

    component.onSettingChange();

    expect(component.hasUnsavedChanges).toBeTruthy();
  });

  it('should warn before leaving with unsaved changes', () => {
    component.hasUnsavedChanges = true;
    spyOn(window, 'confirm').and.returnValue(false);

    const canLeave = component.canDeactivate();

    expect(canLeave).toBeFalsy();
  });

  // ==================== RESET SETTINGS TESTS ====================

  it('should reset settings to defaults', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    apiService.resetSettings.and.returnValue(of(mockSettings));

    component.resetToDefaults();

    expect(apiService.resetSettings).toHaveBeenCalled();
  });

  it('should confirm before resetting', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.resetToDefaults();

    expect(apiService.resetSettings).not.toHaveBeenCalled();
  });

  it('should reload settings after reset', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(component, 'loadSettings');
    apiService.resetSettings.and.returnValue(of(mockSettings));

    component.resetToDefaults();

    expect(component.loadSettings).toHaveBeenCalled();
  });

  // ==================== TABS TESTS ====================

  it('should switch to general tab', () => {
    component.activeTab = 'display';
    component.switchTab('general');

    expect(component.activeTab).toBe('general');
  });

  it('should display active tab', () => {
    component.activeTab = 'general';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.tab.active')?.textContent).toContain('General');
  });

  it('should show tab content', () => {
    component.activeTab = 'general';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.tab-content.general')).toBeTruthy();
  });

  it('should provide all available tabs', () => {
    const tabs = component.getTabs();

    expect(tabs).toContain('general');
    expect(tabs).toContain('qso');
    expect(tabs).toContain('notifications');
    expect(tabs).toContain('display');
    expect(tabs).toContain('integrations');
  });

  // ==================== IMPORT/EXPORT SETTINGS TESTS ====================

  it('should export settings', () => {
    spyOn(component, 'downloadFile');

    component.exportSettings();

    expect(component.downloadFile).toHaveBeenCalled();
  });

  it('should generate settings JSON', () => {
    const json = component.generateSettingsJSON();

    expect(json).toContain('general');
    expect(json).toContain('qsoDefaults');
  });

  it('should import settings from file', () => {
    const file = new File([JSON.stringify(mockSettings)], 'settings.json', { type: 'application/json' });
    const event = { target: { files: [file] } };

    component.onFileSelected(event);

    expect(component.settings).toEqual(mockSettings);
  });

  it('should validate imported settings', () => {
    const invalidSettings = { invalid: true };
    const file = new File([JSON.stringify(invalidSettings)], 'settings.json', { type: 'application/json' });
    const event = { target: { files: [file] } };

    component.onFileSelected(event);

    expect(component.validationError).toBeTruthy();
  });

  // ==================== UI STATE TESTS ====================

  it('should show loading indicator', () => {
    component.loading = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.loading-spinner')).toBeTruthy();
  });

  it('should disable save button while saving', () => {
    component.saving = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const saveBtn = compiled.querySelector('.save-button');
    expect(saveBtn?.getAttribute('disabled')).toBeTruthy();
  });

  it('should highlight unsaved changes', () => {
    component.hasUnsavedChanges = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.unsaved-indicator')).toBeTruthy();
  });

  // ==================== VALIDATION TESTS ====================

  it('should validate email format for notifications', () => {
    component.settings.notifications.emailAddress = 'invalid';

    component.saveSettings();

    expect(component.validationError).toContain('email');
  });

  it('should validate numeric values', () => {
    component.settings.qsoDefaults.defaultPower = -10;

    component.saveSettings();

    expect(component.validationError).toBeTruthy();
  });

  it('should show validation errors', () => {
    component.validationError = 'Invalid setting';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.validation-error')?.textContent).toContain('Invalid setting');
  });

  // ==================== ACCESSIBILITY TESTS ====================

  it('should have accessible form inputs', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const timezoneSelect = compiled.querySelector('select[name="timezone"]');

    expect(timezoneSelect?.getAttribute('aria-label')).toBeTruthy();
  });

  it('should announce settings changes', () => {
    spyOn(component, 'announceToScreenReader');
    apiService.updateSettings.and.returnValue(of(mockSettings));

    component.saveSettings();

    expect(component.announceToScreenReader).toHaveBeenCalled();
  });

  it('should support keyboard navigation between tabs', () => {
    const event = new KeyboardEvent('keydown', { key: 'ArrowRight' });

    component.handleTabKeypress(event);

    expect(component.activeTab).toBeTruthy();
  });

  // ==================== PREVIEW TESTS ====================

  it('should preview theme changes', () => {
    component.previewTheme('dark');

    expect(document.body.classList.contains('dark-theme')).toBeTruthy();
  });

  it('should revert theme preview', () => {
    component.previewTheme('dark');
    component.revertThemePreview();

    expect(document.body.classList.contains('light-theme')).toBeTruthy();
  });

  it('should preview font size changes', () => {
    component.previewFontSize('large');

    expect(document.body.style.fontSize).toBeTruthy();
  });

  // ==================== REFRESH TESTS ====================

  it('should refresh settings', () => {
    spyOn(component, 'loadSettings');

    component.refresh();

    expect(component.loadSettings).toHaveBeenCalled();
  });

  it('should clear messages on refresh', () => {
    component.errorMessage = 'Error';
    component.successMessage = 'Success';

    component.refresh();

    expect(component.errorMessage).toBe('');
    expect(component.successMessage).toBe('');
  });

  // ==================== HELP TESTS ====================

  it('should show help tooltip', () => {
    component.showHelp('timezone');

    expect(component.helpVisible['timezone']).toBeTruthy();
  });

  it('should hide help tooltip', () => {
    component.showHelp('timezone');
    component.hideHelp('timezone');

    expect(component.helpVisible['timezone']).toBeFalsy();
  });
});
