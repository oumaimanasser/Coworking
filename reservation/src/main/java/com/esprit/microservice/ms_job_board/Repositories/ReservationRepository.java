package com.esprit.microservice.ms_job_board.Repositories;

import com.esprit.microservice.ms_job_board.models.Reservation;
import com.esprit.microservice.ms_job_board.models.ReservationStatus;
import com.esprit.microservice.ms_job_board.models.Salle;
import com.esprit.microservice.ms_job_board.models.Creneau;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // ✅ Méthodes CORRECTES pour Reservation
    int countByCreneau(Creneau creneau);
    boolean existsBySalleAndCreneau(Salle salle, Creneau creneau);

    // ❌ ENLEVEZ cette ligne si elle existe :
    // Optional<Reservation> findByUsername(String username);

    // ✅ Méthodes utiles pour Reservation :
    List<Reservation> findByClientName(String clientName);
    List<Reservation> findByClientEmail(String clientEmail);
    List<Reservation> findBySalleId(Long salleId);
    List<Reservation> findByCreneauId(Long creneauId);

    List<Reservation> findByStatus(ReservationStatus reservationStatus);

    List<Reservation> findByCreneauDebutBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);
}