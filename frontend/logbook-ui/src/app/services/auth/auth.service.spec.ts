import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { User, UserRole } from '../../models/auth/user.model';

const TOKEN_KEY = 'auth-token';
const USER_KEY = 'auth-user';

const mockUser = (override: Partial<User> = {}): User => ({
  id: 1,
  username: 'testuser',
  roles: [UserRole.ROLE_USER],
  enabled: true,
  createdAt: '2024-01-01T00:00:00Z',
  ...override,
});

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('constructor', () => {
    it('starts with null user when localStorage is empty', () => {
      expect(service.currentUserValue).toBeNull();
    });

    it('restores user from localStorage', () => {
      const user = mockUser({ id: 5, username: 'restored' });
      localStorage.setItem(USER_KEY, JSON.stringify(user));

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [provideHttpClient(), provideHttpClientTesting()],
      });
      const newService = TestBed.inject(AuthService);

      expect(newService.currentUserValue?.username).toBe('restored');
    });

    it('handles corrupted localStorage gracefully', () => {
      localStorage.setItem(USER_KEY, 'not-valid-json{{{');

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [provideHttpClient(), provideHttpClientTesting()],
      });
      const newService = TestBed.inject(AuthService);

      expect(newService.currentUserValue).toBeNull();
      expect(localStorage.getItem(USER_KEY)).toBeNull();
    });
  });

  describe('login()', () => {
    it('saves token on successful login', () => {
      const mockResponse = {
        token: 'test-token',
        type: 'Bearer',
        userId: 1,
        username: 'u',
        roles: [UserRole.ROLE_USER],
      };

      service.login({ username: 'u', password: 'p' }).subscribe();

      const loginReq = httpMock.expectOne((r) => r.url.includes('/auth/login'));
      expect(loginReq.request.method).toBe('POST');
      loginReq.flush(mockResponse);

      // loadCurrentUser triggers a /me request
      const meReq = httpMock.expectOne((r) => r.url.includes('/auth/me'));
      meReq.flush(mockUser());

      expect(localStorage.getItem(TOKEN_KEY)).toBe('test-token');
    });

    it('calls loadCurrentUser after login', () => {
      const mockResponse = {
        token: 'tok',
        type: 'Bearer',
        userId: 1,
        username: 'u',
        roles: [],
      };

      service.login({ username: 'u', password: 'p' }).subscribe();
      httpMock.expectOne((r) => r.url.includes('/auth/login')).flush(mockResponse);

      const meReq = httpMock.expectOne((r) => r.url.includes('/auth/me'));
      const user = mockUser();
      meReq.flush(user);

      expect(service.currentUserValue?.username).toBe('testuser');
    });
  });

  describe('logout()', () => {
    it('removes token and user from localStorage', () => {
      localStorage.setItem(TOKEN_KEY, 'tok');
      localStorage.setItem(USER_KEY, '{}');

      service.logout();

      expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(USER_KEY)).toBeNull();
    });

    it('emits null to currentUser observable', (done) => {
      (service as any).currentUserSubject.next(mockUser());

      service.currentUser.subscribe((u) => {
        if (u === null) {
          done();
        }
      });

      service.logout();
    });
  });

  describe('isLoggedIn()', () => {
    it('returns true when token exists', () => {
      localStorage.setItem(TOKEN_KEY, 'tok');
      expect(service.isLoggedIn()).toBe(true);
    });

    it('returns false when no token', () => {
      expect(service.isLoggedIn()).toBe(false);
    });
  });

  describe('hasRole()', () => {
    it('returns true when user has the role', () => {
      (service as any).currentUserSubject.next(mockUser({ roles: [UserRole.ROLE_ADMIN] }));
      expect(service.hasRole(UserRole.ROLE_ADMIN)).toBe(true);
    });

    it('returns false when user does not have the role', () => {
      (service as any).currentUserSubject.next(mockUser({ roles: [UserRole.ROLE_USER] }));
      expect(service.hasRole(UserRole.ROLE_ADMIN)).toBe(false);
    });

    it('returns false when there is no user', () => {
      expect(service.hasRole(UserRole.ROLE_USER)).toBe(false);
    });
  });

  describe('isAdmin()', () => {
    it('returns true when user has ROLE_ADMIN', () => {
      (service as any).currentUserSubject.next(mockUser({ roles: [UserRole.ROLE_ADMIN] }));
      expect(service.isAdmin()).toBe(true);
    });

    it('returns false when user lacks ROLE_ADMIN', () => {
      (service as any).currentUserSubject.next(mockUser({ roles: [UserRole.ROLE_USER] }));
      expect(service.isAdmin()).toBe(false);
    });
  });

  describe('getCurrentUserId()', () => {
    it('returns user id when user is present', () => {
      (service as any).currentUserSubject.next(mockUser({ id: 42 }));
      expect(service.getCurrentUserId()).toBe(42);
    });

    it('returns null when no user', () => {
      expect(service.getCurrentUserId()).toBeNull();
    });
  });

  describe('getCurrentUsername()', () => {
    it('returns username when user is present', () => {
      (service as any).currentUserSubject.next(mockUser({ username: 'w1aw' }));
      expect(service.getCurrentUsername()).toBe('w1aw');
    });

    it('returns null when no user', () => {
      expect(service.getCurrentUsername()).toBeNull();
    });
  });

  describe('saveToken() / getToken()', () => {
    it('saves and retrieves a token', () => {
      service.saveToken('my-jwt');
      expect(service.getToken()).toBe('my-jwt');
    });
  });

  describe('loadCurrentUser()', () => {
    it('updates currentUser on success', () => {
      const user = mockUser({ id: 7, username: 'loaded' });

      service.loadCurrentUser();
      const req = httpMock.expectOne((r) => r.url.includes('/auth/me'));
      req.flush(user);

      expect(service.currentUserValue?.username).toBe('loaded');
      expect(localStorage.getItem(USER_KEY)).toContain('loaded');
    });

    it('calls logout on error', () => {
      const logoutSpy = jest.spyOn(service, 'logout');

      service.loadCurrentUser();
      const req = httpMock.expectOne((r) => r.url.includes('/auth/me'));
      req.flush('error', { status: 401, statusText: 'Unauthorized' });

      expect(logoutSpy).toHaveBeenCalled();
    });
  });
});
