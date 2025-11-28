import { Routes } from '@angular/router';
import { AuthGuard } from './services/auth/auth.guard';

// Auth Components
import { LoginComponent } from './components/auth/login/login.component';
import { RegisterComponent } from './components/auth/register/register.component';

// Dashboard
import { DashboardComponent } from './components/dashboard/dashboard.component';

// Log Management
import { InvitationsComponent } from './components/log/invitations/invitations.component';

export const routes: Routes = [
  // Public routes
  {
    path: 'login',
    component: LoginComponent,
    title: 'Login - Ham Radio Logbook'
  },
  {
    path: 'register',
    component: RegisterComponent,
    title: 'Register - Ham Radio Logbook'
  },

  // Protected routes
  {
    path: '',
    component: DashboardComponent,
    canActivate: [AuthGuard],
    title: 'Dashboard - Ham Radio Logbook'
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard],
    title: 'Dashboard - Ham Radio Logbook'
  },
  {
    path: 'invitations',
    component: InvitationsComponent,
    canActivate: [AuthGuard],
    title: 'Invitations - Ham Radio Logbook'
  },

  // Wildcard redirect to dashboard
  {
    path: '**',
    redirectTo: '',
    pathMatch: 'full'
  }
];
