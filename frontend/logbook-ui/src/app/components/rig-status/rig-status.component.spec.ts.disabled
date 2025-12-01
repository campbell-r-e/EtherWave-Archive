import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError, interval } from 'rxjs';
import { RigStatusComponent } from './rig-status.component';
import { ApiService } from '../../services/api.service';
import { WebSocketService } from '../../services/websocket.service';

describe('RigStatusComponent', () => {
  let component: RigStatusComponent;
  let fixture: ComponentFixture<RigStatusComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let wsService: jasmine.SpyObj<WebSocketService>;

  const mockRigStatus = {
    connected: true,
    frequencyHz: 14250000,
    mode: 'USB',
    vfo: 'VFOA',
    power: 100,
    swr: 1.5,
    rigModel: 'Icom IC-7300',
    pttOn: false,
    splitOn: false,
    ritOn: false,
    ritOffset: 0
  };

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'connectToRig',
      'disconnectFromRig',
      'getRigStatus',
      'setRigFrequency',
      'setRigMode',
      'setRigPower',
      'setPTT',
      'setVFO',
      'setSplit'
    ]);
    const wsServiceSpy = jasmine.createSpyObj('WebSocketService', ['subscribe', 'disconnect']);

    await TestBed.configureTestingModule({
      imports: [RigStatusComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: WebSocketService, useValue: wsServiceSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    wsService = TestBed.inject(WebSocketService) as jasmine.SpyObj<WebSocketService>;

    apiService.getRigStatus.and.returnValue(of(mockRigStatus));
    wsService.subscribe.and.returnValue(of(mockRigStatus));

    fixture = TestBed.createComponent(RigStatusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== CONNECTION TESTS ====================

  it('should connect to rig', () => {
    apiService.connectToRig.and.returnValue(of({ connected: true }));

    component.rigHost = 'localhost';
    component.rigPort = 4532;
    component.connect();

    expect(apiService.connectToRig).toHaveBeenCalledWith('localhost', 4532);
  });

  it('should show connection success message', () => {
    apiService.connectToRig.and.returnValue(of({ connected: true }));

    component.connect();

    expect(component.connectionStatus).toBe('Connected');
    expect(component.isConnected).toBeTruthy();
  });

  it('should handle connection failure', () => {
    apiService.connectToRig.and.returnValue(throwError(() => new Error('Connection failed')));

    component.connect();

    expect(component.connectionStatus).toContain('Failed');
    expect(component.isConnected).toBeFalsy();
  });

  it('should validate host before connecting', () => {
    component.rigHost = '';
    component.connect();

    expect(apiService.connectToRig).not.toHaveBeenCalled();
    expect(component.validationError).toBeTruthy();
  });

  it('should validate port range', () => {
    component.rigHost = 'localhost';
    component.rigPort = 99999;
    component.connect();

    expect(component.validationError).toContain('port');
  });

  it('should disconnect from rig', () => {
    component.isConnected = true;
    apiService.disconnectFromRig.and.returnValue(of({ success: true }));

    component.disconnect();

    expect(apiService.disconnectFromRig).toHaveBeenCalled();
    expect(component.isConnected).toBeFalsy();
  });

  it('should stop polling on disconnect', () => {
    component.isConnected = true;
    component.isPolling = true;
    apiService.disconnectFromRig.and.returnValue(of({ success: true }));

    component.disconnect();

    expect(component.isPolling).toBeFalsy();
  });

  // ==================== STATUS POLLING TESTS ====================

  it('should start polling rig status', () => {
    component.isConnected = true;
    component.startPolling();

    expect(component.isPolling).toBeTruthy();
  });

  it('should poll at specified interval', () => {
    jasmine.clock().install();
    component.isConnected = true;
    component.pollingInterval = 1000;

    component.startPolling();
    jasmine.clock().tick(1000);

    expect(apiService.getRigStatus).toHaveBeenCalled();

    jasmine.clock().uninstall();
  });

  it('should stop polling', () => {
    component.isPolling = true;
    component.stopPolling();

    expect(component.isPolling).toBeFalsy();
  });

  it('should update status from polling', () => {
    apiService.getRigStatus.and.returnValue(of(mockRigStatus));

    component.updateStatus();

    expect(component.rigStatus).toEqual(mockRigStatus);
  });

  // ==================== WEBSOCKET TESTS ====================

  it('should subscribe to WebSocket updates', () => {
    component.subscribeToUpdates();

    expect(wsService.subscribe).toHaveBeenCalledWith('/topic/rig-status');
  });

  it('should update status from WebSocket', () => {
    const update = { ...mockRigStatus, frequencyHz: 7125000 };
    wsService.subscribe.and.returnValue(of(update));

    component.subscribeToUpdates();

    expect(component.rigStatus.frequencyHz).toBe(7125000);
  });

  it('should disconnect WebSocket on component destroy', () => {
    component.ngOnDestroy();

    expect(wsService.disconnect).toHaveBeenCalled();
  });

  // ==================== FREQUENCY DISPLAY TESTS ====================

  it('should display frequency in MHz', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('14.250');
  });

  it('should format frequency with proper decimals', () => {
    expect(component.formatFrequency(14250000)).toBe('14.250 MHz');
    expect(component.formatFrequency(7125000)).toBe('7.125 MHz');
  });

  it('should determine band from frequency', () => {
    expect(component.getBand(14250000)).toBe('20m');
    expect(component.getBand(7125000)).toBe('40m');
    expect(component.getBand(21200000)).toBe('15m');
  });

  it('should display band badge', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.band-badge')).toBeTruthy();
  });

  // ==================== MODE DISPLAY TESTS ====================

  it('should display current mode', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('USB');
  });

  it('should convert Hamlib modes to standard modes', () => {
    expect(component.convertMode('USB')).toBe('SSB');
    expect(component.convertMode('LSB')).toBe('SSB');
    expect(component.convertMode('PKTUSB')).toBe('FT8');
  });

  it('should display mode badge with correct styling', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const modeBadge = compiled.querySelector('.mode-badge');
    expect(modeBadge).toBeTruthy();
  });

  // ==================== POWER DISPLAY TESTS ====================

  it('should display power level', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('100 W');
  });

  it('should show power level meter', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.power-meter')).toBeTruthy();
  });

  it('should calculate power percentage', () => {
    expect(component.getPowerPercentage(50)).toBe(50);
    expect(component.getPowerPercentage(100)).toBe(100);
  });

  // ==================== SWR DISPLAY TESTS ====================

  it('should display SWR value', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('1.5');
  });

  it('should show SWR warning for high values', () => {
    component.rigStatus = { ...mockRigStatus, swr: 3.0 };
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.swr-warning')).toBeTruthy();
  });

  it('should determine SWR status', () => {
    expect(component.getSWRStatus(1.5)).toBe('good');
    expect(component.getSWRStatus(2.0)).toBe('fair');
    expect(component.getSWRStatus(3.0)).toBe('high');
  });

  // ==================== VFO DISPLAY TESTS ====================

  it('should display active VFO', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('VFOA');
  });

  it('should highlight active VFO', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.vfo.active')).toBeTruthy();
  });

  // ==================== PTT STATUS TESTS ====================

  it('should display PTT status', () => {
    component.rigStatus = { ...mockRigStatus, pttOn: true };
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.ptt-indicator.active')).toBeTruthy();
  });

  it('should show transmit warning when PTT is on', () => {
    component.rigStatus = { ...mockRigStatus, pttOn: true };
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Transmitting');
  });

  it('should toggle PTT', () => {
    apiService.setPTT.and.returnValue(of({ success: true }));

    component.togglePTT();

    expect(apiService.setPTT).toHaveBeenCalled();
  });

  // ==================== SPLIT OPERATION TESTS ====================

  it('should display split status', () => {
    component.rigStatus = { ...mockRigStatus, splitOn: true };
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.split-indicator')).toBeTruthy();
  });

  it('should toggle split operation', () => {
    apiService.setSplit.and.returnValue(of({ success: true }));

    component.toggleSplit();

    expect(apiService.setSplit).toHaveBeenCalled();
  });

  // ==================== RIT TESTS ====================

  it('should display RIT offset', () => {
    component.rigStatus = { ...mockRigStatus, ritOn: true, ritOffset: 100 };
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('+100 Hz');
  });

  it('should show RIT indicator when active', () => {
    component.rigStatus = { ...mockRigStatus, ritOn: true };
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.rit-indicator.active')).toBeTruthy();
  });

  it('should format RIT offset with sign', () => {
    expect(component.formatRITOffset(100)).toBe('+100 Hz');
    expect(component.formatRITOffset(-50)).toBe('-50 Hz');
  });

  // ==================== RIG MODEL DISPLAY TESTS ====================

  it('should display rig model', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Icom IC-7300');
  });

  it('should show rig icon for known models', () => {
    component.rigStatus = mockRigStatus;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.rig-icon')).toBeTruthy();
  });

  // ==================== CONTROL TESTS ====================

  it('should set frequency', () => {
    apiService.setRigFrequency.and.returnValue(of({ success: true }));

    component.setFrequency(14250000);

    expect(apiService.setRigFrequency).toHaveBeenCalledWith(14250000);
  });

  it('should set mode', () => {
    apiService.setRigMode.and.returnValue(of({ success: true }));

    component.setMode('CW');

    expect(apiService.setRigMode).toHaveBeenCalledWith('CW');
  });

  it('should set power', () => {
    apiService.setRigPower.and.returnValue(of({ success: true }));

    component.setPower(75);

    expect(apiService.setRigPower).toHaveBeenCalledWith(75);
  });

  it('should validate frequency range before setting', () => {
    component.setFrequency(100); // Too low

    expect(apiService.setRigFrequency).not.toHaveBeenCalled();
    expect(component.validationError).toBeTruthy();
  });

  it('should validate power range before setting', () => {
    component.setPower(200); // Too high

    expect(apiService.setRigPower).not.toHaveBeenCalled();
  });

  // ==================== QUICK BAND CHANGE TESTS ====================

  it('should provide quick band buttons', () => {
    const bands = component.getQuickBands();

    expect(bands).toContain('20m');
    expect(bands).toContain('40m');
    expect(bands).toContain('15m');
  });

  it('should change to band frequency', () => {
    apiService.setRigFrequency.and.returnValue(of({ success: true }));

    component.changeToBand('20m');

    expect(apiService.setRigFrequency).toHaveBeenCalledWith(jasmine.any(Number));
  });

  it('should get default frequency for band', () => {
    expect(component.getDefaultFrequency('20m')).toBe(14200000);
    expect(component.getDefaultFrequency('40m')).toBe(7100000);
  });

  // ==================== CONNECTION STATUS TESTS ====================

  it('should show connected indicator', () => {
    component.isConnected = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.connected-indicator')).toBeTruthy();
  });

  it('should show disconnected state', () => {
    component.isConnected = false;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.disconnected-state')).toBeTruthy();
  });

  it('should disable controls when disconnected', () => {
    component.isConnected = false;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const buttons = compiled.querySelectorAll('button:not(.connect-button)');
    buttons.forEach((btn: Element) => {
      expect(btn.getAttribute('disabled')).toBeTruthy();
    });
  });

  // ==================== ERROR HANDLING TESTS ====================

  it('should handle polling errors gracefully', () => {
    apiService.getRigStatus.and.returnValue(throwError(() => new Error('Polling failed')));

    component.updateStatus();

    expect(component.errorMessage).toBeTruthy();
  });

  it('should retry connection after failure', () => {
    apiService.connectToRig.and.returnValue(throwError(() => new Error('Failed')));

    component.connect();
    component.retryConnection();

    expect(apiService.connectToRig).toHaveBeenCalledTimes(2);
  });

  it('should show connection timeout warning', () => {
    jasmine.clock().install();

    component.connect();
    jasmine.clock().tick(10000);

    expect(component.connectionWarning).toBeTruthy();

    jasmine.clock().uninstall();
  });

  // ==================== UI STATE TESTS ====================

  it('should show loading indicator while connecting', () => {
    component.connecting = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.loading-spinner')).toBeTruthy();
  });

  it('should show settings panel', () => {
    component.showSettings = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.settings-panel')).toBeTruthy();
  });

  it('should toggle settings panel', () => {
    component.showSettings = false;
    component.toggleSettings();

    expect(component.showSettings).toBeTruthy();
  });

  // ==================== ACCESSIBILITY TESTS ====================

  it('should have accessible controls', () => {
    component.isConnected = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const pttButton = compiled.querySelector('.ptt-button');
    expect(pttButton?.getAttribute('aria-label')).toBeTruthy();
  });

  it('should announce frequency changes', () => {
    spyOn(component, 'announceToScreenReader');
    apiService.setRigFrequency.and.returnValue(of({ success: true }));

    component.setFrequency(14250000);

    expect(component.announceToScreenReader).toHaveBeenCalled();
  });

  // ==================== PERSISTENCE TESTS ====================

  it('should save connection settings', () => {
    component.rigHost = 'localhost';
    component.rigPort = 4532;

    component.saveSettings();

    expect(localStorage.getItem('rigHost')).toBe('localhost');
    expect(localStorage.getItem('rigPort')).toBe('4532');
  });

  it('should load saved connection settings', () => {
    localStorage.setItem('rigHost', 'localhost');
    localStorage.setItem('rigPort', '4532');

    component.loadSettings();

    expect(component.rigHost).toBe('localhost');
    expect(component.rigPort).toBe(4532);
  });

  // ==================== REFRESH TESTS ====================

  it('should manually refresh status', () => {
    spyOn(component, 'updateStatus');

    component.refresh();

    expect(component.updateStatus).toHaveBeenCalled();
  });

  it('should clear errors on refresh', () => {
    component.errorMessage = 'Error';
    component.refresh();

    expect(component.errorMessage).toBe('');
  });
});
