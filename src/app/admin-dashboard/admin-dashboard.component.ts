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
  status: 'DISPONIBLE' | 'INDISPONIBLE' | 'MAINTENANCE';
  imagePath?: string;
}

export interface Creneau {
  id?: number;
  debut: string;
  fin: string;
  salle: Salle;
}

export interface Reservation {
  id?: number;
  clientName: string;
  salle: Salle;
  creneau: Creneau;
  dateReservation: string;
  status: string;
}

@Component({
  selector: 'admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  currentPage: 'login' | 'dashboard' | 'reservations' | 'creneaux' | 'salles' = 'login';

  loginForm: FormGroup;
  newCreneauForm: FormGroup;
  editCreneauForm: FormGroup;
  newSalleForm: FormGroup;
  editSalleForm: FormGroup;

  creneaux: Creneau[] = [];
  reservations: Reservation[] = [];
  salles: Salle[] = [];

  editingCreneau: Creneau | null = null;
  editingSalle: Salle | null = null;
  selectedFile: File | null = null;

  private apiUrl = 'http://localhost:9090';


  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private authService: AuthService,private router: Router,
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
      status: ['DISPONIBLE', Validators.required]
    });

    this.editSalleForm = this.fb.group({
      id: [''],
      nom: ['', Validators.required],
      capacite: [1, [Validators.required, Validators.min(1)]],
      status: ['DISPONIBLE', Validators.required]
    });
  }
  ngOnInit(): void {
    // Vérifier si l'utilisateur est admin
    if (!this.authService.isAdmin()) {
      this.router.navigate(['/home'], {
        queryParams: { message: 'Accès non autorisé' }
      });
      return;
    }

    if (this.authService.isAuthenticated()) {
      this.currentPage = 'dashboard';
      this.loadAllData();
    } else {
      this.currentPage = 'login';
    }
  }
  // ===== LOGIN =====
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

  showPage(page: 'dashboard' | 'reservations' | 'creneaux' | 'salles'): void {
    this.currentPage = page;
    this.cancelEdit();
  }

  // ===== LOAD DATA =====
  loadAllData(): void {
    this.loadReservations();
    this.loadCreneaux();
    this.loadSalles();
  }

  private getAuthHeaders(): HttpHeaders | null {
    const token = localStorage.getItem('token');
    if (!token) {
      alert('Vous devez être connecté pour accéder à cette fonctionnalité.');
      this.logout();
      return null;
    }
    return new HttpHeaders({ 'Authorization': `Bearer ${token}` });
  }

  private loadSalles(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Salle[]>(`${this.apiUrl}/salles`)
      .subscribe({
        next: data => { this.salles = data; },
        error: err => {
          console.error("Erreur chargement salles:", err);
          if (err.status === 403 || err.status === 401) {
            alert("Accès refusé, veuillez vous reconnecter.");
          }
        }
      });
  }

  private loadCreneaux(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;

    this.http.get<Creneau[]>(`${this.apiUrl}/creneaux`, { headers }).subscribe({
      next: res => this.creneaux = res,
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

  // ===== GESTION DES FICHIERS =====
  onFileSelected(event: any): void {
    this.selectedFile = event.target.files[0] as File;
  }

  getImageUrl(imagePath: string): string {
    return `${this.apiUrl}/salles/images/${imagePath}`;
  }

  // ===== SALLES =====
  addSalle(): void {
    if (!this.newSalleForm.valid) return;

    const formData = new FormData();
    formData.append('nom', this.newSalleForm.get('nom')?.value);
    formData.append('capacite', this.newSalleForm.get('capacite')?.value);
    formData.append('status', this.newSalleForm.get('status')?.value);

    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    this.http.post<Salle>(`${this.apiUrl}/salles`, formData).subscribe({
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
      status: salle.status
    });
    this.selectedFile = null;
  }

  updateSalle(): void {
    if (!this.editSalleForm.valid) return;

    const formData = new FormData();
    formData.append('nom', this.editSalleForm.get('nom')?.value);
    formData.append('capacite', this.editSalleForm.get('capacite')?.value);
    formData.append('status', this.editSalleForm.get('status')?.value);

    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    this.http.put<Salle>(`${this.apiUrl}/salles/${this.editingSalle?.id}`, formData).subscribe({
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
    const headers = this.getAuthHeaders();
    if (!headers) return;
    if (!id || !confirm('Êtes-vous sûr de vouloir supprimer cette salle ?')) return;

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

  // ===== CRENEAUX =====
  addCreneau(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;
    if (!this.newCreneauForm.valid) return;

    const data = this.newCreneauForm.value;
    const payload = { debut: data.debut, fin: data.fin, salle: { id: data.salleId } };
    this.http.post<Creneau>(`${this.apiUrl}/creneaux`, payload, { headers }).subscribe({
      next: res => {
        res.salle = this.salles.find(s => s.id === res.salle.id) || res.salle;
        this.creneaux.push(res);
        this.newCreneauForm.reset();
      },
      error: err => {
        if (err.status === 403) alert('Accès refusé : vous ne pouvez pas ajouter ce créneau.');
        else console.error('Erreur ajout créneau:', err);
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

  updateCreneau(): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;
    if (!this.editCreneauForm.valid || !this.editingCreneau?.id) return;

    const data = this.editCreneauForm.value;
    const payload = { debut: data.debut, fin: data.fin, salle: { id: data.salleId } };
    this.http.put<Creneau>(`${this.apiUrl}/creneaux/${this.editingCreneau.id}`, payload, { headers }).subscribe({
      next: res => {
        res.salle = this.salles.find(s => s.id === res.salle.id) || res.salle;
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
    const headers = this.getAuthHeaders();
    if (!headers) return;
    if (!id || !confirm('Êtes-vous sûr de vouloir supprimer ce créneau ?')) return;

    this.http.delete(`${this.apiUrl}/creneaux/${id}`, { headers }).subscribe({
      next: () => this.creneaux = this.creneaux.filter(c => c.id !== id),
      error: err => {
        if (err.status === 403) alert('Accès refusé : vous ne pouvez pas supprimer ce créneau.');
        else console.error('Erreur suppression créneau:', err);
      }
    });
  }

  // ===== RESERVATIONS =====
  annulerReservation(id?: number): void {
    const headers = this.getAuthHeaders();
    if (!headers) return;
    if (!id || !confirm('Êtes-vous sûr de vouloir annuler cette réservation ?')) return;

    this.http.delete(`${this.apiUrl}/reservations/${id}`, { headers }).subscribe({
      next: () => this.reservations = this.reservations.filter(r => r.id !== id),
      error: err => {
        if (err.status === 403) alert('Accès refusé : vous ne pouvez pas annuler cette réservation.');
        else console.error('Impossible d\'annuler cette réservation:', err);
      }
    });
  }

  cancelEdit(): void {
    this.editingCreneau = null;
    this.editingSalle = null;
    this.editCreneauForm.reset();
    this.editSalleForm.reset();
    this.selectedFile = null;
  }

  formatDate(date?: string): string {
    return date ? this.datePipe.transform(date, 'dd/MM/yyyy HH:mm')! : '';
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
