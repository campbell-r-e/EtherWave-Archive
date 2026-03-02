import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../services/auth/auth.service';
import { ThemeService } from '../../../services/theme/theme.service';
import { Router, ActivatedRoute } from '@angular/router';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let mockAuthService: { login: jest.Mock; isLoggedIn: jest.Mock };
  let mockRouter: { navigate: jest.Mock };

  beforeEach(async () => {
    mockAuthService = {
      login: jest.fn(() =>
        of({ token: 'tok', type: 'Bearer', userId: 1, username: 'u', roles: [] })
      ),
      isLoggedIn: jest.fn(() => false),
    };
    mockRouter = { navigate: jest.fn() };

    const mockThemeService = { getCurrentTheme: jest.fn(() => 'light'), isDarkTheme: jest.fn(() => false) };
    const mockRoute = { snapshot: { queryParams: { returnUrl: '/dashboard' } } };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: ThemeService, useValue: mockThemeService },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  describe('onSubmit()', () => {
    it('does not call login when form is empty (invalid)', () => {
      component.loginForm.reset();
      component.onSubmit();
      expect(mockAuthService.login).not.toHaveBeenCalled();
    });

    it('calls authService.login with form values', () => {
      component.loginForm.setValue({ username: 'w1aw', password: 'secret' });
      component.onSubmit();
      expect(mockAuthService.login).toHaveBeenCalledWith({ username: 'w1aw', password: 'secret' });
    });

    it('navigates to returnUrl on successful login', () => {
      component.loginForm.setValue({ username: 'w1aw', password: 'secret' });
      component.onSubmit();
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('sets error message on failed login', () => {
      mockAuthService.login.mockReturnValue(throwError(() => new Error('Unauthorized')));
      component.loginForm.setValue({ username: 'w1aw', password: 'wrong' });
      component.onSubmit();
      expect(component.error).toBe('Invalid username or password');
      expect(component.loading).toBe(false);
    });

    it('sets submitted to true on submit', () => {
      component.onSubmit();
      expect(component.submitted).toBe(true);
    });
  });

  describe('form validation', () => {
    it('form is invalid when empty', () => {
      expect(component.loginForm.invalid).toBe(true);
    });

    it('form is valid with username and password', () => {
      component.loginForm.setValue({ username: 'w1aw', password: 'secret' });
      expect(component.loginForm.valid).toBe(true);
    });
  });
});
