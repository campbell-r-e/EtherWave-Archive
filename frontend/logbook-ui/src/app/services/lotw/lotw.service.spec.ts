import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { LotwService, LotwSyncRequest, LotwSyncResult } from './lotw.service';

describe('LotwService', () => {
  let service: LotwService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LotwService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('sync()', () => {
    it('POSTs to the correct URL with request body', () => {
      const request: LotwSyncRequest = {
        lotwCallsign: 'W1AW',
        lotwPassword: 'secret',
        since: '2024-01-01',
      };

      const mockResult: LotwSyncResult = {
        downloaded: 50,
        matched: 30,
        updated: 25,
        message: 'Sync complete',
      };

      let result: LotwSyncResult | null = null;
      service.sync(7, request).subscribe((r) => (result = r));

      const req = httpMock.expectOne((r) => r.url.includes('/lotw/sync/7'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResult);

      expect(result).toEqual(mockResult);
    });

    it('includes the logId in the URL path', () => {
      service.sync(42, { lotwCallsign: 'K1ABC', lotwPassword: 'pw' }).subscribe();
      const req = httpMock.expectOne((r) => r.url.includes('/lotw/sync/42'));
      req.flush({ downloaded: 0, matched: 0, updated: 0, message: '' });
      expect(req.request.url).toContain('/lotw/sync/42');
    });

    it('sends sync request without "since" when not provided', () => {
      const request: LotwSyncRequest = {
        lotwCallsign: 'N5KO',
        lotwPassword: 'abc',
      };

      service.sync(1, request).subscribe();
      const req = httpMock.expectOne((r) => r.url.includes('/lotw/sync/1'));
      expect(req.request.body.since).toBeUndefined();
      req.flush({ downloaded: 0, matched: 0, updated: 0, message: '' });
    });
  });
});
