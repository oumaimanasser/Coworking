import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import {AdminDashboardComponent} from './admin-dashboard/admin-dashboard.component';
import {ReservationComponent} from './reservation/reservation.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'home', component: HomeComponent },
  { path: 'admin', component: AdminDashboardComponent },
  { path: 'reservation', component: ReservationComponent },// dashboard admin
];
