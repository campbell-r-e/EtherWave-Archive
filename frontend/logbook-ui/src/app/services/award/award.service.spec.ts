import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AwardService, AwardProgress } from './award.service';

const mockProgress: AwardProgress = {
  logId: 5,
  logName: 'Test Log',
  totalQsos: 100,
  dxcc: {
    workedCountries: ['USA', 'CAN'],
    confirmedCountries: ['USA'],
    workedCount: 2,
    confirmedCount: 1,
    totalEntities: 340,
  },
  was: {
    workedStates: ['OH', 'CA'],
    confirmedStates: ['OH'],
    workedCount: 2,
    confirmedCount: 1,
    totalStates: 50,
  },
  vucc: {
    workedGrids: ['EN81', 'EM72'],
    confirmedGrids: ['EN81'],
    workedCount: 2,
    confirmedCount: 1,
    threshold: 100,
  },
};

describe('AwardService', () => {
  let service: AwardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AwardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getProgress()', () => {
    it('GETs award progress for the given logId', () => {
      let result: AwardProgress | null = null;
      service.getProgress(5).subscribe((p) => (result = p));

      const req = httpMock.expectOne((r) => r.url.includes('/awards/5'));
      expect(req.request.method).toBe('GET');
      req.flush(mockProgress);

      expect(result).toEqual(mockProgress);
    });

    it('passes the correct logId in the URL', () => {
      service.getProgress(42).subscribe();
      const req = httpMock.expectOne((r) => r.url.includes('/awards/42'));
      req.flush(mockProgress);
      expect(req.request.url).toContain('/awards/42');
    });
  });
});
