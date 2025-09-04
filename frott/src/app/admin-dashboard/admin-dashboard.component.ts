import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

export interface Salle {
  id?: number;
  nom: string;
  capacite: number;
  prix: number;
  status?: 'DISPONIBLE' | 'INDISPONIBLE' | 'MAINTENANCE';
  imagePath?: string;
}

export interface Creneau {
  id?: number;
  debut: string; // ISO format (e.g., 2025-09-03T09:00:00)
  fin: string;   // ISO format
  salle: Salle;
  personnalise?: string;
}

export interface Reservation {
  id?: number;
  nombrePersonnes: number;
  clientName: string;
  clientEmail: string;
  salle: Salle;
  creneau: Creneau;
  dateReservation: string; // ISO format
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  paiementStatus: 'EN_ATTENTE' | 'PAYEE' | 'ANNULE';
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  currentPage: 'login' | 'dashboard' | 'reservations' | 'creneaux' | 'salles' | 'paiements' = 'login';
  loginForm: FormGroup;
  newCreneauForm: FormGroup;
  editCreneauForm: FormGroup;
  newSalleForm: FormGroup;
  editSalleForm: FormGroup;
  creneaux: Creneau[] = [];
  reservations: Reservation[] = [];
  salles: Salle[] = [];
  reservationsAvecPaiementEnAttente: Reservation[] = [];
  reservationsPayees: Reservation[] = [];
  editingCreneau: Creneau | null = null;
  editingSalle: Salle | null = null;
  selectedFile: File | null = null;
  private apiUrl = 'http://localhost:9090'; // Updated to correct port

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private datePipe: DatePipe
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });

    this.newCreneauForm = this.fb.group({
      debut: ['', Validators.required],
      fin: ['', Validators.required],
      salleId: ['', Validators.required]
    });

    this.editCreneauForm = this.fb.group({
      debut: ['', Validators.required],
      fin: ['', Validators.required],
      salleId: ['', Validators.required]
    });

    this.newSalleForm = this.fb.group({
      nom: ['', Validators.required],
      capacite: [1, [Validators.required, Validators.min(1)]],
      prix: [0, [Validators.required, Validators.min(0)]],
      status: ['DISPONIBLE', Validators.required]
    });

    this.editSalleForm = this.fb.group({
      id: [''],
      nom: ['', Validators.required],
      capacite: [1, [Validators.required, Validators.min(1)]],
      prix: [0, [Validators.required, Validators.min(0)]],
      status: ['DISPONIBLE', Validators.required]
    });
  }

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    if (!this.authService.isAdmin()) {
      this.router.navigate(['/home'], {
        queryParams: { message: 'Accès réservé aux administrateurs' }
      });
      return;
    }

    this.currentPage = 'dashboard';
    this.loadAllData();
  }

  login(): void {
    if (!this.loginForm.valid) {
      alert('Veuillez remplir tous les champs correctement.');
      return;
    }
    const { email, password } = this.loginForm.value;
    this.authService.login({ email, password }).subscribe({
      next: (res: any) => {
        localStorage.setItem('token', res.token);
        this.currentPage = 'dashboard';
        this.loadAllData();
      },
      error: (err) => {
        console.error('Erreur login:', err);
        alert('Email ou mot de passe incorrect.');
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.currentPage = 'login';
    this.router.navigate(['/login']);
  }

  showPage(page: 'dashboard' | 'reservations' | 'creneaux' | 'salles' | 'paiements'): void {
    this.currentPage = page;
    this.cancelEdit();
    if (page === 'paiements') {
      this.loadReservationsAvecPaiementEnAttente();
      this.loadReservationsPayees();
    }
  }

  private getAuthHeaders(): HttpHeaders | null {
    const token = localStorage.getItem('token');
    if (!token) {
      alert('Session expirée. Veuillez vous reconnecter.');
      this.logout();
      return null;
    }
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  loadAllData(): void {
    this.loadSalles();
  }

  private loadSalles(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Salle[]>(`${this.apiUrl}/salles`, { headers }).subscribe({
      next: (data: Salle[]) => {
        this.salles = data;
        console.log('Salles chargées:', data);
        this.loadCreneaux();
        this.loadReservations();
      },
      error: (err) => {
        console.error('Erreur chargement salles:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors du chargement des salles.');
        }
      }
    });
  }

  private loadCreneaux(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Creneau[]>(`${this.apiUrl}/creneaux`, { headers }).subscribe({
      next: (creneaux: Creneau[]) => {
        this.creneaux = creneaux.map(creneau => {
          const salleComplete = this.salles.find(s => s.id === creneau.salle.id);
          if (salleComplete) creneau.salle = salleComplete;
          creneau.personnalise = creneau.personnalise || ''; // Ensure personnalise is set
          return creneau;
        });
        console.log('Creneaux chargés:', this.creneaux);
      },
      error: (err) => {
        console.error('Erreur chargement créneaux:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors du chargement des créneaux.');
        }
      }
    });
  }

  private loadReservations(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Reservation[]>(`${this.apiUrl}/reservations`, { headers }).subscribe({
      next: (res) => {
        this.reservations = res;
        console.log('Réservations chargées:', res);
      },
      error: (err) => {
        console.error('Erreur chargement réservations:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors du chargement des réservations.');
        }
      }
    });
  }

  onFileSelected(event: any): void {
    this.selectedFile = event.target.files[0] as File;
  }

  getImageUrl(imagePath?: string): string {
    if (!imagePath) return '';
    const filename = imagePath.split('/').pop() || imagePath;
    return `${this.apiUrl}/salles/images/${filename}`;
  }

  onImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    console.warn('Image non trouvée:', imgElement.src);
    imgElement.style.display = 'none';
  }

  addSalle(): void {
    if (!this.newSalleForm.valid) {
      alert('Veuillez remplir tous les champs de la salle correctement.');
      return;
    }
    const headers = this.getAuthHeaders();
    if (!headers) return;

    const formData = new FormData();
    formData.append('nom', this.newSalleForm.get('nom')?.value);
    formData.append('capacite', String(this.newSalleForm.get('capacite')?.value));
    formData.append('prix', String(this.newSalleForm.get('prix')?.value));
    formData.append('status', this.newSalleForm.get('status')?.value);
    if (this.selectedFile) formData.append('image', this.selectedFile);

    this.http.post<Salle>(`${this.apiUrl}/salles`, formData, { headers }).subscribe({
      next: (res) => {
        this.salles.push(res);
        this.newSalleForm.reset({ status: 'DISPONIBLE' });
        this.selectedFile = null;
        this.loadSalles();
        alert('Salle ajoutée avec succès.');
      },
      error: (err) => {
        console.error('Erreur ajout salle:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors de l’ajout de la salle.');
        }
      }
    });
  }

  editSalle(salle: Salle): void {
    this.editingSalle = salle;
    this.editSalleForm.patchValue({
      id: salle.id,
      nom: salle.nom,
      capacite: salle.capacite,
      prix: salle.prix,
      status: salle.status || 'DISPONIBLE'
    });
    this.selectedFile = null;
  }

  updateSalle(): void {
    if (!this.editSalleForm.valid || !this.editingSalle?.id) {
      alert('Veuillez remplir tous les champs correctement.');
      return;
    }
    const headers = this.getAuthHeaders();
    if (!headers) return;

    const formData = new FormData();
    formData.append('nom', this.editSalleForm.get('nom')?.value);
    formData.append('capacite', String(this.newSalleForm.get('capacite')?.value));
    formData.append('prix', String(this.newSalleForm.get('prix')?.value));
    formData.append('status', this.editSalleForm.get('status')?.value);
    if (this.selectedFile) formData.append('image', this.selectedFile);

    this.http.put<Salle>(`${this.apiUrl}/salles/${this.editingSalle.id}`, formData, { headers }).subscribe({
      next: (res) => {
        const index = this.salles.findIndex(s => s.id === res.id);
        if (index !== -1) this.salles[index] = res;
        this.cancelEdit();
        this.loadSalles();
        alert('Salle modifiée avec succès.');
      },
      error: (err) => {
        console.error('Erreur modification salle:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors de la modification de la salle.');
        }
      }
    });
  }

  deleteSalle(id?: number): void {
    if (!id || !confirm('Êtes-vous sûr de vouloir supprimer cette salle ?')) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.delete(`${this.apiUrl}/salles/${id}`, { headers }).subscribe({
      next: () => {
        this.salles = this.salles.filter(s => s.id !== id);
        this.loadSalles();
        alert('Salle supprimée avec succès.');
      },
      error: (err) => {
        console.error('Erreur suppression salle:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors de la suppression de la salle.');
        }
      }
    });
  }
  addCreneau(): void {
    if (!this.newCreneauForm.valid) {
      alert('Veuillez remplir tous les champs du créneau correctement.');
      return;
    }
    const headers = this.getAuthHeaders();
    if (!headers) return;

    const data = this.newCreneauForm.value;
    const payload = {
      debut: new Date(data.debut).toISOString(),
      fin: new Date(data.fin).toISOString(),
      salle: { id: Number(data.salleId) },
      personnalise: '' // Include personnalise
    };

    this.http.post<Creneau>(`${this.apiUrl}/creneaux`, payload, { headers }).subscribe({
      next: (res: Creneau) => {
        const salleComplete = this.salles.find(s => s.id === res.salle.id);
        if (salleComplete) res.salle = salleComplete;
        this.creneaux.push(res);
        this.newCreneauForm.reset();
        alert('Créneau ajouté avec succès.');
      },
      error: (err) => {
        console.error('Erreur ajout créneau:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors de l’ajout du créneau: ' + (err.error?.message || 'Vérifiez les données saisies.'));
        }
      }
    });
  }

  updateCreneau(): void {
    if (!this.editCreneauForm.valid || !this.editingCreneau?.id) {
      alert('Veuillez remplir tous les champs correctement.');
      return;
    }
    const headers = this.getAuthHeaders();
    if (!headers) return;

    const data = this.editCreneauForm.value;
    const payload = {
      debut: new Date(data.debut).toISOString(),
      fin: new Date(data.fin).toISOString(),
      salle: { id: Number(data.salleId) },
      personnalise: this.editingCreneau.personnalise || '' // Preserve or set personnalise
    };

    this.http.put<Creneau>(`${this.apiUrl}/creneaux/${this.editingCreneau.id}`, payload, { headers }).subscribe({
      next: (res: Creneau) => {
        const salleComplete = this.salles.find(s => s.id === res.salle.id);
        if (salleComplete) res.salle = salleComplete;
        const index = this.creneaux.findIndex(c => c.id === res.id);
        if (index !== -1) this.creneaux[index] = res;
        this.cancelEdit();
        alert('Créneau modifié avec succès.');
      },
      error: (err) => {
        console.error('Erreur modification créneau:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors de la modification du créneau: ' + (err.error?.message || 'Vérifiez les données saisies.'));
        }
      }
    });
  }
  editCreneau(creneau: Creneau): void {
    this.editingCreneau = creneau;
    this.editCreneauForm.patchValue({
      debut: creneau.debut.slice(0, 16),
      fin: creneau.fin.slice(0, 16),
      salleId: creneau.salle?.id
    });
  }
  deleteCreneau(id?: number): void {
    if (!id || !confirm('Êtes-vous sûr de vouloir supprimer ce créneau ?')) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.delete(`${this.apiUrl}/creneaux/${id}`, { headers }).subscribe({
      next: () => {
        this.creneaux = this.creneaux.filter(c => c.id !== id);
        alert('Créneau supprimé avec succès.');
      },
      error: (err) => {
        console.error('Erreur suppression créneau:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors de la suppression du créneau: ' + (err.error?.message || 'Vérifiez les données saisies.'));
        }
      }
    });
  }

  annulerReservation(id?: number): void {
    if (!id || !confirm('Êtes-vous sûr de vouloir annuler cette réservation ?')) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.delete(`${this.apiUrl}/reservations/${id}`, { headers }).subscribe({
      next: () => {
        this.reservations = this.reservations.filter(r => r.id !== id);
        alert('Réservation annulée avec succès.');
      },
      error: (err) => {
        console.error('Erreur annulation réservation:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors de l’annulation de la réservation: ' + (err.error?.message || 'Vérifiez les données saisies.'));
        }
      }
    });
  }

  loadReservationsAvecPaiementEnAttente(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Reservation[]>(`${this.apiUrl}/reservations/paiements/en-attente`, { headers }).subscribe({
      next: (res) => {
        this.reservationsAvecPaiementEnAttente = res;
        console.log('Réservations en attente:', res);
      },
      error: (err) => {
        console.error('Erreur chargement réservations en attente:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors du chargement des réservations en attente.');
        }
      }
    });
  }

  loadReservationsPayees(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Reservation[]>(`${this.apiUrl}/reservations/paiements/payees`, { headers }).subscribe({
      next: (res) => {
        this.reservationsPayees = res;
        console.log('Réservations payées:', res);
      },
      error: (err) => {
        console.error('Erreur chargement réservations payées:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors du chargement des réservations payées.');
        }
      }
    });
  }

  confirmerPaiement(reservationId?: number): void {
    if (!reservationId || !confirm('Êtes-vous sûr de vouloir confirmer ce paiement ?')) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.post(`${this.apiUrl}/paiements/confirmer/${reservationId}`, {}, { headers }).subscribe({
      next: () => {
        alert('Paiement confirmé avec succès.');
        this.loadReservationsAvecPaiementEnAttente();
        this.loadReservationsPayees();
        this.loadReservations();
      },
      error: (err) => {
        console.error('Erreur confirmation paiement:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors de la confirmation du paiement: ' + (err.error?.message || 'Vérifiez les données saisies.'));
        }
      }
    });
  }

  getPaiementStatusClass(status: string): string {
    switch (status) {
      case 'PAYEE': return 'status-paye';
      case 'EN_ATTENTE': return 'status-en-attente';
      case 'ANNULE': return 'status-annule';
      default: return '';
    }
  }

  getPaiementStatusText(status: string): string {
    switch (status) {
      case 'PAYEE': return 'Payé';
      case 'EN_ATTENTE': return 'En attente';
      case 'ANNULE': return 'Annulé';
      default: return status;
    }
  }

  cancelEdit(): void {
    this.editingCreneau = null;
    this.editingSalle = null;
    this.editCreneauForm.reset();
    this.editSalleForm.reset();
    this.selectedFile = null;
  }

  formatDate(dateString?: string): string {
    if (!dateString) return 'Non défini';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return 'Date invalide';
      return this.datePipe.transform(date, 'dd/MM/yyyy HH:mm') || 'Format invalide';
    } catch (error) {
      return 'Erreur de format';
    }
  }

  getTotalReservations(): number { return this.reservations.length; }
  getTotalCreneaux(): number { return this.creneaux.length; }
  getTotalSalles(): number { return this.salles.length; }

  getRecentReservations(): Reservation[] {
    return [...this.reservations]
      .sort((a, b) => new Date(b.dateReservation).getTime() - new Date(a.dateReservation).getTime())
      .slice(0, 5);
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return '';
    switch (status) {
      case 'DISPONIBLE': return 'status-available';
      case 'INDISPONIBLE': return 'status-unavailable';
      case 'MAINTENANCE': return 'status-maintenance';
      default: return '';
    }
  }

  private handleAuthError(): void {
    alert('Session expirée. Veuillez vous reconnecter.');
    this.logout();
  }
  annulerPaiement(reservationId?: number): void {
    if (!reservationId || !confirm('Êtes-vous sûr de vouloir annuler ce paiement ?')) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.post(`${this.apiUrl}/paiements/annuler/${reservationId}`, {}, { headers }).subscribe({
      next: () => {
        alert('Paiement annulé avec succès.');
        this.loadReservationsAvecPaiementEnAttente();
        this.loadReservationsPayees();
        this.loadReservations();
      },
      error: (err) => {
        console.error('Erreur annulation paiement:', err);
        if (err.status === 401 || err.status === 403) {
          this.handleAuthError();
        } else {
          alert('Erreur lors de l’annulation du paiement: ' + (err.error?.message || 'Vérifiez les données saisies.'));
        }
      }
    });
  }
}
