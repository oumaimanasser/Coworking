// admin-dashboard.component.ts
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
  status: 'DISPONIBLE' | 'INDISPONIBLE' | 'MAINTENANCE';
  imagePath?: string;
}

export interface Creneau {
  id?: number;
  debut: string; // Format ISO
  fin: string;   // Format ISO
  salle: Salle;
}

export interface Reservation {
  id?: number;
  nombrePersonnes: number;
  clientName: string;
  clientEmail: string;
  salle: Salle;
  creneau: Creneau;
  dateReservation: string; // Format ISO
  status: string;
  paiementStatus: 'PAYE' | 'EN_ATTENTE' | 'ANNULE';
  datePaiement?: string;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {

  // Pages (inclut la page "paiements")
  currentPage: 'login' | 'dashboard' | 'reservations' | 'creneaux' | 'salles' | 'paiements' = 'login';

  // Forms
  loginForm: FormGroup;
  newCreneauForm: FormGroup;
  editCreneauForm: FormGroup;
  newSalleForm: FormGroup;
  editSalleForm: FormGroup;

  // Données
  creneaux: Creneau[] = [];
  reservations: Reservation[] = [];
  salles: Salle[] = [];

  // Paiements
  reservationsAvecPaiementEnAttente: Reservation[] = [];
  reservationsPayees: Reservation[] = [];

  // Edition / upload
  editingCreneau: Creneau | null = null;
  editingSalle: Salle | null = null;
  selectedFile: File | null = null;

  // API
  private apiUrl = 'http://localhost:9090';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private datePipe: DatePipe
  ) {
    // ====== INIT FORMS ======
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

  // ====== LIFECYCLE ======
  ngOnInit(): void {
    // Vérifier auth + rôle Admin
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

  // ====== AUTH ======
  login(): void {
    if (!this.loginForm.valid) return;
    const { email, password } = this.loginForm.value;
    this.authService.login({ email, password }).subscribe({
      next: (res: any) => {
        localStorage.setItem('token', res.token);
        this.currentPage = 'dashboard';
        this.loadAllData();
      },
      error: () => alert('Email ou mot de passe incorrect.')
    });
  }

  logout(): void {
    this.authService.logout();
    this.currentPage = 'login';
  }

  showPage(page: 'dashboard' | 'reservations' | 'creneaux' | 'salles' | 'paiements'): void {
    this.currentPage = page;
    this.cancelEdit();

    // Charger les données de la page Paiements
    if (page === 'paiements') {
      this.loadReservationsAvecPaiementEnAttente();
      this.loadReservationsPayees();
    }
  }

  // ====== HELPERS HTTP ======
  private getAuthHeaders(): HttpHeaders | null {
    const token = localStorage.getItem('token');
    if (!token) {
      alert('Vous devez être connecté pour accéder à cette fonctionnalité.');
      this.logout();
      return null;
    }
    return new HttpHeaders({ 'Authorization': `Bearer ${token}` });
  }

  // ====== LOAD DATA ======
  loadAllData(): void {
    this.loadReservations();
    this.loadCreneaux();
    this.loadSalles();
  }

  private loadSalles(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Salle[]>(`${this.apiUrl}/salles`, { headers }).subscribe({
      next: (data: Salle[]) => {
        this.salles = data;
        console.log('Salles chargées:', data);

        // Maintenant charger les créneaux et réservations
        this.loadCreneaux();
        this.loadReservations();
      },
      error: err => {
        console.error('Erreur chargement salles:', err);
        if (err.status === 403 || err.status === 401) {
          alert('Accès refusé, veuillez vous reconnecter.');
          this.logout();
        }
      }
    });
  }

  private loadCreneaux(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Creneau[]>(`${this.apiUrl}/creneaux`, { headers }).subscribe({
      next: (creneaux: Creneau[]) => {
        // Charger les informations complètes des salles pour chaque créneau
        this.creneaux = creneaux.map(creneau => {
          if (creneau.salle && creneau.salle.id) {
            const salleComplete = this.salles.find(s => s.id === creneau.salle.id);
            if (salleComplete) {
              creneau.salle = salleComplete;
            }
          }
          return creneau;
        });
      },
      error: err => {
        if (err.status === 403) {
          alert('Accès refusé : vous n\'avez pas la permission d\'accéder aux créneaux.');
        } else {
          console.error('Erreur chargement créneaux:', err);
        }
      }
    });
  }
  private loadReservations(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Reservation[]>(`${this.apiUrl}/reservations`, { headers }).subscribe({
      next: res => this.reservations = res,
      error: err => {
        if (err.status === 403) {
          alert('Accès refusé : vous n\'avez pas la permission d\'accéder aux réservations.');
        } else {
          console.error('Erreur chargement réservations:', err);
        }
      }
    });
  }

  // ====== FICHIERS / IMAGES ======
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

  // ====== SALLES ======
  addSalle(): void {
    if (!this.newSalleForm.valid) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    const formData = new FormData();
    formData.append('nom', this.newSalleForm.get('nom')?.value);
    formData.append('capacite', this.newSalleForm.get('capacite')?.value);
    formData.append('prix', this.newSalleForm.get('prix')?.value);
    formData.append('status', this.newSalleForm.get('status')?.value);
    if (this.selectedFile) formData.append('image', this.selectedFile);

    this.http.post<Salle>(`${this.apiUrl}/salles`, formData, { headers }).subscribe({
      next: res => {
        this.salles.push(res);
        this.newSalleForm.reset();
        this.selectedFile = null;
        this.loadSalles();
      },
      error: err => {
        if (err.status === 403) alert('Accès refusé : vous ne pouvez pas ajouter une salle.');
        else console.error('Erreur ajout salle:', err);
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
      status: salle.status
    });
    this.selectedFile = null;
  }

  updateSalle(): void {
    if (!this.editSalleForm.valid || !this.editingSalle?.id) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    const formData = new FormData();
    formData.append('nom', this.editSalleForm.get('nom')?.value);
    formData.append('capacite', this.editSalleForm.get('capacite')?.value);
    formData.append('prix', this.editSalleForm.get('prix')?.value);
    formData.append('status', this.editSalleForm.get('status')?.value);
    if (this.selectedFile) formData.append('image', this.selectedFile);

    this.http.put<Salle>(`${this.apiUrl}/salles/${this.editingSalle.id}`, formData, { headers }).subscribe({
      next: res => {
        const index = this.salles.findIndex(s => s.id === res.id);
        if (index !== -1) this.salles[index] = res;
        this.cancelEdit();
        this.loadSalles();
      },
      error: err => {
        if (err.status === 403) alert('Accès refusé : vous ne pouvez pas modifier cette salle.');
        else console.error('Erreur modification salle:', err);
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
      },
      error: err => {
        if (err.status === 403) alert('Accès refusé : vous ne pouvez pas supprimer cette salle.');
        else console.error('Erreur suppression salle:', err);
      }
    });
  }

  // ====== CRENEAUX ======
  addCreneau(): void {
    if (!this.newCreneauForm.valid) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    const data = this.newCreneauForm.value;

    // Format correct pour le backend Spring
    const payload = {
      debut: new Date(data.debut).toISOString(),
      fin: new Date(data.fin).toISOString(),
      salle: { id: data.salleId }
    };

    this.http.post<Creneau>(`${this.apiUrl}/creneaux`, payload, { headers }).subscribe({
      next: (res: Creneau) => {
        // Associer la salle complète au créneau
        const salleComplete = this.salles.find(s => s.id === res.salle.id);
        if (salleComplete) {
          res.salle = salleComplete;
        }
        this.creneaux.push(res);
        this.newCreneauForm.reset();
      },
      error: err => {
        if (err.status === 403) {
          alert('Accès refusé : vous ne pouvez pas ajouter ce créneau.');
        } else {
          console.error('Erreur ajout créneau:', err);
        }
      }
    });
  }
  editCreneau(creneau: Creneau): void {
    this.editingCreneau = creneau;
    this.editCreneauForm.patchValue({
      debut: creneau.debut.slice(0, 16), // pour <input type="datetime-local">
      fin: creneau.fin.slice(0, 16),
      salleId: creneau.salle?.id
    });
  }

  updateCreneau(): void {
    if (!this.editCreneauForm.valid || !this.editingCreneau?.id) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    const data = this.editCreneauForm.value;

    // Format correct pour le backend Spring
    const payload = {
      debut: new Date(data.debut).toISOString(),
      fin: new Date(data.fin).toISOString(),
      salle: { id: data.salleId }
    };

    this.http.put<Creneau>(`${this.apiUrl}/creneaux/${this.editingCreneau.id}`, payload, { headers }).subscribe({
      next: (res: Creneau) => {
        // Associer la salle complète au créneau
        const salleComplete = this.salles.find(s => s.id === res.salle.id);
        if (salleComplete) {
          res.salle = salleComplete;
        }

        const index = this.creneaux.findIndex(c => c.id === res.id);
        if (index !== -1) this.creneaux[index] = res;
        this.cancelEdit();
      },
      error: err => {
        if (err.status === 403) alert('Accès refusé : vous ne pouvez pas modifier ce créneau.');
        else console.error('Erreur modification créneau:', err);
      }
    });
  }

  deleteCreneau(id?: number): void {
    if (!id || !confirm('Êtes-vous sûr de vouloir supprimer ce créneau ?')) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.delete(`${this.apiUrl}/creneaux/${id}`, { headers }).subscribe({
      next: () => this.creneaux = this.creneaux.filter(c => c.id !== id),
      error: err => {
        if (err.status === 403) alert('Accès refusé : vous ne pouvez pas supprimer ce créneau.');
        else console.error('Erreur suppression créneau:', err);
      }
    });
  }

  // ====== RESERVATIONS ======
  annulerReservation(id?: number): void {
    if (!id || !confirm('Êtes-vous sûr de vouloir annuler cette réservation ?')) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.delete(`${this.apiUrl}/reservations/${id}`, { headers }).subscribe({
      next: () => this.reservations = this.reservations.filter(r => r.id !== id),
      error: err => {
        if (err.status === 403) alert('Accès refusé : vous ne pouvez pas annuler cette réservation.');
        else console.error('Impossible d\'annuler cette réservation:', err);
      }
    });
  }

  // ====== PAIEMENTS ======
  loadReservationsAvecPaiementEnAttente(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Reservation[]>(`${this.apiUrl}/reservations/paiements/en-attente`, { headers }).subscribe({
      next: res => this.reservationsAvecPaiementEnAttente = res,
      error: err => {
        console.error('Erreur chargement réservations avec paiement en attente:', err);
        if (err.status === 403) alert('Accès refusé pour les réservations avec paiement en attente.');
      }
    });
  }

  loadReservationsPayees(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Reservation[]>(`${this.apiUrl}/reservations/paiements/payees`, { headers }).subscribe({
      next: res => this.reservationsPayees = res,
      error: err => {
        console.error('Erreur chargement réservations payées:', err);
        if (err.status === 403) alert('Accès refusé pour les réservations payées.');
      }
    });
  }

  confirmerPaiement(reservationId?: number): void {
    if (!reservationId || !confirm('Êtes-vous sûr de vouloir confirmer ce paiement ?')) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.post(`${this.apiUrl}/paiements/confirmer/${reservationId}`, {}, { headers }).subscribe({
      next: () => {
        alert('Paiement confirmé avec succès !');
        this.loadReservationsAvecPaiementEnAttente();
        this.loadReservationsPayees();
        this.loadReservations();
      },
      error: err => {
        console.error('Erreur confirmation paiement:', err);
        alert('Erreur lors de la confirmation du paiement.');
      }
    });
  }

  annulerPaiement(reservationId?: number): void {
    if (!reservationId || !confirm('Êtes-vous sûr de vouloir annuler ce paiement ?')) return;
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.post(`${this.apiUrl}/paiements/annuler/${reservationId}`, {}, { headers }).subscribe({
      next: () => {
        alert('Paiement annulé avec succès !');
        this.loadReservationsAvecPaiementEnAttente();
        this.loadReservationsPayees();
        this.loadReservations();
      },
      error: err => {
        console.error('Erreur annulation paiement:', err);
        alert('Erreur lors de l\'annulation du paiement.');
      }
    });
  }

  getPaiementStatusClass(status: string): string {
    switch (status) {
      case 'PAYE': return 'status-paye';
      case 'EN_ATTENTE': return 'status-en-attente';
      case 'ANNULE': return 'status-annule';
      default: return '';
    }
  }

  getPaiementStatusText(status: string): string {
    switch (status) {
      case 'PAYE': return 'Payé';
      case 'EN_ATTENTE': return 'En attente';
      case 'ANNULE': return 'Annulé';
      default: return status;
    }
  }

  // ====== UI HELPERS ======
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

  getStatusClass(status: string): string {
    switch (status) {
      case 'DISPONIBLE': return 'status-available';
      case 'INDISPONIBLE': return 'status-unavailable';
      case 'MAINTENANCE': return 'status-maintenance';
      default: return '';
    }
  }
}
