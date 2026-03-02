import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { DXClusterPanelComponent } from './dx-cluster-panel.component';
import { DXClusterService, DXSpot } from '../../services/dx-cluster/dx-cluster.service';

const makeSpot = (override: Partial<DXSpot> = {}): DXSpot => ({
  spotter: 'W1AW',
  dxCallsign: 'VK2ABC',
  frequency: 14250,
  band: '20m',
  mode: 'SSB',
  comment: '',
  time: new Date().toISOString(),
  ...override,
});

describe('DXClusterPanelComponent', () => {
  let component: DXClusterPanelComponent;
  let fixture: ComponentFixture<DXClusterPanelComponent>;
  let spotsSubject: BehaviorSubject<DXSpot[]>;
  let mockService: {
    spots$: any;
    startPolling: jest.Mock;
    stopPolling: jest.Mock;
    fetchSpots: jest.Mock;
  };

  beforeEach(async () => {
    spotsSubject = new BehaviorSubject<DXSpot[]>([]);
    mockService = {
      spots$: spotsSubject.asObservable(),
      startPolling: jest.fn(),
      stopPolling: jest.fn(),
      fetchSpots: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DXClusterPanelComponent],
      providers: [{ provide: DXClusterService, useValue: mockService }],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(DXClusterPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('starts polling on init', () => {
    expect(mockService.startPolling).toHaveBeenCalled();
  });

  describe('formatFreq()', () => {
    it('returns em dash for 0', () => {
      expect(component.formatFreq(0)).toBe('—');
    });

    it('formats 14250 kHz as "14.3"', () => {
      expect(component.formatFreq(14250)).toBe('14.3');
    });

    it('formats 7100 kHz as "7.1"', () => {
      expect(component.formatFreq(7100)).toBe('7.1');
    });
  });

  describe('spotAge()', () => {
    it('returns empty string for empty time', () => {
      expect(component.spotAge('')).toBe('');
    });

    it('returns "now" for a very recent spot', () => {
      const recent = new Date(Date.now() - 30_000).toISOString(); // 30s ago
      expect(component.spotAge(recent)).toBe('now');
    });

    it('returns minutes for a spot a few minutes old', () => {
      const fiveMinAgo = new Date(Date.now() - 5 * 60_000).toISOString();
      expect(component.spotAge(fiveMinAgo)).toBe('5m');
    });

    it('returns hours for a spot a few hours old', () => {
      const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60_000).toISOString();
      expect(component.spotAge(twoHoursAgo)).toBe('2h');
    });

    it('returns days for an old spot', () => {
      const oneDayAgo = new Date(Date.now() - 25 * 60 * 60_000).toISOString();
      expect(component.spotAge(oneDayAgo)).toBe('1d');
    });
  });

  describe('bandClass()', () => {
    it('returns band-lf for 160m', () => {
      expect(component.bandClass('160m')).toBe('band-lf');
    });

    it('returns band-lf for 40m', () => {
      expect(component.bandClass('40m')).toBe('band-lf');
    });

    it('returns band-hf for 20m', () => {
      expect(component.bandClass('20m')).toBe('band-hf');
    });

    it('returns band-hf for 30m', () => {
      expect(component.bandClass('30m')).toBe('band-hf');
    });

    it('returns band-vhf for 10m', () => {
      expect(component.bandClass('10m')).toBe('band-vhf');
    });

    it('returns band-vhf for 6m', () => {
      expect(component.bandClass('6m')).toBe('band-vhf');
    });

    it('returns band-uhf for 2m', () => {
      expect(component.bandClass('2m')).toBe('band-uhf');
    });

    it('returns band-uhf for 70cm', () => {
      expect(component.bandClass('70cm')).toBe('band-uhf');
    });

    it('returns empty string for unknown band', () => {
      expect(component.bandClass('unknown')).toBe('');
    });

    it('is case-insensitive', () => {
      expect(component.bandClass('20M')).toBe('band-hf');
    });
  });

  describe('togglePanel()', () => {
    it('toggles isExpanded', () => {
      expect(component.isExpanded).toBe(true);
      component.togglePanel();
      expect(component.isExpanded).toBe(false);
      component.togglePanel();
      expect(component.isExpanded).toBe(true);
    });
  });

  describe('ngOnDestroy()', () => {
    it('calls stopPolling on destroy', () => {
      component.ngOnDestroy();
      expect(mockService.stopPolling).toHaveBeenCalled();
    });
  });

  describe('spots subscription', () => {
    it('updates spots when service emits', () => {
      const spots = [makeSpot(), makeSpot({ dxCallsign: 'JA1ABC' })];
      spotsSubject.next(spots);
      expect(component.spots.length).toBe(2);
    });

    it('updates lastUpdated when spots arrive', () => {
      spotsSubject.next([makeSpot()]);
      expect(component.lastUpdated).not.toBeNull();
    });
  });
});
