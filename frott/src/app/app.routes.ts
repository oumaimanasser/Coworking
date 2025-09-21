import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import {AdminDashboardComponent} from './admin-dashboard/admin-dashboard.component';
import {ReservationComponent} from './reservation/reservation.component';
import { ProfileComponent } from './profile/profile.component';
import { AuthGuard } from './auth.guard';
export const routes: Routes = [
  { path: 'home', component: HomeComponent },
  { path: 'reservation', component: ReservationComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'admin', component: AdminDashboardComponent },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: '**', redirectTo: '/home' }
];
