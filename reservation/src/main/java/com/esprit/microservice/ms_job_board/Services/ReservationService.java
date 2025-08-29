package com.esprit.microservice.ms_job_board.Services;

import com.esprit.microservice.ms_job_board.Repositories.CreneauRepository;
import com.esprit.microservice.ms_job_board.Repositories.ReservationRepository;
import com.esprit.microservice.ms_job_board.Repositories.SalleRepository;
import com.esprit.microservice.ms_job_board.models.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SalleRepository salleRepository;
    private final CreneauRepository creneauRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              SalleRepository salleRepository,
                              CreneauRepository creneauRepository) {
        this.reservationRepository = reservationRepository;
        this.salleRepository = salleRepository;
        this.creneauRepository = creneauRepository;
    }

    // Méthode principale pour créer une réservation avec l'objet Reservation
    public Reservation createReservation(Reservation reservation) {
        // Valider que la salle et le créneau existent
        Salle salle = salleRepository.findById(reservation.getSalle().getId())
                .orElseThrow(() -> new RuntimeException("Salle non trouvée"));

        Creneau creneau = creneauRepository.findById(reservation.getCreneau().getId())
                .orElseThrow(() -> new RuntimeException("Créneau non trouvé"));

        // Vérifier la disponibilité de la salle
        if (salle.getStatus() != StatutSalle.DISPONIBLE) {
            throw new RuntimeException("Salle non disponible");
        }

        // Vérifier si le créneau est déjà réservé pour cette salle
        boolean creneauDejaReserve = reservationRepository.existsBySalleAndCreneau(salle, creneau);
        if (creneauDejaReserve) {
            throw new RuntimeException("Ce créneau est déjà réservé pour cette salle");
        }

        // Définir les relations
        reservation.setSalle(salle);
        reservation.setCreneau(creneau);
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.CONFIRMED);

        // Marquer la salle comme OCCUPEE
        salle.setStatus(StatutSalle.INDISPONIBLE);
        salleRepository.save(salle);

        return reservationRepository.save(reservation);
    }

    // Annuler une réservation
    public void annuler(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        // Libérer la salle
        Salle salle = reservation.getSalle();
        salle.setStatus(StatutSalle.DISPONIBLE);
        salleRepository.save(salle);

        reservationRepository.delete(reservation);
    }

    // Lister toutes les réservations
    public List<Reservation> listerReservations() {
        return reservationRepository.findAll();
    }

    // Filtrer les réservations par date
    public List<Reservation> filtrerReservationsParDate(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        return reservationRepository.findByCreneauDebutBetween(startDateTime, endDateTime);
    }

    // Récupérer une réservation par ID
    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    // Récupérer les réservations d'un utilisateur
    public List<Reservation> getReservationsByUser(String username) {
        return reservationRepository.findByClientName(username);
    }
}