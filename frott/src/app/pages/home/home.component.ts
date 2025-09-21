import { Component, OnInit, ElementRef, ViewChild, HostListener, OnDestroy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { 
  trigger, 
  state, 
  style, 
  transition, 
  animate, 
  query, 
  stagger 
} from '@angular/animations';

export interface Salle {
  id: number;
  nom: string;
  capacite: number;
  prix: number;
  imagePath?: string;
  status?: 'DISPONIBLE' | 'INDISPONIBLE' | 'MAINTENANCE';
  rating?: number;
  amenities?: string[];
  description?: string;
  type?: 'meeting' | 'coworking' | 'event' | 'default';
}

interface ContactForm {
  name: string;
  email: string;
  subject: string;
  message: string;
}

interface Particle {
  x: number;
  y: number;
  delay: number;
}

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  standalone: true,
  imports: [RouterLink, CommonModule, FormsModule],
  styleUrls: ['./home.component.css'],
  animations: [
    trigger('fadeInUp', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(30px)' }),
        animate('0.6s cubic-bezier(0.4, 0, 0.2, 1)', 
          style({ opacity: 1, transform: 'translateY(0)' })
        )
      ])
    ]),
    trigger('slideInUp', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(50px)' }),
        animate('0.8s cubic-bezier(0.4, 0, 0.2, 1)', 
          style({ opacity: 1, transform: 'translateY(0)' })
        )
      ])
    ]),
    trigger('slideInLeft', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(-50px)' }),
        animate('0.6s cubic-bezier(0.4, 0, 0.2, 1)', 
          style({ opacity: 1, transform: 'translateX(0)' })
        )
      ])
    ]),
    trigger('slideInRight', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(50px)' }),
        animate('0.6s cubic-bezier(0.4, 0, 0.2, 1)', 
          style({ opacity: 1, transform: 'translateX(0)' })
        )
      ])
    ]),
    trigger('cardAnimation', [
      transition(':enter', [
        style({ 
          opacity: 0, 
          transform: 'translateY(50px) scale(0.95)' 
        }),
        animate('0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275)', 
          style({ 
            opacity: 1, 
            transform: 'translateY(0) scale(1)' 
          })
        )
      ])
    ]),
    trigger('staggerAnimation', [
      transition(':enter', [
        query('.space-card', [
          style({ opacity: 0, transform: 'translateY(50px)' }),
          stagger(150, [
            animate('0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275)', 
              style({ opacity: 1, transform: 'translateY(0)' })
            )
          ])
        ], { optional: true })
      ])
    ])
  ]
})
export class HomeComponent implements OnInit, OnDestroy {
  @ViewChild('header') header!: ElementRef;
  @ViewChild('scrollIndicator') scrollIndicator!: ElementRef;
  @ViewChild('featuredSpaces') featuredSpaces!: ElementRef;
  @ViewChild('contactSection') contactSection!: ElementRef;

