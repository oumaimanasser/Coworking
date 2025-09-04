import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Pour ngModel dans le formulaire

export interface Salle {
  id: number;
  nom: string;
  capacite: number;
  prix: number;
  imagePath?: string;
  status?: 'DISPONIBLE' | 'INDISPONIBLE' | 'MAINTENANCE';
}

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  standalone: true,
  imports: [RouterLink, CommonModule, FormsModule], // Modules nécessaires
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  salles: Salle[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';
  contactForm = {
    name: '',
    email: '',
    message: ''
  };
  private apiUrl = 'http://localhost:9090';

  constructor(
    private router: Router,
    private http: HttpClient,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadSalles();
  }

  // Charge les salles disponibles depuis le backend
  loadSalles(): void {
    const headers = this.authService.getAuthHeaders();
    if (!headers || !headers.get('Authorization')) {
      this.errorMessage = 'Session expirée. Veuillez vous reconnecter.';
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
        this.errorMessage = 'Erreur lors du chargement des salles: ' + (err.error?.message || 'Vérifiez la connexion au serveur.');
      }
    });
  }

  // Navigation vers les solutions
  onDiscoverSolutions(): void {
    console.log('Découvrir nos solutions');
    this.router.navigate(['/solutions-bureaux']);
  }

  // Navigation vers les salles de réunion
  onDiscoverMeetingRooms(): void {
    console.log('Découvrir nos salles de réunion');
    this.router.navigate(['/salles-reunion']);
  }

  // Navigation vers les lieux
  onDiscoverVenues(): void {
    console.log('Découvrir nos lieux');
    this.router.navigate(['/lieux-evenements']);
  }

  // Défilement fluide vers la section contact
  scrollToContact(): void {
    document.getElementById('contact-section')?.scrollIntoView({ behavior: 'smooth' });
    console.log('Scroll vers contact');
  }

  // Navigation vers la réservation
  goToReservation(salle?: Salle): void {
    if (salle) {
      console.log(`Navigating to reservation for ${salle.nom}`);
      this.router.navigate(['/reservation'], { state: { salle } }).then(success => {
        if (!success) {
          console.error('Navigation failed');
          this.errorMessage = 'Erreur lors de la navigation vers la réservation.';
        }
      });
    } else {
      this.router.navigate(['/reservation']).then(success => {
        if (!success) {
          console.error('Navigation failed');
          this.errorMessage = 'Erreur lors de la navigation vers la réservation.';
        }
      });
    }
  }

  // Génère l'URL de l'image avec un fallback
  getImageUrl(imagePath?: string): string {
    return imagePath ? `${this.apiUrl}/salles/images/${imagePath.split('/').pop()}` : 'assets/default-image.jpg';
  }

  // Gère les erreurs de chargement d'image
  onImageError(event: Event): void {
    (event.target as HTMLImageElement).src = 'assets/default-image.jpg';
  }

  // Soumet le formulaire de contact
  onSubmitContact(): void {
    console.log('Form submitted:', this.contactForm);
    // Exemple d'appel HTTP (à décommenter et ajuster selon votre backend)
    // this.http.post(`${this.apiUrl}/contact`, this.contactForm, { headers: this.authService.getAuthHeaders() }).subscribe({
    //   next: () => {
    //     this.errorMessage = 'Message envoyé avec succès!';
    //     this.contactForm = { name: '', email: '', message: '' }; // Réinitialisation
    //   },
    //   error: (err) => {
    //     console.error('Erreur envoi contact:', err);
    //     this.errorMessage = 'Erreur lors de l\'envoi du message.';
    //   }
    // });
    this.contactForm = { name: '', email: '', message: '' }; // Réinitialisation par défaut
  }
}
