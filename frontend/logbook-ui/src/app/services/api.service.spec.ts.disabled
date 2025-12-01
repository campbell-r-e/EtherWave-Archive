import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;

  const API_URL = 'http://localhost:8080/api';

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getToken']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        ApiService,
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;

    authService.getToken.and.returnValue('fake-jwt-token');
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ==================== SERVICE CREATION TEST ====================

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ==================== QSO API TESTS ====================

  describe('QSO API', () => {
    it('should get all QSOs for a log', () => {
      const mockQSOs = [
        { id: 1, callsign: 'W1AW', frequencyKhz: 14250 },
        { id: 2, callsign: 'K2ABC', frequencyKhz: 7125 }
      ];

      service.getQSOs(1).subscribe(qsos => {
        expect(qsos.length).toBe(2);
        expect(qsos).toEqual(mockQSOs);
      });

      const req = httpMock.expectOne(`${API_URL}/qsos?logId=1`);
      expect(req.request.method).toBe('GET');
      expect(req.request.headers.get('Authorization')).toBe('Bearer fake-jwt-token');
      req.flush(mockQSOs);
    });

    it('should create a QSO', () => {
      const newQSO = {
        callsign: 'W1AW',
        frequencyKhz: 14250,
        mode: 'SSB',
        rstSent: '59',
        rstRcvd: '59'
      };

      service.createQSO(newQSO).subscribe(qso => {
        expect(qso.id).toBe(1);
        expect(qso.callsign).toBe('W1AW');
      });

      const req = httpMock.expectOne(`${API_URL}/qsos`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newQSO);
      req.flush({ id: 1, ...newQSO });
    });

    it('should update a QSO', () => {
      const updatedQSO = {
        id: 1,
        callsign: 'W1AW',
        frequencyKhz: 14255
      };

      service.updateQSO(1, updatedQSO).subscribe(qso => {
        expect(qso.frequencyKhz).toBe(14255);
      });

      const req = httpMock.expectOne(`${API_URL}/qsos/1`);
      expect(req.request.method).toBe('PUT');
      req.flush(updatedQSO);
    });

    it('should delete a QSO', () => {
      service.deleteQSO(1).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${API_URL}/qsos/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });

    it('should search QSOs by callsign', () => {
      const mockResults = [
        { id: 1, callsign: 'W1AW', frequencyKhz: 14250 }
      ];

      service.searchQSOs(1, 'W1AW').subscribe(qsos => {
        expect(qsos.length).toBe(1);
        expect(qsos[0].callsign).toBe('W1AW');
      });

      const req = httpMock.expectOne(`${API_URL}/qsos/search?logId=1&callsign=W1AW`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResults);
    });
  });

  // ==================== LOG API TESTS ====================

  describe('Log API', () => {
    it('should get all logs', () => {
      const mockLogs = [
        { id: 1, name: 'Field Day 2025' },
        { id: 2, name: 'Winter Field Day 2025' }
      ];

      service.getLogs().subscribe(logs => {
        expect(logs.length).toBe(2);
        expect(logs).toEqual(mockLogs);
      });

      const req = httpMock.expectOne(`${API_URL}/logs`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLogs);
    });

    it('should create a log', () => {
      const newLog = {
        name: 'Test Log',
        contestCode: 'ARRL-FD'
      };

      service.createLog(newLog).subscribe(log => {
        expect(log.id).toBe(1);
        expect(log.name).toBe('Test Log');
      });

      const req = httpMock.expectOne(`${API_URL}/logs`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1, ...newLog });
    });

    it('should freeze a log', () => {
      service.freezeLog(1).subscribe(log => {
        expect(log.frozen).toBe(true);
      });

      const req = httpMock.expectOne(`${API_URL}/logs/1/freeze`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1, frozen: true });
    });

    it('should unfreeze a log', () => {
      service.unfreezeLog(1).subscribe(log => {
        expect(log.frozen).toBe(false);
      });

      const req = httpMock.expectOne(`${API_URL}/logs/1/unfreeze`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1, frozen: false });
    });
  });

  // ==================== STATION API TESTS ====================

  describe('Station API', () => {
    it('should get all stations', () => {
      const mockStations = [
        { id: 1, callsign: 'W1AW', gridSquare: 'FN31' }
      ];

      service.getStations().subscribe(stations => {
        expect(stations.length).toBe(1);
        expect(stations[0].callsign).toBe('W1AW');
      });

      const req = httpMock.expectOne(`${API_URL}/stations`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStations);
    });

    it('should create a station', () => {
      const newStation = {
        callsign: 'K2ABC',
        gridSquare: 'FN42'
      };

      service.createStation(newStation).subscribe(station => {
        expect(station.callsign).toBe('K2ABC');
      });

      const req = httpMock.expectOne(`${API_URL}/stations`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1, ...newStation });
    });
  });

  // ==================== EXPORT API TESTS ====================

  describe('Export API', () => {
    it('should export log as ADIF', () => {
      const mockAdif = '<ADIF_VER:5>3.1.4<EOH>';

      service.exportAdif(1).subscribe(data => {
        expect(data).toBe(mockAdif);
      });

      const req = httpMock.expectOne(`${API_URL}/export/adif/1`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('text');
      req.flush(mockAdif);
    });

    it('should export log as Cabrillo', () => {
      const mockCabrillo = 'START-OF-LOG: 3.0';

      service.exportCabrillo(1).subscribe(data => {
        expect(data).toContain('START-OF-LOG');
      });

      const req = httpMock.expectOne(`${API_URL}/export/cabrillo/1`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('text');
      req.flush(mockCabrillo);
    });
  });

  // ==================== IMPORT API TESTS ====================

  describe('Import API', () => {
    it('should import ADIF file', () => {
      const mockFile = new File(['content'], 'test.adi', { type: 'text/plain' });
      const mockResult = {
        successCount: 10,
        failureCount: 0
      };

      service.importAdif(mockFile, 1).subscribe(result => {
        expect(result.successCount).toBe(10);
        expect(result.failureCount).toBe(0);
      });

      const req = httpMock.expectOne(`${API_URL}/import/adif?logId=1`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResult);
    });
  });

  // ==================== INVITATION API TESTS ====================

  describe('Invitation API', () => {
    it('should get invitations', () => {
      const mockInvitations = [
        { id: 1, logId: 1, invitedUserId: 2, role: 'STATION', status: 'PENDING' }
      ];

      service.getInvitations().subscribe(invitations => {
        expect(invitations.length).toBe(1);
        expect(invitations[0].status).toBe('PENDING');
      });

      const req = httpMock.expectOne(`${API_URL}/invitations`);
      expect(req.request.method).toBe('GET');
      req.flush(mockInvitations);
    });

    it('should accept invitation', () => {
      service.acceptInvitation(1).subscribe(response => {
        expect(response.status).toBe('ACCEPTED');
      });

      const req = httpMock.expectOne(`${API_URL}/invitations/1/accept`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1, status: 'ACCEPTED' });
    });

    it('should decline invitation', () => {
      service.declineInvitation(1).subscribe(response => {
        expect(response.status).toBe('DECLINED');
      });

      const req = httpMock.expectOne(`${API_URL}/invitations/1/decline`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1, status: 'DECLINED' });
    });
  });

  // ==================== CONTEST API TESTS ====================

  describe('Contest API', () => {
    it('should get all contests', () => {
      const mockContests = [
        { contestCode: 'ARRL-FD', contestName: 'ARRL Field Day' },
        { contestCode: 'WFD', contestName: 'Winter Field Day' }
      ];

      service.getContests().subscribe(contests => {
        expect(contests.length).toBe(2);
        expect(contests[0].contestCode).toBe('ARRL-FD');
      });

      const req = httpMock.expectOne(`${API_URL}/contests`);
      expect(req.request.method).toBe('GET');
      req.flush(mockContests);
    });
  });

  // ==================== ERROR HANDLING TESTS ====================

  describe('Error Handling', () => {
    it('should handle 401 Unauthorized error', () => {
      service.getQSOs(1).subscribe(
        () => fail('should have failed with 401 error'),
        error => {
          expect(error.status).toBe(401);
        }
      );

      const req = httpMock.expectOne(`${API_URL}/qsos?logId=1`);
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle 404 Not Found error', () => {
      service.getQSOs(999).subscribe(
        () => fail('should have failed with 404 error'),
        error => {
          expect(error.status).toBe(404);
        }
      );

      const req = httpMock.expectOne(`${API_URL}/qsos?logId=999`);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });

    it('should handle 500 Server Error', () => {
      service.createQSO({}).subscribe(
        () => fail('should have failed with 500 error'),
        error => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne(`${API_URL}/qsos`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should handle network errors', () => {
      service.getLogs().subscribe(
        () => fail('should have failed with network error'),
        error => {
          expect(error.status).toBe(0);
        }
      );

      const req = httpMock.expectOne(`${API_URL}/logs`);
      req.error(new ErrorEvent('Network error'));
    });
  });

  // ==================== AUTHORIZATION HEADER TESTS ====================

  describe('Authorization Headers', () => {
    it('should include Authorization header in all requests', () => {
      service.getQSOs(1).subscribe();

      const req = httpMock.expectOne(`${API_URL}/qsos?logId=1`);
      expect(req.request.headers.get('Authorization')).toBe('Bearer fake-jwt-token');
      req.flush([]);
    });

    it('should not include Authorization header if no token', () => {
      authService.getToken.and.returnValue(null);

      service.getQSOs(1).subscribe();

      const req = httpMock.expectOne(`${API_URL}/qsos?logId=1`);
      expect(req.request.headers.has('Authorization')).toBeFalsy();
      req.flush([]);
    });
  });

  // ==================== PAGINATION TESTS ====================

  describe('Pagination', () => {
    it('should handle paginated QSO requests', () => {
      const mockPage = {
        content: [{ id: 1, callsign: 'W1AW' }],
        totalElements: 100,
        totalPages: 10,
        number: 0
      };

      service.getQSOsPaginated(1, 0, 10).subscribe(page => {
        expect(page.content.length).toBe(1);
        expect(page.totalElements).toBe(100);
      });

      const req = httpMock.expectOne(`${API_URL}/qsos/paginated?logId=1&page=0&size=10`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPage);
    });
  });

  // ==================== TELEMETRY API TESTS ====================

  describe('Telemetry API', () => {
    it('should record rig telemetry', () => {
      const telemetry = {
        stationId: 1,
        frequencyHz: 14250000,
        mode: 'USB',
        power: 100
      };

      service.recordTelemetry(telemetry).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${API_URL}/telemetry`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(telemetry);
      req.flush({ id: 1, ...telemetry });
    });
  });
});
