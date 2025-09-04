import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService, User } from '../services/auth.service';

export interface Salle {
  id: number;
  nom: string;
  capacite: number;
  prix: number;
  imagePath?: string;
  status?: 'DISPONIBLE' | 'INDISPONIBLE' | 'MAINTENANCE';
}

export interface Creneau {
  id: number;
  debut: string; // ISO format
  fin: string;   // ISO format
  salle: Salle;
  personnalise?: string; // Fixed syntax: semicolon removed, proper object structure
}

export interface Reservation {
  id?: number;
  nombrePersonnes: number;
  clientName: string;
  clientEmail: string;
  salle: Salle;
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
  styleUrls: ['./reservation.component.css']
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
  private apiUrl = 'http://localhost:9090';

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
    const headers = this.authService.getAuthHeaders();
    if (!headers || !headers.get('Authorization')) {
      this.showError('Session expirée. Veuillez vous reconnecter.');
      this.authService.logout();
      setTimeout(() => this.router.navigate(['/login']), 3000);
      this.isLoading = false;
      return;
    }

    this.http.get<Salle[]>(`${this.apiUrl}/salles`, { headers }).subscribe({
      next: (data) => {
        this.salles = data.filter(s => s.status === 'DISPONIBLE');
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement salles:', err);
        this.isLoading = false;
        if (err.status === 401 || err.status === 403) {
          this.showError('Session expirée. Veuillez vous reconnecter.');
          this.authService.logout();
          setTimeout(() => this.router.navigate(['/login']), 3000);
        } else {
          this.showError('Erreur lors du chargement des salles: ' + (err.error?.message || 'Vérifiez la connexion au serveur.'));
        }
      }
    });
  }

  loadCreneauxForSalle(): void {
    if (!this.selectedSalle) {
      this.creneaux = [];
      return;
    }
    this.isLoading = true;
    const headers = this.authService.getAuthHeaders();
    if (!headers || !headers.get('Authorization')) {
      this.showError('Session expirée. Veuillez vous reconnecter.');
      this.authService.logout();
      setTimeout(() => this.router.navigate(['/login']), 3000);
      this.isLoading = false;
      return;
    }

    this.http.get<Creneau[]>(`${this.apiUrl}/creneaux/disponibles`, { headers }).subscribe({
      next: (data) => {
        this.creneaux = data
          .filter(c => c.salle.id === this.selectedSalle!.id)
          .map(c => ({ ...c, personnalise: c.personnalise || '' }));
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement créneaux:', err);
        this.isLoading = false;
        this.showError('Erreur lors du chargement des créneaux disponibles.');
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
    const headers = this.authService.getAuthHeaders();
    if (!headers || !headers.get('Authorization')) {
      this.showError('Session expirée. Veuillez vous reconnecter.');
      this.authService.logout();
      setTimeout(() => this.router.navigate(['/login']), 3000);
      this.isLoading = false;
      return;
    }

    const reservationPayload = {
      nombrePersonnes: this.nombrePersonnes,
      salle: { id: this.selectedSalle!.id },
      clientName: this.currentUser.username,
      clientEmail: this.currentUser.email
    };

    if (this.useCustomCreneau) {
      const creneauPayload = {
        debut: new Date(this.creneauPersonnalise.debut).toISOString(),
        fin: new Date(this.creneauPersonnalise.fin).toISOString(),
        salle: { id: this.selectedSalle!.id },
        personnalise: ''
      };

      this.http.post<Creneau>(`${this.apiUrl}/creneaux`, creneauPayload, { headers }).subscribe({
        next: (creneau) => {
          this.createReservation({ ...reservationPayload, creneau: { id: creneau.id } }, headers);
        },
        error: (err) => {
          this.isLoading = false;
          this.showError('Erreur lors de la création du créneau: ' + (err.error?.message || 'Vérifiez les données saisies.'));
        }
      });
    } else {
      this.createReservation({ ...reservationPayload, creneau: { id: this.selectedCreneau! } }, headers);
    }
  }

  private createReservation(payload: any, headers: HttpHeaders): void {
    this.http.post<Reservation>(`${this.apiUrl}/reservations`, payload, { headers }).subscribe({
      next: () => {
        this.isLoading = false;
        this.showConfirmation = true;
        this.showErrorMessage = false;
        alert('Réservation créée avec succès.');
        this.resetForm();
      },
      error: (err) => {
        this.isLoading = false;
        this.showError('Erreur lors de la création de la réservation: ' + (err.error?.message || 'Vérifiez les données saisies.'));
      }
    });
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
