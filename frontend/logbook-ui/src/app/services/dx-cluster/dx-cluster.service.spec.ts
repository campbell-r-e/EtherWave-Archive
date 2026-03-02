import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { DXClusterService, DXSpot } from './dx-cluster.service';
import { of } from 'rxjs';

const makeSpot = (override: Partial<DXSpot> = {}): DXSpot => ({
  spotter: 'W1AW',
  dxCallsign: 'VK2ABC',
  frequency: 14250,
  band: '20m',
  mode: 'SSB',
  comment: 'Good signal',
  time: new Date().toISOString(),
  ...override,
});

describe('DXClusterService', () => {
  let service: DXClusterService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DXClusterService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    service.stopPolling();
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('spots$', () => {
    it('starts with an empty array', (done) => {
      service.spots$.subscribe((spots) => {
        expect(spots).toEqual([]);
        done();
      });
    });
  });

  describe('fetchSpots()', () => {
    it('GETs spots from the API', () => {
      const spots = [makeSpot(), makeSpot({ dxCallsign: 'JA1ABC' })];
      let result: DXSpot[] = [];

      service.fetchSpots(25, '20m').subscribe((s) => (result = s));

      const req = httpMock.expectOne((r) => r.url.includes('/dx-cluster/spots'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('limit')).toBe('25');
      expect(req.request.params.get('band')).toBe('20m');
      req.flush(spots);

      expect(result.length).toBe(2);
    });

    it('does not include band param when band is empty', () => {
      service.fetchSpots(50, '').subscribe();

      const req = httpMock.expectOne((r) => r.url.includes('/dx-cluster/spots'));
      expect(req.request.params.has('band')).toBe(false);
      req.flush([]);
    });
  });

  describe('stopPolling()', () => {
    it('unsubscribes the poll subscription', () => {
      // Start polling (will fire immediately due to startWith(0))
      jest.spyOn(service, 'fetchSpots').mockReturnValue(of([]));
      service.startPolling(50, '');

      expect((service as any).pollSub).not.toBeNull();
      service.stopPolling();
      expect((service as any).pollSub).toBeNull();
    });

    it('is safe to call when not polling', () => {
      expect(() => service.stopPolling()).not.toThrow();
    });
  });

  describe('startPolling()', () => {
    it('stops previous poll and starts a new one', () => {
      const stopSpy = jest.spyOn(service, 'stopPolling');
      jest.spyOn(service, 'fetchSpots').mockReturnValue(of([]));

      service.startPolling();
      expect(stopSpy).toHaveBeenCalledTimes(1);

      service.startPolling();
      expect(stopSpy).toHaveBeenCalledTimes(2);

      service.stopPolling();
    });
  });

  describe('ngOnDestroy()', () => {
    it('stops polling on destroy', () => {
      const stopSpy = jest.spyOn(service, 'stopPolling');
      service.ngOnDestroy();
      expect(stopSpy).toHaveBeenCalled();
    });
  });
});
