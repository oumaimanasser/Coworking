import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

export interface User {
  id: number;
  username: string;
  email: string;
  roles?: string[];        // Ajouter le champ roles
  authorities?: string[];  // Ajouter le champ authorities
  role?: string;           // Ajouter le champ role (pour les cas simples)
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
  authorities?: string[];
  roles?: string[];        // Ajouter le champ roles dans la réponse
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

        // Stocker l'utilisateur avec ses rôles
        const userWithRoles = {
          ...res.user,
          roles: res.roles || res.authorities || res.user.roles || [],
          authorities: res.authorities || res.user.authorities || []
        };

        localStorage.setItem('user', JSON.stringify(userWithRoles));
        this.currentUserSubject.next(userWithRoles);
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
    return !!localStorage.getItem('token');
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  register(userData: { username: string; email: string; password: string; }): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  // Méthode pour vérifier si l'utilisateur est admin
  isAdmin(): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;

    // Vérifier plusieurs formats possibles de rôles
    const hasAdminInRoles = user.roles ? user.roles.includes('ADMIN') : false;
    const hasRoleAdminInRoles = user.roles ? user.roles.includes('ROLE_ADMIN') : false;
    const hasAdminInAuthorities = user.authorities ? user.authorities.includes('ADMIN') : false;
    const hasRoleAdminInAuthorities = user.authorities ? user.authorities.includes('ROLE_ADMIN') : false;

    return user.role === 'ADMIN' ||
      hasAdminInRoles ||
      hasRoleAdminInRoles ||
      hasAdminInAuthorities ||
      hasRoleAdminInAuthorities;
  }

  // Méthode pour récupérer les rôles de l'utilisateur
  getUserRoles(): string[] {
    const user = this.getCurrentUser();
    if (!user) return [];

    return user.roles || user.authorities || [];
  }

  // Méthode pour vérifier si l'utilisateur a un rôle spécifique
  hasRole(role: string): boolean {
    const roles = this.getUserRoles();
    return roles.includes(role) || roles.includes(`ROLE_${role}`);
  }

  // Méthode pour extraire les informations du token JWT
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
