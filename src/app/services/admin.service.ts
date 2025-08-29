import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Creneau {
  id?: number;
  dateHeure: string;
  capacite: number;
  prix: number;
  placesReservees?: number;
}

export interface Reservation {
  id?: number;
  dateReservation: string;
  status: string;
  user: any;
  creneau: Creneau;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private baseUrl = 'http://localhost:9090/api'; // correspond à ton backend

  constructor(private http: HttpClient) {}

  // CRUD Creneau
  getCreneaux(): Observable<Creneau[]> {
    return this.http.get<Creneau[]>(`${this.baseUrl}/creneaux`);
  }

  addCreneau(creneau: Creneau): Observable<Creneau> {
    return this.http.post<Creneau>(`${this.baseUrl}/creneaux`, creneau);
  }

  deleteCreneau(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/creneaux/${id}`);
  }

  getCreneauxDisponibles(): Observable<Creneau[]> {
    return this.http.get<Creneau[]>(`${this.baseUrl}/creneaux/disponibles`);
  }

  // Reservations
  getReservations(startDate?: string, endDate?: string): Observable<Reservation[]> {
    let url = `${this.baseUrl}/reservations`;
    if (startDate && endDate) {
      url += `?startDate=${startDate}&endDate=${endDate}`;
    }
    return this.http.get<Reservation[]>(url);
  }

  reserver(userId: number, creneauId: number): Observable<Reservation> {
    return this.http.post<Reservation>(`${this.baseUrl}/reservations?userId=${userId}&creneauId=${creneauId}`, {});
  }

  annulerReservation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/reservations/${id}`);
  }
}
