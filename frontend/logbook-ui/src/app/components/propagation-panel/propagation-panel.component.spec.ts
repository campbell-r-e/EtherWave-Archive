import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { PropagationPanelComponent } from './propagation-panel.component';
import { PropagationService, PropagationConditions, BandCondition } from '../../services/propagation/propagation.service';

const makeConditions = (): PropagationConditions => ({
  sfi: 150,
  kIndex: 2,
  aIndex: 8,
  fetchedAt: '2024-01-01T12:00:00Z',
  bands: {
    '20m': { band: '20m', displayName: '20 Meters', condition: 'GOOD', description: 'Good' },
    '40m': { band: '40m', displayName: '40 Meters', condition: 'EXCELLENT', description: 'Excellent' },
    '160m': { band: '160m', displayName: '160 Meters', condition: 'POOR', description: 'Poor' },
  },
});

describe('PropagationPanelComponent', () => {
  let component: PropagationPanelComponent;
  let fixture: ComponentFixture<PropagationPanelComponent>;
  let mockService: { getConditions: jest.Mock };

  beforeEach(async () => {
    mockService = { getConditions: jest.fn(() => of(makeConditions())) };

    await TestBed.configureTestingModule({
      imports: [PropagationPanelComponent],
      providers: [{ provide: PropagationService, useValue: mockService }],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(PropagationPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('loads conditions on init', () => {
    expect(mockService.getConditions).toHaveBeenCalled();
    expect(component.conditions).not.toBeNull();
  });

  describe('conditionClass()', () => {
    it('returns cond-excellent for EXCELLENT', () => {
      expect(component.conditionClass('EXCELLENT')).toBe('cond-excellent');
    });

    it('returns cond-good for GOOD', () => {
      expect(component.conditionClass('GOOD')).toBe('cond-good');
    });

    it('returns cond-fair for FAIR', () => {
      expect(component.conditionClass('FAIR')).toBe('cond-fair');
    });

    it('returns cond-poor for POOR', () => {
      expect(component.conditionClass('POOR')).toBe('cond-poor');
    });
  });

  describe('conditionLabel()', () => {
    it('returns "Excellent" for EXCELLENT', () => {
      expect(component.conditionLabel('EXCELLENT')).toBe('Excellent');
    });

    it('returns "Good" for GOOD', () => {
      expect(component.conditionLabel('GOOD')).toBe('Good');
    });

    it('returns "Fair" for FAIR', () => {
      expect(component.conditionLabel('FAIR')).toBe('Fair');
    });

    it('returns "Poor" for POOR', () => {
      expect(component.conditionLabel('POOR')).toBe('Poor');
    });
  });

  describe('kLabel()', () => {
    it('returns "Quiet" for K <= 1', () => {
      expect(component.kLabel(0)).toBe('Quiet');
      expect(component.kLabel(1)).toBe('Quiet');
    });

    it('returns "Unsettled" for K=2', () => {
      expect(component.kLabel(2)).toBe('Unsettled');
    });

    it('returns "Active" for K=3', () => {
      expect(component.kLabel(3)).toBe('Active');
    });

    it('returns "Minor Storm" for K=4', () => {
      expect(component.kLabel(4)).toBe('Minor Storm');
    });

    it('returns "Moderate Storm" for K=5', () => {
      expect(component.kLabel(5)).toBe('Moderate Storm');
    });

    it('returns "Major Storm" for K > 5', () => {
      expect(component.kLabel(6)).toBe('Major Storm');
      expect(component.kLabel(9)).toBe('Major Storm');
    });
  });

  describe('sfiLabel()', () => {
    it('returns "Very High" for SFI >= 200', () => {
      expect(component.sfiLabel(200)).toBe('Very High');
      expect(component.sfiLabel(250)).toBe('Very High');
    });

    it('returns "High" for SFI >= 150', () => {
      expect(component.sfiLabel(150)).toBe('High');
    });

    it('returns "Moderate" for SFI >= 100', () => {
      expect(component.sfiLabel(100)).toBe('Moderate');
    });

    it('returns "Low" for SFI >= 80', () => {
      expect(component.sfiLabel(80)).toBe('Low');
    });

    it('returns "Very Low" for SFI < 80', () => {
      expect(component.sfiLabel(70)).toBe('Very Low');
    });
  });

  describe('bandList getter', () => {
    it('returns empty array when conditions is null', () => {
      component.conditions = null;
      expect(component.bandList).toEqual([]);
    });

    it('returns bands in BAND_ORDER that exist in conditions', () => {
      // makeConditions() has 20m, 40m, 160m — all are in BAND_ORDER
      expect(component.bandList.length).toBe(3);
    });

    it('filters out bands not in conditions', () => {
      // Only 20m is in the bands map
      component.conditions = {
        ...makeConditions(),
        bands: {
          '20m': { band: '20m', displayName: '20 Meters', condition: 'GOOD', description: '' },
        },
      };
      expect(component.bandList.length).toBe(1);
    });
  });

  describe('load() error path', () => {
    it('shows error message when service fails', () => {
      mockService.getConditions.mockReturnValue(throwError(() => new Error('Network error')));
      component.load();
      expect(component.error).toBe('Unable to load propagation data.');
      expect(component.loading).toBe(false);
    });
  });

  describe('togglePanel()', () => {
    it('toggles isExpanded', () => {
      expect(component.isExpanded).toBe(true);
      component.togglePanel();
      expect(component.isExpanded).toBe(false);
    });
  });
});
