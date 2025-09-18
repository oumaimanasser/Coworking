import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  username: string;
  roles: string[];
  message?: string;
}

export interface User {
  id?: number;
  username: string;
  email: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:9090/auth';
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    this.loadUserFromStorage();
  }

  private loadUserFromStorage(): void {
    const userData = localStorage.getItem('user');
    const token = localStorage.getItem('token');
    if (userData && token) {
      try {
        this.currentUserSubject.next(JSON.parse(userData));
      } catch {
        this.logout();
      }
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(res => {
        localStorage.setItem('token', res.token);

        const user: User = {
          username: res.username,
          email: res.email,
          roles: res.roles || []
        };

        localStorage.setItem('user', JSON.stringify(user));
        localStorage.setItem('roles', JSON.stringify(res.roles || []));
        this.currentUserSubject.next(user);
      }),
      catchError(error => {
        console.error('Erreur de connexion:', error);
        return throwError(() => new Error(error.error?.message || 'Échec de la connexion'));
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('roles');
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders({
      Authorization: token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    });
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  register(userData: { username: string; email: string; password: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    return user.roles.includes('ROLE_ADMIN');
  }

  getUserRoles(): string[] {
    const user = this.getCurrentUser();
    return user?.roles || [];
  }

  hasRole(role: string): boolean {
    const roles = this.getUserRoles();
    return roles.includes(role) || roles.includes(`ROLE_${role}`);
  }

  getUserInfoFromToken(): any {
    const token = this.getToken();
    if (!token) return null;

    try {
      const payload = token.split('.')[1];
      const decodedPayload = atob(payload);
      return JSON.parse(decodedPayload);
    } catch (e) {
      console.error('Error decoding token:', e);
      return null;
    }
  }
}