import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReservationService } from '../services/reservation.service';
import { AuthService, User } from '../services/auth.service';
import { HttpClientModule } from '@angular/common/http';

interface Salle {
  id: number;
  nom: string;
  capacite: number;
}

interface Creneau {
  id: number;
  debut: string;
  fin: string;
  salle: Salle;
}

@Component({
  selector: 'app-reservation',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './reservation.component.html',
  styleUrls: ['./reservation.component.css']
})
export class ReservationComponent implements OnInit {

  salles: Salle[] = [];
  creneauxDisponibles: Creneau[] = [];

  selectedSalle: number | null = null;
  selectedCreneau: number | null = null;

  isLoading: boolean = false;
  showErrorMessage: boolean = false;
  showConfirmation: boolean = false;
  errorText: string = '';

  currentUser: User | null = null;

  constructor(
    private reservationService: ReservationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadSalles();
  }

  // Charger toutes les salles depuis backend (exemple)
  loadSalles(): void {
    this.isLoading = true;
    // Exemple GET /salles
    fetch('http://localhost:9090/salles')
      .then(res => res.json())
      .then((data: Salle[]) => {
        this.salles = data;
        this.isLoading = false;
      })
      .catch(err => {
        console.error('Erreur chargement salles:', err);
        this.isLoading = false;
      });
  }

  // Quand la salle change, charger les créneaux disponibles
  onSalleChange(): void {
    if (!this.selectedSalle) {
      this.creneauxDisponibles = [];
      return;
    }

    this.isLoading = true;
    fetch(`http://localhost:9090/creneaux/disponibles?salleId=${this.selectedSalle}`)
      .then(res => res.json())
      .then((data: Creneau[]) => {
        this.creneauxDisponibles = data;
        this.isLoading = false;
      })
      .catch(err => {
        console.error('Erreur chargement créneaux:', err);
        this.isLoading = false;
      });
  }

  selectCreneau(creneauId: number): void {
    this.selectedCreneau = creneauId;
  }

  formatTime(dateTime: string): string {
    const date = new Date(dateTime);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  canReserve(): boolean {
    return !!this.selectedSalle && !!this.selectedCreneau;
  }

  reserver(): void {
    if (!this.canReserve()) {
      this.showErrorMessage = true;
      this.errorText = "Veuillez sélectionner une salle et un créneau.";
      return;
    }

    this.isLoading = true;
    this.showErrorMessage = false;
    this.showConfirmation = false;

    this.reservationService.createReservation(this.selectedSalle!, this.selectedCreneau!)
      .subscribe({
        next: (res) => {
          this.isLoading = false;
          this.showConfirmation = true;
          console.log('Réservation créée:', res);
        },
        error: (err) => {
          this.isLoading = false;
          this.showErrorMessage = true;
          this.errorText = err.error?.message || 'Erreur lors de la réservation';
          console.error('Erreur réservation:', err);
        }
      });
  }
}
