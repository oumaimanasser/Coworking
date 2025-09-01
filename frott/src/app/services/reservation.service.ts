import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {

  private apiUrl = 'http://localhost:9090/reservations';

  constructor(private http: HttpClient) { }

  // Récupérer le JWT depuis le localStorage (après login)
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token'); // stocké après login
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  // Créer une réservation
  createReservation(salleId: number, creneauId: number): Observable<any> {
    const body = {
      salle: { id: salleId },
      creneau: { id: creneauId }
    };
    return this.http.post(this.apiUrl, body, { headers: this.getAuthHeaders() });
  }

  // Lister les réservations (admin)
  getAllReservations(): Observable<any> {
    return this.http.get(this.apiUrl, { headers: this.getAuthHeaders() });
  }
}
