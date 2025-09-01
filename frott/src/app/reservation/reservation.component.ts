import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReservationService } from '../services/reservation.service';
import { AuthService, User } from '../services/auth.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';

export interface Salle {
  id: number;
  nom: string;
  capacite: number;
  prix: number;
  imagePath?: string;
  status?: string;
}

interface Creneau {
  id?: number;
  debut: string;
  fin: string;
  salle: Salle;
  personnalise?: boolean;
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
  selectedSalle: number | null = null;
  selectedCreneau: Creneau | null = null;
  nombrePersonnes: number = 1;

  creneauPersonnalise: any = {
    debut: '',
    fin: ''
  };

  showPersonnalise = true;
  isLoading = false;
  showErrorMessage = false;
  showConfirmation = false;
  errorText = '';

  currentUser: User | null = null;
  private apiUrl = 'http://localhost:9090';

  constructor(
    private reservationService: ReservationService,
    private authService: AuthService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();

    if (!this.authService.isAuthenticated() || !this.currentUser) {
      this.showErrorMessage = true;
      this.errorText = "Vous devez être connecté pour effectuer une réservation.";
      setTimeout(() => {
        this.router.navigate(['/login']);
      }, 3000);
      return;
    }

    this.loadSalles();
  }

  loadSalles(): void {
    this.isLoading = true;

    // Plus besoin d'en-têtes d'authentification pour GET /salles
    // Car votre SecurityConfig permet l'accès public à GET /salles/**
    this.http.get<Salle[]>(`${this.apiUrl}/salles`)
      .subscribe({
        next: (data) => {
          this.salles = data;
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Erreur chargement salles:', err);
          this.isLoading = false;
          this.showErrorMessage = true;
          this.errorText = 'Erreur lors du chargement des salles. Veuillez réessayer.';
        }
      });
  }

  onSalleChange(): void {
    this.selectedCreneau = null;
    this.creneauPersonnalise = { debut: '', fin: '' };

    const selectedSalle = this.salles.find(s => s.id === this.selectedSalle);
    if (selectedSalle && this.nombrePersonnes > selectedSalle.capacite) {
      this.nombrePersonnes = selectedSalle.capacite;
      this.showErrorMessage = true;
      this.errorText = `Le nombre de personnes a été ajusté à la capacité maximale de la salle (${selectedSalle.capacite}).`;
    }
  }

  onNombrePersonnesChange(): void {
    if (this.selectedSalle) {
      const selectedSalle = this.salles.find(s => s.id === this.selectedSalle);
      if (selectedSalle && this.nombrePersonnes > selectedSalle.capacite) {
        this.showErrorMessage = true;
        this.errorText = `La capacité maximale de cette salle est de ${selectedSalle.capacite} personnes.`;
      } else {
        this.showErrorMessage = false;
      }
    }
  }

  onCreneauChange(): void {
    if (this.creneauPersonnalise.debut && this.creneauPersonnalise.fin) {
      const debut = new Date(this.creneauPersonnalise.debut);
      const fin = new Date(this.creneauPersonnalise.fin);

      if (fin <= debut) {
        this.showErrorMessage = true;
        this.errorText = "L'heure de fin doit être après l'heure de début.";
      } else {
        this.showErrorMessage = false;
      }
    }
  }

  getSelectedSalleName(): string {
    const salle = this.salles.find(s => s.id === this.selectedSalle);
    return salle ? salle.nom : '';
  }

  getSelectedSalleCapacite(): number {
    const salle = this.salles.find(s => s.id === this.selectedSalle);
    return salle ? salle.capacite : 0;
  }

  getSelectedSallePrix(): number {
    const salle = this.salles.find(s => s.id === this.selectedSalle);
    return salle ? salle.prix : 0;
  }

  canReserve(): boolean {
    return !!this.selectedSalle &&
      !!this.creneauPersonnalise.debut &&
      !!this.creneauPersonnalise.fin &&
      this.nombrePersonnes > 0 &&
      this.nombrePersonnes <= this.getSelectedSalleCapacite() &&
      this.authService.isAuthenticated() &&
      !this.showErrorMessage;
  }

  reserver(): void {
    if (!this.canReserve()) {
      this.showErrorMessage = true;
      this.errorText = "Veuillez compléter tous les champs obligatoires correctement.";
      return;
    }

    this.isLoading = true;
    this.showErrorMessage = false;

    // Structure correcte attendue par le backend Spring
    const request = {
      salle: {
        id: this.selectedSalle
      },
      creneau: {
        id: null, // ou laissez-le undefined si non requis
        debut: this.creneauPersonnalise.debut,
        fin: this.creneauPersonnalise.fin
      },
      nombrePersonnes: this.nombrePersonnes,
      clientName: this.currentUser?.username, // Ajoutez ces champs si requis
      clientEmail: this.currentUser?.email,   // par le backend
      status: 'PENDING'
    };

    const token = this.authService.getToken();
    if (!token) {
      this.handleAuthError();
      return;
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    console.log('Requête envoyée:', request); // Debug

    this.http.post<any>(`${this.apiUrl}/reservations`, request, { headers })
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.showConfirmation = true;
          this.showErrorMessage = false;
          this.selectedCreneau = {
            id: response.creneau?.id,
            debut: response.creneau?.debut,
            fin: response.creneau?.fin,
            salle: response.salle,
            personnalise: true
          };
        },
        error: (err) => {
          this.isLoading = false;
          this.showErrorMessage = true;

          console.error('Détails de l\'erreur:', err);

          if (err.status === 400) {
            this.errorText = err.error?.message || 'Données invalides. Vérifiez les informations saisies.';
          } else if (err.status === 403) {
            this.errorText = "Accès refusé. Contactez l'administrateur.";
          } else if (err.status === 401) {
            this.handleAuthError();
          } else if (err.error && err.error.message) {
            this.errorText = err.error.message;
          } else {
            this.errorText = 'Erreur lors de la création de la réservation';
          }

          console.error('Erreur réservation:', err);
        }
      });
  }

  resetForm(): void {
    this.selectedSalle = null;
    this.selectedCreneau = null;
    this.nombrePersonnes = 1;
    this.showConfirmation = false;
    this.showErrorMessage = false;
    this.creneauPersonnalise = { debut: '', fin: '' };
  }

  getTodayDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  formatDisplayDate(dateString: string | undefined): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');

    return `${day}/${month}/${year} à ${hours}:${minutes}`;
  }

  private handleAuthError(): void {
    this.isLoading = false;
    this.showErrorMessage = true;
    this.errorText = "Session expirée. Veuillez vous reconnecter.";
    this.authService.logout();
    setTimeout(() => {
      this.router.navigate(['/login']);
    }, 3000);
  }
}
