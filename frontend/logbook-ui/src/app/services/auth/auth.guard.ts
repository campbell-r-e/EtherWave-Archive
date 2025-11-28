import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    if (this.authService.isLoggedIn()) {
      // Check for required roles if specified in route data
      const requiredRoles = route.data['roles'];
      if (requiredRoles) {
        const hasRole = requiredRoles.some((role: string) => this.authService.hasRole(role));
        if (!hasRole) {
          // User doesn't have required role, redirect to home
          this.router.navigate(['/']);
          return false;
        }
      }
      return true;
    }

    // Not logged in, redirect to login page with return url
    this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
}
