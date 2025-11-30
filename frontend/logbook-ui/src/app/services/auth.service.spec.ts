import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  const API_URL = 'http://localhost:8080/api/auth';

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ==================== LOGIN TESTS ====================

  it('should login successfully', (done) => {
    const credentials = { username: 'testuser', password: 'password' };
    const mockResponse = {
      token: 'fake-jwt-token',
      user: { id: 1, username: 'testuser', callsign: 'W1AW' }
    };

    service.login(credentials.username, credentials.password).subscribe(response => {
      expect(response.token).toBe('fake-jwt-token');
      expect(response.user.username).toBe('testuser');
      done();
    });

    const req = httpMock.expectOne(`${API_URL}/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(credentials);
    req.flush(mockResponse);
  });

  it('should store token in localStorage on successful login', (done) => {
    const mockResponse = {
      token: 'fake-jwt-token',
      user: { id: 1, username: 'testuser' }
    };

    service.login('testuser', 'password').subscribe(() => {
      expect(localStorage.getItem('token')).toBe('fake-jwt-token');
      done();
    });

    const req = httpMock.expectOne(`${API_URL}/login`);
    req.flush(mockResponse);
  });

  it('should store user in localStorage on successful login', (done) => {
    const mockResponse = {
      token: 'fake-jwt-token',
      user: { id: 1, username: 'testuser', callsign: 'W1AW' }
    };

    service.login('testuser', 'password').subscribe(() => {
      const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
      expect(storedUser.username).toBe('testuser');
      expect(storedUser.callsign).toBe('W1AW');
      done();
    });

    const req = httpMock.expectOne(`${API_URL}/login`);
    req.flush(mockResponse);
  });

  it('should emit authenticated state on login', (done) => {
    const mockResponse = {
      token: 'fake-jwt-token',
      user: { id: 1, username: 'testuser' }
    };

    service.isAuthenticated$.subscribe(isAuth => {
      if (isAuth) {
        expect(isAuth).toBeTruthy();
        done();
      }
    });

    service.login('testuser', 'password').subscribe();

    const req = httpMock.expectOne(`${API_URL}/login`);
    req.flush(mockResponse);
  });

  it('should handle login failure', (done) => {
    service.login('testuser', 'wrongpassword').subscribe(
      () => fail('should have failed'),
      error => {
        expect(error.status).toBe(401);
        done();
      }
    );

    const req = httpMock.expectOne(`${API_URL}/login`);
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });

  // ==================== REGISTRATION TESTS ====================

  it('should register successfully', (done) => {
    const userData = {
      username: 'newuser',
      email: 'new@example.com',
      password: 'password123',
      callsign: 'K2ABC'
    };
    const mockResponse = { success: true, message: 'User registered' };

    service.register(userData).subscribe(response => {
      expect(response.success).toBeTruthy();
      done();
    });

    const req = httpMock.expectOne(`${API_URL}/register`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(userData);
    req.flush(mockResponse);
  });

  it('should handle registration failure', (done) => {
    const userData = {
      username: 'existinguser',
      email: 'existing@example.com',
      password: 'password',
      callsign: 'W1AW'
    };

    service.register(userData).subscribe(
      () => fail('should have failed'),
      error => {
        expect(error.status).toBe(400);
        done();
      }
    );

    const req = httpMock.expectOne(`${API_URL}/register`);
    req.flush('Username already exists', { status: 400, statusText: 'Bad Request' });
  });

  // ==================== LOGOUT TESTS ====================

  it('should logout and clear localStorage', () => {
    localStorage.setItem('token', 'fake-token');
    localStorage.setItem('user', JSON.stringify({ username: 'test' }));

    service.logout();

    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('should emit unauthenticated state on logout', (done) => {
    localStorage.setItem('token', 'fake-token');

    service.isAuthenticated$.subscribe(isAuth => {
      if (!isAuth) {
        expect(isAuth).toBeFalsy();
        done();
      }
    });

    service.logout();
  });

  it('should navigate to login on logout', () => {
    service.logout();

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  // ==================== TOKEN MANAGEMENT TESTS ====================

  it('should get token from localStorage', () => {
    localStorage.setItem('token', 'stored-token');

    const token = service.getToken();

    expect(token).toBe('stored-token');
  });

  it('should return null if no token stored', () => {
    const token = service.getToken();

    expect(token).toBeNull();
  });

  it('should check if user is authenticated', () => {
    expect(service.isAuthenticated()).toBeFalsy();

    localStorage.setItem('token', 'fake-token');
    expect(service.isAuthenticated()).toBeTruthy();
  });

  it('should validate token expiration', () => {
    const futureToken = service.createMockToken({ exp: Date.now() / 1000 + 3600 }); // 1 hour from now
    localStorage.setItem('token', futureToken);

    expect(service.isTokenExpired()).toBeFalsy();
  });

  it('should detect expired token', () => {
    const expiredToken = service.createMockToken({ exp: Date.now() / 1000 - 3600 }); // 1 hour ago
    localStorage.setItem('token', expiredToken);

    expect(service.isTokenExpired()).toBeTruthy();
  });

  it('should logout on expired token', () => {
    const expiredToken = service.createMockToken({ exp: Date.now() / 1000 - 3600 });
    localStorage.setItem('token', expiredToken);
    spyOn(service, 'logout');

    service.checkTokenExpiration();

    expect(service.logout).toHaveBeenCalled();
  });

  // ==================== USER MANAGEMENT TESTS ====================

  it('should get current user from localStorage', () => {
    const user = { id: 1, username: 'testuser', callsign: 'W1AW' };
    localStorage.setItem('user', JSON.stringify(user));

    const currentUser = service.getCurrentUser();

    expect(currentUser?.username).toBe('testuser');
    expect(currentUser?.callsign).toBe('W1AW');
  });

  it('should return null if no user stored', () => {
    const currentUser = service.getCurrentUser();

    expect(currentUser).toBeNull();
  });

  it('should update user profile', (done) => {
    const updatedUser = {
      id: 1,
      username: 'testuser',
      email: 'updated@example.com',
      callsign: 'W1AW'
    };

    service.updateProfile(updatedUser).subscribe(response => {
      expect(response.email).toBe('updated@example.com');
      done();
    });

    const req = httpMock.expectOne(`${API_URL}/profile`);
    expect(req.request.method).toBe('PUT');
    req.flush(updatedUser);
  });

  // ==================== PASSWORD MANAGEMENT TESTS ====================

  it('should change password', (done) => {
    const passwordData = {
      currentPassword: 'oldpass',
      newPassword: 'newpass'
    };

    service.changePassword(passwordData.currentPassword, passwordData.newPassword)
      .subscribe(response => {
        expect(response.success).toBeTruthy();
        done();
      });

    const req = httpMock.expectOne(`${API_URL}/change-password`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true });
  });

  it('should request password reset', (done) => {
    const email = 'user@example.com';

    service.requestPasswordReset(email).subscribe(response => {
      expect(response.success).toBeTruthy();
      done();
    });

    const req = httpMock.expectOne(`${API_URL}/forgot-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email });
    req.flush({ success: true });
  });

  it('should reset password with token', (done) => {
    const resetData = {
      token: 'reset-token',
      newPassword: 'newpassword123'
    };

    service.resetPassword(resetData.token, resetData.newPassword)
      .subscribe(response => {
        expect(response.success).toBeTruthy();
        done();
      });

    const req = httpMock.expectOne(`${API_URL}/reset-password`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true });
  });

  // ==================== TOKEN REFRESH TESTS ====================

  it('should refresh token', (done) => {
    const mockResponse = { token: 'new-token' };

    service.refreshToken().subscribe(response => {
      expect(response.token).toBe('new-token');
      expect(localStorage.getItem('token')).toBe('new-token');
      done();
    });

    const req = httpMock.expectOne(`${API_URL}/refresh`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should auto-refresh token before expiration', () => {
    jasmine.clock().install();
    const almostExpiredToken = service.createMockToken({ exp: Date.now() / 1000 + 300 }); // 5 minutes
    localStorage.setItem('token', almostExpiredToken);
    spyOn(service, 'refreshToken').and.returnValue(of({ token: 'new-token' }));

    service.startTokenRefreshTimer();
    jasmine.clock().tick(60000); // 1 minute

    expect(service.refreshToken).toHaveBeenCalled();

    jasmine.clock().uninstall();
  });

  // ==================== ROLE & PERMISSION TESTS ====================

  it('should check user role', () => {
    const user = { id: 1, username: 'admin', role: 'ROLE_ADMIN' };
    localStorage.setItem('user', JSON.stringify(user));

    expect(service.hasRole('ROLE_ADMIN')).toBeTruthy();
    expect(service.hasRole('ROLE_USER')).toBeFalsy();
  });

  it('should check if user is admin', () => {
    const adminUser = { id: 1, username: 'admin', role: 'ROLE_ADMIN' };
    localStorage.setItem('user', JSON.stringify(adminUser));

    expect(service.isAdmin()).toBeTruthy();
  });

  it('should check if user is regular user', () => {
    const regularUser = { id: 1, username: 'user', role: 'ROLE_USER' };
    localStorage.setItem('user', JSON.stringify(regularUser));

    expect(service.isAdmin()).toBeFalsy();
  });

  // ==================== SESSION MANAGEMENT TESTS ====================

  it('should track session activity', () => {
    const lastActivity = service.getLastActivity();

    expect(lastActivity).toBeTruthy();
  });

  it('should update last activity timestamp', () => {
    const before = service.getLastActivity();

    setTimeout(() => {
      service.updateActivity();
      const after = service.getLastActivity();
      expect(after).toBeGreaterThan(before);
    }, 100);
  });

  it('should logout on inactivity timeout', () => {
    jasmine.clock().install();
    spyOn(service, 'logout');

    service.startInactivityTimer(5000); // 5 second timeout
    jasmine.clock().tick(6000);

    expect(service.logout).toHaveBeenCalled();

    jasmine.clock().uninstall();
  });

  // ==================== ERROR HANDLING TESTS ====================

  it('should handle network errors gracefully', (done) => {
    service.login('testuser', 'password').subscribe(
      () => fail('should have failed'),
      error => {
        expect(error.status).toBe(0);
        done();
      }
    );

    const req = httpMock.expectOne(`${API_URL}/login`);
    req.error(new ErrorEvent('Network error'));
  });

  it('should handle server errors', (done) => {
    service.login('testuser', 'password').subscribe(
      () => fail('should have failed'),
      error => {
        expect(error.status).toBe(500);
        done();
      }
    );

    const req = httpMock.expectOne(`${API_URL}/login`);
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
  });
});
