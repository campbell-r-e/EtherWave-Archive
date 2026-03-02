import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { PropagationService, PropagationConditions } from './propagation.service';

const mockConditions: PropagationConditions = {
  sfi: 150,
  kIndex: 2,
  aIndex: 10,
  fetchedAt: '2024-01-01T12:00:00Z',
  bands: {
    '20m': {
      band: '20m',
      displayName: '20 Meters',
      condition: 'GOOD',
      description: 'Good conditions',
    },
  },
};

describe('PropagationService', () => {
  let service: PropagationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PropagationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getConditions()', () => {
    it('GETs propagation conditions', () => {
      let result: PropagationConditions | null = null;
      service.getConditions().subscribe((c) => (result = c));

      const req = httpMock.expectOne((r) => r.url.includes('/propagation/conditions'));
      expect(req.request.method).toBe('GET');
      req.flush(mockConditions);

      expect(result).toEqual(mockConditions);
    });

    it('returns band data including 20m', () => {
      let result: PropagationConditions | null = null;
      service.getConditions().subscribe((c) => (result = c));

      httpMock.expectOne((r) => r.url.includes('/propagation/conditions')).flush(mockConditions);

      expect(result?.bands['20m'].condition).toBe('GOOD');
    });
  });
});
