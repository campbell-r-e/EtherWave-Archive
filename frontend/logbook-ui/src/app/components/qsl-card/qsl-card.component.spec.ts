import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { QslCardComponent } from './qsl-card.component';
import { QSO } from '../../models/qso.model';

const makeQso = (override: Partial<QSO> = {}): QSO => ({
  stationId: 1,
  callsign: 'VK2ABC',
  frequencyKhz: 14250,
  mode: 'SSB',
  qsoDate: '2024-01-15',
  timeOn: '14:30',
  rstSent: '59',
  rstRcvd: '57',
  ...override,
});

describe('QslCardComponent', () => {
  let component: QslCardComponent;
  let fixture: ComponentFixture<QslCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QslCardComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(QslCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  describe('formatFreq()', () => {
    it('returns em dash for undefined', () => {
      expect(component.formatFreq(undefined)).toBe('—');
    });

    it('returns em dash for 0', () => {
      expect(component.formatFreq(0)).toBe('—');
    });

    it('formats 14250 kHz as "14.250 MHz"', () => {
      expect(component.formatFreq(14250)).toBe('14.250 MHz');
    });

    it('formats 7100 kHz as "7.100 MHz"', () => {
      expect(component.formatFreq(7100)).toBe('7.100 MHz');
    });

    it('formats 144200 kHz as "144.200 MHz"', () => {
      expect(component.formatFreq(144200)).toBe('144.200 MHz');
    });
  });

  describe('modeDisplay getter', () => {
    it('returns em dash when qso is null', () => {
      component.qso = null;
      expect(component.modeDisplay).toBe('—');
    });

    it('returns mode from qso', () => {
      component.qso = makeQso({ mode: 'FT8' });
      expect(component.modeDisplay).toBe('FT8');
    });
  });

  describe('rstDisplay getter', () => {
    it('shows "59 / 59" defaults when qso is null', () => {
      component.qso = null;
      expect(component.rstDisplay).toBe('59 / 59');
    });

    it('shows actual RST values from qso', () => {
      component.qso = makeQso({ rstSent: '59', rstRcvd: '57' });
      expect(component.rstDisplay).toBe('59 / 57');
    });

    it('uses default 59 when rstSent is missing', () => {
      component.qso = makeQso({ rstSent: undefined, rstRcvd: '55' });
      expect(component.rstDisplay).toBe('59 / 55');
    });
  });

  describe('close()', () => {
    it('emits the closed event', () => {
      const closedSpy = jest.fn();
      component.closed.subscribe(closedSpy);
      component.close();
      expect(closedSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('print()', () => {
    it('calls window.print()', () => {
      const printSpy = jest.spyOn(window, 'print').mockImplementation(() => {});
      component.print();
      expect(printSpy).toHaveBeenCalledTimes(1);
      printSpy.mockRestore();
    });
  });

  describe('formatDate()', () => {
    it('returns em dash for undefined', () => {
      expect(component.formatDate(undefined)).toBe('—');
    });

    it('returns the date string unchanged', () => {
      expect(component.formatDate('2024-01-15')).toBe('2024-01-15');
    });
  });
});
