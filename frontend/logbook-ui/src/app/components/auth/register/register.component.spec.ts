import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../../services/auth/auth.service';
import { ThemeService } from '../../../services/theme/theme.service';
import { Router } from '@angular/router';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let mockAuthService: { register: jest.Mock; isLoggedIn: jest.Mock };
  let mockRouter: { navigate: jest.Mock };

  beforeEach(async () => {
    mockAuthService = {
      register: jest.fn(() =>
        of({ token: 'tok', type: 'Bearer', userId: 2, username: 'newuser', roles: [] })
      ),
      isLoggedIn: jest.fn(() => false),
    };
    mockRouter = { navigate: jest.fn() };

    const mockThemeService = { getCurrentTheme: jest.fn(() => 'light'), isDarkTheme: jest.fn(() => false) };

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
        { provide: ThemeService, useValue: mockThemeService },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  describe('onSubmit()', () => {
    const validForm = () => {
      component.registerForm.patchValue({
        username: 'newuser',
        password: 'password123',
        confirmPassword: 'password123',
        callsign: 'W1AW',
      });
    };

    it('does not call register when form is invalid', () => {
      component.registerForm.reset();
      component.onSubmit();
      expect(mockAuthService.register).not.toHaveBeenCalled();
    });

    it('calls authService.register with form values', () => {
      validForm();
      component.onSubmit();
      expect(mockAuthService.register).toHaveBeenCalledWith(
        expect.objectContaining({ username: 'newuser' })
      );
    });

    it('uppercases callsign before registering', () => {
      component.registerForm.patchValue({
        username: 'newuser',
        password: 'password123',
        confirmPassword: 'password123',
        callsign: 'w1aw',
      });
      component.onSubmit();
      expect(mockAuthService.register).toHaveBeenCalledWith(
        expect.objectContaining({ callsign: 'W1AW' })
      );
    });

    it('navigates to "/" on successful registration', () => {
      validForm();
      component.onSubmit();
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
    });

    it('shows error message on registration failure', () => {
      mockAuthService.register.mockReturnValue(
        throwError(() => ({ error: { message: 'Username taken' } }))
      );
      validForm();
      component.onSubmit();
      expect(component.error).toBe('Username taken');
      expect(component.loading).toBe(false);
    });

    it('uses default error message when no error message from server', () => {
      mockAuthService.register.mockReturnValue(throwError(() => new Error('network')));
      validForm();
      component.onSubmit();
      expect(component.error).toBe('Registration failed. Please try again.');
    });

    it('sets submitted to true on submit', () => {
      component.onSubmit();
      expect(component.submitted).toBe(true);
    });
  });

  describe('passwordMatchValidator', () => {
    it('sets passwordMismatch error when passwords do not match', () => {
      component.registerForm.patchValue({
        username: 'newuser',
        password: 'password123',
        confirmPassword: 'different',
      });
      expect(component.registerForm.get('confirmPassword')?.errors?.['passwordMismatch']).toBe(true);
    });

    it('clears passwordMismatch error when passwords match', () => {
      component.registerForm.patchValue({
        username: 'newuser',
        password: 'password123',
        confirmPassword: 'password123',
      });
      expect(component.registerForm.get('confirmPassword')?.errors?.['passwordMismatch']).toBeFalsy();
    });
  });
});
