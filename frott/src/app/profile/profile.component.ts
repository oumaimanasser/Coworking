import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { Observable, throwError } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';

interface Creneau {
  id: number;
  debut: string;
  fin: string;
  salle?: Salle;
}

interface Salle {
  id: number;
  nom: string;
}

interface Reservation {
  id: number;
  nomSalle?: string;
  salle?: Salle;
  creneau?: Creneau;
  date: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  paiementStatus: 'EN_ATTENTE' | 'PAYEE' | 'ANNULE';
  prix: number;
}

interface ReservationResponse {
  reservations: Reservation[];
  total: number;
  paid: number;
  unpaid: number;
}

interface Profile {
  id?: number;
  username: string;
  email: string;
  password?: string;
  roles?: { id: number; name: string }[];
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  animations: [
    trigger('fadeInUp', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(30px)' }),
        animate('0.6s cubic-bezier(0.4, 0, 0.2, 1)', style({ opacity: 1, transform: 'translateY(0)' }))
      ])
    ]),
    trigger('fadeInOut', [
      state('void', style({ opacity: 0, transform: 'translateY(10px)' })),
      transition(':enter', [
        animate('400ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('300ms ease-in', style({ opacity: 0, transform: 'translateY(10px)' }))
      ])
    ])
  ]
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup;
  profile: Profile | null = null;
  reservations: Reservation[] = [];
  reservationResponse: ReservationResponse | null = null;
  errorMessage: string = '';
  successMessage: string = '';
  isSubmitting: boolean = false;
  showPassword: boolean = false;
  private apiUrl = 'http://localhost:9090/api/users';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private datePipe: DatePipe
  ) {
    this.profileForm = this.fb.group({
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.minLength(8)]]
    });
  }

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.showError('Vous devez être connecté pour accéder à votre profil.');
      setTimeout(() => this.router.navigate(['/login']), 3000);
      return;
    }
    this.loadProfile();
    this.loadReservations();
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  loadProfile(): void {
    this.isSubmitting = true;
    this.http.get<Profile>(`${this.apiUrl}/profile`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(err => this.handleHttpError(err, 'Erreur lors du chargement du profil')),
        finalize(() => this.isSubmitting = false)
      )
      .subscribe({
        next: (data) => {
          this.profile = data;
          this.profileForm.patchValue({
            username: data.username,
            email: data.email,
            password: ''
          });
        }
      });
  }

  updateProfile(): void {
    if (this.profileForm.valid && !this.isSubmitting) {
      this.isSubmitting = true;
      this.errorMessage = '';
      this.successMessage = '';
      const updates: Partial<Profile> = {};
      if (this.profileForm.get('username')?.value !== this.profile?.username) {
        updates.username = this.profileForm.get('username')?.value;
      }
      if (this.profileForm.get('email')?.value !== this.profile?.email) {
        updates.email = this.profileForm.get('email')?.value;
      }
      if (this.profileForm.get('password')?.value) {
        updates.password = this.profileForm.get('password')?.value;
      }
      this.http.put(`${this.apiUrl}/profile`, updates, { headers: this.getAuthHeaders() })
        .pipe(
          catchError(err => this.handleHttpError(err, 'Erreur lors de la mise à jour du profil')),
          finalize(() => this.isSubmitting = false)
        )
        .subscribe({
          next: (response: any) => {
            this.successMessage = response.message || 'Profil mis à jour avec succès';
            if (response.user) {
              const user = JSON.parse(localStorage.getItem('user') || '{}');
              user.username = response.user.username;
              user.email = response.user.email;
              localStorage.setItem('user', JSON.stringify(user));
              this.profile = response.user;
            }
            setTimeout(() => this.successMessage = '', 5000);
          }
        });
    } else {
      this.showError('Veuillez remplir correctement tous les champs requis.');
    }
  }

 loadReservations(): void {
  this.isSubmitting = true;
  this.http.get<ReservationResponse>(`${this.apiUrl}/reservations`, { headers: this.getAuthHeaders() })
    .pipe(
      catchError(err => this.handleHttpError(err, 'Erreur lors du chargement des réservations')),
      finalize(() => this.isSubmitting = false)
    )
    .subscribe({
      next: (data) => {
        console.log('Reservations Data:', data); // Ajout pour débogage
        this.reservationResponse = data;
        this.reservations = data.reservations.map(res => ({
          id: res.id,
          nomSalle: res.salle?.nom || res.nomSalle || 'Salle inconnue',
          creneau: res.creneau || { id: 0, debut: '', fin: '', salle: undefined },
          date: res.date,
          status: res.status || 'PENDING',
          paiementStatus: res.paiementStatus || 'EN_ATTENTE',
          prix: res.prix || 0
        }));
      }
    });
}

  deleteReservation(id: number): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette réservation ?')) return;
    this.isSubmitting = true;
    this.http.delete(`${this.apiUrl}/reservations/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(err => this.handleHttpError(err, 'Erreur lors de la suppression de la réservation')),
        finalize(() => this.isSubmitting = false)
      )
      .subscribe({
        next: (response: any) => {
          this.successMessage = response.message || 'Réservation supprimée avec succès';
          this.loadReservations();
          setTimeout(() => this.successMessage = '', 5000);
        }
      });
  }

  private getAuthHeaders(): HttpHeaders {
    const headers = this.authService.getAuthHeaders();
    console.log('Auth Headers:', headers.get('Authorization'));
    return headers;
  }

  private handleHttpError(err: any, defaultMessage: string): Observable<never> {
    this.isSubmitting = false;
    let errorMessage = defaultMessage + ': ';
    if (err.status === 401 || err.status === 403) {
      errorMessage += 'Session expirée ou accès non autorisé. Veuillez vous reconnecter.';
      this.authService.logout();
      setTimeout(() => this.router.navigate(['/login']), 3000);
    } else if (err.status === 400 || err.status === 409) {
      errorMessage += err.error?.message || 'Données invalides.';
    } else if (err.status === 404) {
      errorMessage += 'Ressource non trouvée.';
    } else {
      errorMessage += 'Vérifiez la connexion au serveur.';
    }
    console.error('HTTP Error:', err);
    this.showError(errorMessage);
    return throwError(() => new Error(errorMessage));
  }

  private showError(message: string): void {
    this.errorMessage = message;
    setTimeout(() => this.errorMessage = '', 5000);
  }

  formatDisplayDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return 'N/A';
      return this.datePipe.transform(date, 'dd/MM/yyyy HH:mm') || 'N/A';
    } catch (error) {
      return 'Erreur de format';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'CONFIRMED':
        return 'status-confirmed';
      case 'PENDING':
        return 'status-pending';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return '';
    }
  }

  getPaiementClass(status: string): string {
    switch (status) {
      case 'PAYEE':
        return 'paiement-payee';
      case 'EN_ATTENTE':
        return 'paiement-pending';
      case 'ANNULE':
        return 'paiement-cancelled';
      default:
        return '';
    }
  }
}