  salles: Salle[] = [];
  filteredSalles: Salle[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';
  
  contactForm: ContactForm = {
    name: '',
    email: '',
    subject: '',
    message: ''
  };

  newsletterEmail: string = '';
  activeFilter: string = 'all';
  isSubmittingContact: boolean = false;
  contactSuccess: boolean = false;
  
  particles: Particle[] = [];
  
  private apiUrl = 'http://localhost:9090';
  private imageBaseUrl = 'http://localhost:9090/salles/images/';
  private scrollTimeout: any;

  constructor(
    private router: Router,
    private http: HttpClient,
    private authService: AuthService,
    private sanitizer: DomSanitizer
  ) {
    this.generateParticles();
  }

  ngOnInit(): void {
    this.loadSalles();
    this.setupScrollEffects();
    this.setupIntersectionObserver();
  }

  ngOnDestroy(): void {
    if (this.scrollTimeout) {
      clearTimeout(this.scrollTimeout);
    }
  }

  isLoggedIn(): boolean {
    return this.authService.isAuthenticated();
  }

  getUserName(): string {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    return user.username || user.email || 'Utilisateur';
  }

  logout(): void {
    this.authService.logout();
    this.showSuccess('Déconnexion réussie !');
    this.router.navigate(['/home']);
  }

  private generateParticles(): void {
    for (let i = 0; i < 20; i++) {
      this.particles.push({
        x: Math.random() * 100,
        y: Math.random() * 100,
        delay: Math.random() * 6
      });
    }
  }

  private setupScrollEffects(): void {
    this.updateScrollIndicator();
  }

  private setupIntersectionObserver(): void {
    if ('IntersectionObserver' in window) {
      const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            entry.target.classList.add('animate-in');
          }
        });
      }, {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
      });

      setTimeout(() => {
        document.querySelectorAll('.space-card, .section-header, .contact-info').forEach(el => {
          observer.observe(el);
        });
      }, 100);
    }
  }

  @HostListener('window:scroll', ['$event'])
  onScroll(event: Event): void {
    this.updateScrollIndicator();
    this.updateHeaderState();
  }

  private updateScrollIndicator(): void {
    if (this.scrollIndicator) {
      const winScroll = document.body.scrollTop || document.documentElement.scrollTop;
      const height = document.documentElement.scrollHeight - document.documentElement.clientHeight;
      const scrolled = (winScroll / height) * 100;
      this.scrollIndicator.nativeElement.style.width = scrolled + '%';
    }
  }

  private updateHeaderState(): void {
    if (this.header) {
      const scrollY = window.scrollY;
      if (scrollY > 100) {
        this.header.nativeElement.classList.add('scrolled');
      } else {
        this.header.nativeElement.classList.remove('scrolled');
      }
    }
  }

  loadSalles(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const headers = this.authService.getAuthHeaders();
    if (!headers || !headers.get('Authorization')) {
      this.handleAuthError();
      return;
    }

    this.http.get<Salle[]>(`${this.apiUrl}/salles`, { headers }).subscribe({
      next: (data) => {
        const invalidSalles = data.filter(salle => !salle || !salle.nom || !salle.capacite || !salle.prix);
        if (invalidSalles.length) {
          console.warn('Invalid salle data received:', invalidSalles);
        }
        
        this.salles = data
          .filter(salle => salle && salle.nom && salle.capacite && salle.prix)
          .map(salle => {
            const imagePath = salle.imagePath ? `${this.imageBaseUrl}${salle.imagePath}` : undefined;
            console.log(`Salle ${salle.nom}: imagePath = ${salle.imagePath}, full URL = ${imagePath}`);
            return {
              ...salle,
              imagePath: imagePath,
              rating: salle.rating || this.generateRandomRating(),
              amenities: salle.amenities || ['WiFi', 'Café', 'Projecteur'],
              type: salle.type || 'default'
            };
          });
        
        this.applyFilter();
        this.isLoading = false;
        
        setTimeout(() => {
          this.setupIntersectionObserver();
        }, 300);
      },
      error: (err) => {
        console.error('Erreur chargement salles:', err);
        this.handleLoadingError(err);
        this.isLoading = false;
      }
    });
  }

  private handleAuthError(): void {
    this.errorMessage = 'Session expirée. Redirection vers la page de connexion...';
    this.authService.logout();
    setTimeout(() => {
      this.router.navigate(['/login']);
    }, 2000);
    this.isLoading = false;
  }

  private handleLoadingError(error: any): void {
    this.isLoading = false;
    if (error.status === 401) {
      this.handleAuthError();
    } else if (error.status === 0) {
      this.errorMessage = 'Impossible de se connecter au serveur. Vérifiez votre connexion internet.';
    } else {
      this.errorMessage = `Erreur lors du chargement des salles: ${error.error?.message || 'Erreur inconnue'}`;
    }
  }

  private generateRandomRating(): number {
    return Math.round((Math.random() * 1.5 + 3.5) * 10) / 10; // Entre 3.5 et 5.0
  }

  setFilter(filter: string): void {
    this.activeFilter = filter;
    this.applyFilter();
  }

  private applyFilter(): void {
    switch (this.activeFilter) {
      case 'disponible':
        this.filteredSalles = this.salles.filter(s => s.status === 'DISPONIBLE');
        break;
      case 'premium':
        this.filteredSalles = this.salles.filter(s => s.prix >= 50);
        break;
      case 'all':
      default:
        this.filteredSalles = this.salles;
        break;
    }
    console.log('Filtered salles:', this.filteredSalles);
  }

  scrollToContact(): void {
    this.smoothScrollTo('contact-section');
  }

  scrollToSpaces(): void {
    this.smoothScrollTo('featured-spaces');
  }

  private smoothScrollTo(elementId: string): void {
    const element = document.getElementById(elementId);
    if (element) {
      const headerHeight = this.header?.nativeElement?.offsetHeight || 80;
      const elementPosition = element.offsetTop - headerHeight - 20;
      
      window.scrollTo({
        top: elementPosition,
        behavior: 'smooth'
      });
    }
  }

  goToReservation(salle?: Salle): void {
    this.animateButton();
    
    if (salle) {
      console.log(`Navigation vers la réservation pour ${salle.nom}`);
      this.router.navigate(['/reservation'], { 
        state: { 
          salle: salle,
          source: 'home' 
        } 
      }).then(success => {
        if (!success) {
          this.showError('Erreur lors de la navigation vers la réservation.');
        }
      }).catch(error => {
        console.error('Navigation error:', error);
        this.showError('Erreur lors de la navigation.');
      });
    } else {
      this.router.navigate(['/reservation']).then(success => {
        if (!success) {
          this.showError('Erreur lors de la navigation vers la réservation.');
        }
      });
    }
  }

  private animateButton(): void {
    console.log('Button animation triggered');
  }

 getImageUrl(imagePath?: string, salleType?: string): SafeUrl {
  if (!imagePath || imagePath.trim() === '') {
    return this.sanitizer.bypassSecurityTrustUrl(this.getDefaultImageUrl(salleType));
  }

  // Check if imagePath is already a full URL
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    console.log(`Using full image URL: ${imagePath}`);
    return this.sanitizer.bypassSecurityTrustUrl(imagePath);
  }

  // Otherwise, construct the URL using the apiUrl
  const fullUrl = `${this.apiUrl}/salles/images/${imagePath}`;
  console.log(`Image URL generated: ${fullUrl}`);
  return this.sanitizer.bypassSecurityTrustUrl(fullUrl);
}

  private getDefaultImageUrl(salleType?: string): string {
    const defaultImages: { [key: string]: string[] } = {
      meeting: [
        'https://images.unsplash.com/photo-1516321310762-4794370e6a50?w=500&h=300&fit=crop',
        'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=500&h=300&fit=crop',
      ],
      coworking: [
        'https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=500&h=300&fit=crop',
        'https://images.unsplash.com/photo-1560472354-b33ff0c44a43?w=500&h=300&fit=crop',
      ],
      event: [
        'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=500&h=300&fit=crop',
        'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=500&h=300&fit=crop',
      ],
      default: [
        'https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=500&h=300&fit=crop',
        'https://images.unsplash.com/photo-1560472354-b33ff0c44a43?w=500&h=300&fit=crop',
        'https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=500&h=300&fit=crop',
      ],
    };
    const images = defaultImages[salleType || 'default'] || defaultImages['default'];
    const selectedImage = images[Math.floor(Math.random() * images.length)];
    console.log(`Fallback image selected for type ${salleType || 'default'}: ${selectedImage}`);
    return selectedImage;
  }

  onImageError(event: Event, salleType?: string, salleNom?: string): void {
    const imgElement = event.target as HTMLImageElement;
    console.error(`Image failed to load for salle "${salleNom || 'Unknown'}": ${imgElement.src}`);
    imgElement.src = this.getDefaultImageUrl(salleType);
    imgElement.classList.add('image-error');
    console.log(`Switched to fallback image for salle "${salleNom || 'Unknown'}": ${imgElement.src}`);
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'DISPONIBLE':
        return 'disponible';
      case 'INDISPONIBLE':
        return 'indisponible';
      case 'MAINTENANCE':
        return 'maintenance';
      default:
        return 'disponible';
    }
  }

  getStatusIcon(status?: string): string {
    switch (status) {
      case 'DISPONIBLE':
        return 'fas fa-check-circle';
      case 'INDISPONIBLE':
        return 'fas fa-times-circle';
      case 'MAINTENANCE':
        return 'fas fa-tools';
      default:
        return 'fas fa-question-circle';
    }
  }

  onSubmitContact(): void {
    if (this.isSubmittingContact) {
      return;
    }

    if (!this.isContactFormValid()) {
      this.showError('Veuillez remplir tous les champs requis.');
      return;
    }

    this.isSubmittingContact = true;
    this.contactSuccess = false;

    this.http.post(`${this.apiUrl}/contact`, this.contactForm).subscribe({
      next: () => {
        this.handleContactSuccess();
      },
      error: (error) => {
        this.handleContactError(error);
      }
    });
  }

  private isContactFormValid(): boolean {
    return !!(
      this.contactForm.name.trim() &&
      this.contactForm.email.trim() &&
      this.contactForm.subject.trim() &&
      this.contactForm.message.trim()
    );
  }

  private handleContactSuccess(): void {
    this.isSubmittingContact = false;
    this.contactSuccess = true;
    
    setTimeout(() => {
      this.resetContactForm();
      this.contactSuccess = false;
    }, 5000);
    
    console.log('Message de contact envoyé:', this.contactForm);
  }

  private handleContactError(error: any): void {
    this.isSubmittingContact = false;
    console.error('Erreur envoi contact:', error);
    
    let errorMessage = 'Erreur lors de l\'envoi du message.';
    if (error.status === 0) {
      errorMessage = 'Impossible de se connecter au serveur.';
    } else if (error.error?.message) {
      errorMessage = error.error.message;
    }
    
    this.showError(errorMessage);
  }

  private resetContactForm(): void {
    this.contactForm = {
      name: '',
      email: '',
      subject: '',
      message: ''
    };
  }

  private showError(message: string): void {
    this.errorMessage = message;
    setTimeout(() => {
      this.errorMessage = '';
    }, 5000);
  }

  private showSuccess(message: string): void {
    this.errorMessage = message;
    setTimeout(() => {
      this.errorMessage = '';
    }, 5000);
  }

  onNewsletterSubmit(email: string): void {
    if (email && this.isValidEmail(email)) {
      console.log('Newsletter subscription:', email);
      this.http.post(`${this.apiUrl}/newsletter`, { email }).subscribe({
        next: () => {
          this.showSuccess('Inscription à la newsletter réussie !');
          this.newsletterEmail = '';
        },
        error: (error) => {
          this.showError('Erreur lors de l\'inscription à la newsletter.');
          console.error('Newsletter error:', error);
        }
      });
    } else {
      this.showError('Veuillez entrer un email valide.');
    }
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  onCardHover(salle: Salle): void {
    console.log('Card hovered:', salle.nom);
  }

  onCardClick(salle: Salle): void {
    console.log('Card clicked:', salle.nom);
  }

  onDiscoverSolutions(): void {
    this.router.navigate(['/solutions-bureaux']);
  }

  onDiscoverMeetingRooms(): void {
    this.router.navigate(['/salles-reunion']);
  }

  onDiscoverVenues(): void {
    this.router.navigate(['/lieux-evenements']);
  }

  onSocialClick(platform: string): void {
    console.log(`Redirection vers ${platform}`);
  }

  refreshData(): void {
    this.loadSalles();
  }

  onKeyboardNavigation(event: KeyboardEvent, action: string): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      switch (action) {
        case 'scroll-contact':
          this.scrollToContact();
          break;
        case 'scroll-spaces':
          this.scrollToSpaces();
          break;
      }
    }
  }

  trackInteraction(action: string, label?: string): void {
    console.log('Tracking interaction:', { action, label });
  }
}