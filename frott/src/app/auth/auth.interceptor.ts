import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService, private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('authToken'); // Match key used in LoginComponent
    let authReq = req;

    // Skip adding Authorization header for public endpoints
    if (token && !req.url.includes('/auth/') && !req.url.includes('/salles') && !req.url.includes('/creneaux/disponibles')) {
      authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
    }

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('HTTP Error:', {
          status: error.status,
          statusText: error.statusText,
          url: error.url,
          message: error.message,
          error: error.error
        });

        if (error.status === 401) {
          alert('Session expirée, veuillez vous reconnecter.');
          this.authService.logout();
          this.router.navigate(['/login']);
        } else if (error.status === 403) {
          alert('Accès refusé : vous n’avez pas la permission pour cette action.');
          // Redirect to login for admin dashboard issues, as 403 likely indicates missing ROLE_ADMIN
          this.router.navigate(['/login']);
        } else {
          alert(`Erreur ${error.status}: ${error.message}`);
        }
        return throwError(() => error);
      })
    );
  }
}
