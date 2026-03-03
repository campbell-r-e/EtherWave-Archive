import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import {
  StationColorPreferencesService,
  DEFAULT_STATION_COLORS,
  StationColorConfig,
} from './station-color-preferences.service';

const STORAGE_KEY = 'ew_station_colors';

describe('StationColorPreferencesService', () => {
  let service: StationColorPreferencesService;
  let httpMock: HttpTestingController;

  /**
   * Helper: flush the initial loadFromBackend() request that fires in the constructor.
   */
  const flushInitial = (status: 200 | 204 = 204, body: any = null) => {
    const req = httpMock.expectOne((r) => r.url.includes('/user/station-colors'));
    req.flush(body, { status, statusText: status === 200 ? 'OK' : 'No Content' });
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StationColorPreferencesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    flushInitial();
    expect(service).toBeTruthy();
  });

  describe('constructor', () => {
    it('loads defaults when localStorage is empty', () => {
      flushInitial(204);
      expect(service.getColors()).toEqual(DEFAULT_STATION_COLORS);
    });

    it('loads saved colors from localStorage', () => {
      // Flush the pending GET from the service constructed in beforeEach
      flushInitial(204);

      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ ...DEFAULT_STATION_COLORS, station1: '#ff0000' })
      );

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [provideHttpClient(), provideHttpClientTesting()],
      });
      const newService = TestBed.inject(StationColorPreferencesService);
      const newHttpMock = TestBed.inject(HttpTestingController);

      newHttpMock.expectOne((r) => r.url.includes('/user/station-colors')).flush(null, {
        status: 204,
        statusText: 'No Content',
      });

      expect(newService.getStationColor(1)).toBe('#ff0000');
      newHttpMock.verify();
    });

    it('applies backend 200 response over localStorage', () => {
      const backendColors: StationColorConfig = {
        ...DEFAULT_STATION_COLORS,
        station1: '#aabbcc',
      };

      flushInitial(200, JSON.stringify(backendColors));

      expect(service.getStationColor(1)).toBe('#aabbcc');
    });

    it('keeps defaults when backend returns 204', () => {
      flushInitial(204);
      expect(service.getStationColor(1)).toBe(DEFAULT_STATION_COLORS.station1);
    });
  });

  describe('getStationColor()', () => {
    beforeEach(() => flushInitial());

    it('returns correct color for station 1', () => {
      expect(service.getStationColor(1)).toBe(DEFAULT_STATION_COLORS.station1);
    });

    it('returns gota color', () => {
      expect(service.getStationColor('gota')).toBe(DEFAULT_STATION_COLORS.gota);
    });
  });

  describe('setStationColor()', () => {
    beforeEach(() => flushInitial());

    it('updates station color and persists', () => {
      service.setStationColor(1, '#112233');

      // Flush the PUT from saveToBackend
      httpMock.expectOne((r) => r.method === 'PUT').flush(null);

      expect(service.getStationColor(1)).toBe('#112233');
      const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
      expect(stored.station1).toBe('#112233');
    });

    it('updates gota color', () => {
      service.setStationColor('gota', '#ffffff');
      httpMock.expectOne((r) => r.method === 'PUT').flush(null);

      expect(service.getStationColor('gota')).toBe('#ffffff');
    });
  });

  describe('isCustomized()', () => {
    beforeEach(() => flushInitial());

    it('returns false with default colors', () => {
      expect(service.isCustomized()).toBe(false);
    });

    it('returns true after changing a color', () => {
      service.setStationColor(2, '#999999');
      httpMock.expectOne((r) => r.method === 'PUT').flush(null);

      expect(service.isCustomized()).toBe(true);
    });
  });

  describe('resetToDefaults()', () => {
    beforeEach(() => flushInitial());

    it('resets colors to defaults', () => {
      service.setStationColor(1, '#custom');
      httpMock.expectOne((r) => r.method === 'PUT').flush(null);

      service.resetToDefaults();
      httpMock.expectOne((r) => r.method === 'DELETE').flush(null);

      expect(service.getColors()).toEqual(DEFAULT_STATION_COLORS);
    });

    it('calls DELETE on the backend', () => {
      service.resetToDefaults();
      const req = httpMock.expectOne((r) => r.method === 'DELETE');
      expect(req.request.url).toContain('/user/station-colors');
      req.flush(null);
    });

    it('is non-fatal when backend DELETE fails', () => {
      expect(() => {
        service.resetToDefaults();
        httpMock
          .expectOne((r) => r.method === 'DELETE')
          .flush('error', { status: 500, statusText: 'Server Error' });
      }).not.toThrow();
    });
  });

  describe('getStationColorWithOpacity()', () => {
    beforeEach(() => flushInitial());

    it('converts hex to rgba with given opacity', () => {
      // station1 default is '#0080ff'
      const result = service.getStationColorWithOpacity(1, 0.5);
      expect(result).toBe('rgba(0, 128, 255, 0.5)');
    });

    it('returns fully opaque rgba when opacity is 1', () => {
      const result = service.getStationColorWithOpacity(1, 1);
      expect(result).toBe('rgba(0, 128, 255, 1)');
    });
  });

  describe('getStationColorMap()', () => {
    beforeEach(() => flushInitial());

    it('returns a map with entries for stations 1-6 and gota', () => {
      const map = service.getStationColorMap();
      expect(map.size).toBe(7);
      expect(map.has(1)).toBe(true);
      expect(map.has(6)).toBe(true);
      expect(map.has('gota')).toBe(true);
    });
  });
});
