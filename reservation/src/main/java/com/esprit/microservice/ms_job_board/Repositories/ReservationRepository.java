package com.esprit.microservice.ms_job_board.Repositories;

import com.esprit.microservice.ms_job_board.models.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Méthodes CORRECTES pour Reservation
    int countByCreneau(Creneau creneau);
    boolean existsBySalleAndCreneau(Salle salle, Creneau creneau);

    // Méthodes utiles pour Reservation :
    List<Reservation> findByClientName(String clientName);
    List<Reservation> findByClientEmail(String clientEmail);
    List<Reservation> findBySalleId(Long salleId);
    List<Reservation> findByCreneauId(Long creneauId);

    List<Reservation> findByStatus(ReservationStatus reservationStatus);

    @Query("SELECT r FROM Reservation r WHERE r.creneau.debut BETWEEN :startDateTime AND :endDateTime")
    List<Reservation> findByCreneauDebutBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<Reservation> findByPaiementStatus(PaiementStatus paiementStatus);
    @Query("SELECT r FROM Reservation r JOIN FETCH r.salle JOIN FETCH r.creneau WHERE r.clientName = :clientName")
    List<Reservation> findByClientNameWithDetails(String clientName);
}