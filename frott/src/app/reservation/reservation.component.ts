import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService, User } from '../services/auth.service';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface Salle {
  id: number;
  nom: string;
  capacite: number;
  prix: number;
  imagePath?: string;
  status?: 'DISPONIBLE' | 'INDISPONIBLE' | 'MAINTENANCE';
}

export interface Creneau {
  id: number | null;
  debut: string;
  fin: string;
  salle: Salle;
}

export interface Reservation {
  id?: number;
  nombrePersonnes: number;
  clientName: string;
  clientEmail: string;
  salle: { id: number };
  creneau: Creneau;
  dateReservation: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  paiementStatus: 'EN_ATTENTE' | 'PAYEE' | 'ANNULE';
}

@Component({
  selector: 'app-reservation',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reservation.component.html',
  styleUrls: ['./reservation.component.css'],
  animations: [
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
export class ReservationComponent implements OnInit {
  salles: Salle[] = [];
  creneaux: Creneau[] = [];
  selectedSalle: Salle | null = null;
  selectedCreneau: number | null = null;
  creneauPersonnalise: { debut: string; fin: string } = { debut: '', fin: '' };
  useCustomCreneau: boolean = false;
  nombrePersonnes: number = 1;
  isLoading: boolean = false;
  showErrorMessage: boolean = false;
  showConfirmation: boolean = false;
  errorText: string = '';
  currentUser: User | null = null;
  private apiUrl = 'http://localhost:9090'; // Corrected to match no context path

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private datePipe: DatePipe
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    if (!this.authService.isAuthenticated() || !this.currentUser) {
      this.showError('Vous devez être connecté pour effectuer une réservation.');
      setTimeout(() => this.router.navigate(['/login']), 3000);
      return;
    }
    this.loadSalles();
  }

  loadSalles(): void {
    this.isLoading = true;
    const headers = this.getAuthHeaders();
    console.log('Fetching salles from:', `${this.apiUrl}/salles`); // Debug
    this.http.get<Salle[]>(`${this.apiUrl}/salles`, { headers }).pipe(
      catchError(err => this.handleHttpError(err, 'Erreur lors du chargement des salles'))
    ).subscribe({
      next: (data) => {
        this.salles = data.filter(s => s.status === 'DISPONIBLE');
        this.isLoading = false;
      }
    });
  }

  loadCreneauxForSalle(): void {
    if (!this.selectedSalle) {
      this.creneaux = [];
      return;
    }
    this.isLoading = true;
    const headers = this.getAuthHeaders();
    console.log('Fetching creneaux from:', `${this.apiUrl}/creneaux/disponibles`); // Debug
    this.http.get<Creneau[]>(`${this.apiUrl}/creneaux/disponibles`, { headers }).pipe(
      catchError(err => this.handleHttpError(err, 'Erreur lors du chargement des créneaux'))
    ).subscribe({
      next: (data) => {
        this.creneaux = data
          .filter(c => c.salle.id === this.selectedSalle!.id);
        this.isLoading = false;
      }
    });
  }

  toggleCreneauMode(useCustom: boolean): void {
    this.useCustomCreneau = useCustom;
    this.selectedCreneau = null;
    this.creneauPersonnalise = { debut: '', fin: '' };
    this.showErrorMessage = false;
  }

  onSalleChange(): void {
    this.selectedCreneau = null;
    this.creneauPersonnalise = { debut: '', fin: '' };
    this.nombrePersonnes = 1;
    this.showErrorMessage = false;
    this.showConfirmation = false;
    if (this.selectedSalle) {
      if (this.nombrePersonnes > this.selectedSalle.capacite) {
        this.nombrePersonnes = this.selectedSalle.capacite;
        this.showError(`Le nombre de personnes a été ajusté à la capacité maximale de la salle (${this.selectedSalle.capacite}).`);
      }
      this.loadCreneauxForSalle();
    } else {
      this.creneaux = [];
    }
  }

  onNombrePersonnesChange(): void {
    if (this.selectedSalle && this.nombrePersonnes > this.selectedSalle.capacite) {
      this.showError(`La capacité maximale de cette salle est de ${this.selectedSalle.capacite} personnes.`);
    } else {
      this.showErrorMessage = false;
    }
  }

  onCreneauChange(): void {
    if (this.useCustomCreneau && this.creneauPersonnalise.debut && this.creneauPersonnalise.fin) {
      const debut = new Date(this.creneauPersonnalise.debut);
      const fin = new Date(this.creneauPersonnalise.fin);
      if (fin <= debut) {
        this.showError('La date de fin doit être postérieure à la date de début.');
      } else {
        this.showErrorMessage = false;
      }
    } else if (!this.useCustomCreneau && this.selectedCreneau) {
      const creneau = this.creneaux.find(c => c.id === this.selectedCreneau);
      if (creneau) {
        const debut = new Date(creneau.debut);
        const fin = new Date(creneau.fin);
        if (fin <= debut) {
          this.showError('L’heure de fin doit être après l’heure de début.');
        } else {
          this.showErrorMessage = false;
        }
      }
    }
  }

  getSelectedSalleName(): string {
    return this.selectedSalle?.nom || '';
  }

  getSelectedSalleCapacite(): number {
    return this.selectedSalle?.capacite || 0;
  }

  getSelectedSallePrix(): number {
    return this.selectedSalle?.prix || 0;
  }

  getSelectedCreneau(): Creneau | null {
    return this.selectedCreneau ? this.creneaux.find(c => c.id === this.selectedCreneau) || null : null;
  }

  getTodayDate(): string {
    return new Date().toISOString().slice(0, 10);
  }

  formatDisplayDate(dateString: string | undefined): string {
    if (!dateString) return '';
    try {
      return this.datePipe.transform(new Date(dateString), 'dd/MM/yyyy HH:mm') || '';
    } catch (error) {
      return 'Erreur de format';
    }
  }

  canReserve(): boolean {
    if (!this.selectedSalle || this.nombrePersonnes < 1 || this.nombrePersonnes > this.getSelectedSalleCapacite()) {
      return false;
    }
    if (this.useCustomCreneau) {
      if (!this.creneauPersonnalise.debut || !this.creneauPersonnalise.fin) return false;
      const debut = new Date(this.creneauPersonnalise.debut);
      const fin = new Date(this.creneauPersonnalise.fin);
      return debut < fin && !this.showErrorMessage;
    }
    return !!this.selectedCreneau && !this.showErrorMessage;
  }

  reserver(): void {
    if (!this.canReserve() || !this.currentUser) {
      this.showError('Veuillez sélectionner une salle, un créneau valide et un nombre de personnes.');
      return;
    }

    this.isLoading = true;
    const headers = this.getAuthHeaders();
    if (!headers.get('Authorization')) {
      this.showError('Session expirée. Veuillez vous reconnecter.');
      this.authService.logout();
      setTimeout(() => this.router.navigate(['/login']), 3000);
      this.isLoading = false;
      return;
    }

    // Construct creneau payload
    let creneauPayload: Creneau = { id: null, debut: '', fin: '', salle: { id: 0, nom: '', capacite: 0, prix: 0 } }; 
    if (this.useCustomCreneau) {
      creneauPayload.debut = new Date(this.creneauPersonnalise.debut).toISOString();
      creneauPayload.fin = new Date(this.creneauPersonnalise.fin).toISOString();
      creneauPayload.salle = this.selectedSalle!;
      creneauPayload.id = null; // For custom, id null to trigger creation in backend
    } else {
      const selected = this.getSelectedCreneau();
      if (selected) {
        creneauPayload = selected;
      }
    }

    // Construct reservation payload
    const reservationPayload: Reservation = {
      nombrePersonnes: this.nombrePersonnes,
      clientName: this.currentUser!.username,
      clientEmail: this.currentUser!.email.toLowerCase(),
      salle: { id: this.selectedSalle!.id },
      creneau: creneauPayload,
      dateReservation: new Date().toISOString(),
      status: 'PENDING',
      paiementStatus: 'EN_ATTENTE'
    };

    console.log('Sending to URL:', `${this.apiUrl}/reservations`); // Debug
    console.log('Reservation payload:', JSON.stringify(reservationPayload, null, 2)); // Debug

    this.http.post<Reservation>(`${this.apiUrl}/reservations`, reservationPayload, { headers }).pipe(
      catchError(err => this.handleHttpError(err, 'Erreur lors de la création de la réservation'))
    ).subscribe({
      next: (response) => {
        console.log('Reservation response:', response); // Debug
        this.isLoading = false;
        this.showConfirmation = true;
        this.showErrorMessage = false;
        alert('Réservation créée avec succès. Un email de confirmation sera envoyé après validation.');
        this.resetForm();
      }
    });
  }

  private getAuthHeaders(): HttpHeaders {
    const headers = this.authService.getAuthHeaders();
    console.log('Auth Headers:', headers.get('Authorization')); // Debug
    return headers;
  }

  private handleHttpError(err: any, defaultMessage: string): Observable<never> {
    this.isLoading = false;
    let errorMessage = defaultMessage + ': ';
    if (err.status === 401 || err.status === 403) {
      errorMessage += 'Session expirée ou accès non autorisé. Veuillez vous reconnecter.';
      this.authService.logout();
      setTimeout(() => this.router.navigate(['/login']), 3000);
    } else if (err.status === 404) {
      errorMessage += 'Ressource non trouvée. Vérifiez si le serveur est en cours d\'exécution sur le port 9090.';
    } else if (err.status === 400 || err.status === 409) {
      errorMessage += err.error?.message || 'Données invalides.';
    } else {
      errorMessage += 'Vérifiez la connexion au serveur.';
    }
    console.error('HTTP Error:', err);
    this.showError(errorMessage);
    return throwError(() => new Error(errorMessage));
  }

  resetForm(): void {
    this.selectedSalle = null;
    this.selectedCreneau = null;
    this.creneauPersonnalise = { debut: '', fin: '' };
    this.nombrePersonnes = 1;
    this.useCustomCreneau = false;
    this.creneaux = [];
    this.showConfirmation = false;
    this.showErrorMessage = false;
    this.isLoading = false;
  }

  private showError(message: string): void {
    this.showErrorMessage = true;
    this.errorText = message;
  }
}