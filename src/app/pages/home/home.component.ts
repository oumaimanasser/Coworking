import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  standalone: true,
  styleUrls: ['./home.component.css']
})
export class HomeComponent {

  constructor(private router: Router) { }

  onDiscoverSolutions() {
    console.log('Découvrir nos solutions');
    // Navigation vers la page solutions bureaux
  }

  onDiscoverMeetingRooms() {
    console.log('Découvrir nos salles de réunion');
    // Navigation vers la page salles de réunion
  }

  onDiscoverVenues() {
    console.log('Découvrir nos lieux');
    // Navigation vers la page lieux événements
  }

  scrollToContact() {
    // Ici tu peux faire défiler vers une section contact
    console.log('Scroll vers contact');
  }

  // Méthode correcte pour naviguer vers la page réservation
  goToReservation() {
    this.router.navigate(['/reservation']);
  }
}
