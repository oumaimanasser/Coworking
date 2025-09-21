package com.esprit.microservice.ms_job_board.Services;

import com.esprit.microservice.ms_job_board.models.Creneau;
import com.esprit.microservice.ms_job_board.models.Reservation;
import com.esprit.microservice.ms_job_board.models.Salle;
import com.esprit.microservice.ms_job_board.models.User;
import com.esprit.microservice.ms_job_board.models.PaiementStatus;
import com.esprit.microservice.ms_job_board.models.ReservationStatus;
import com.esprit.microservice.ms_job_board.models.StatutSalle;
import com.esprit.microservice.ms_job_board.Repositories.CreneauRepository;
import com.esprit.microservice.ms_job_board.Repositories.ReservationRepository;
import com.esprit.microservice.ms_job_board.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.esprit.microservice.ms_job_board.Repositories.SalleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final SalleRepository salleRepository;
    private final CreneauRepository creneauRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository,
                              SalleRepository salleRepository,
                              CreneauRepository creneauRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.salleRepository = salleRepository;
        this.creneauRepository = creneauRepository;
        this.userRepository = userRepository;
    }

    private boolean overlaps(Creneau newCreneau, Creneau existing) {
        return !(newCreneau.getFin().isBefore(existing.getDebut()) || newCreneau.getDebut().isAfter(existing.getFin()));
    }

    public Reservation createReservation(Reservation reservation) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName().toLowerCase();
        logger.info("Authenticating user with email: {}", email);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé dans la base: " + email));

        reservation.setClientEmail(user.getEmail().toLowerCase());
        reservation.setClientName(user.getUsername());

        Salle salle = salleRepository.findById(reservation.getSalle().getId())
                .orElseThrow(() -> new IllegalArgumentException("Salle non trouvée"));

        Creneau creneau;
        if (reservation.getCreneau().getId() == null) {
            creneau = new Creneau();
            creneau.setDebut(reservation.getCreneau().getDebut());
            creneau.setFin(reservation.getCreneau().getFin());
            creneau.setSalle(salle);
            // Validation des dates
            if (creneau.getDebut() == null || creneau.getFin() == null || creneau.getDebut().isAfter(creneau.getFin())) {
                throw new IllegalArgumentException("Dates invalides pour le créneau personnalisé");
            }
            List<Creneau> existingCreneaux = creneauRepository.findBySalle(salle);
            for (Creneau ex : existingCreneaux) {
                if (overlaps(creneau, ex)) {
                    logger.error("Conflit de créneau détecté: {} chevauche {}", creneau, ex);
                    throw new RuntimeException("Le créneau personnalisé chevauche un créneau existant");
                }
            }
            creneau = creneauRepository.save(creneau);
        } else {
            creneau = creneauRepository.findById(reservation.getCreneau().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Créneau non trouvé"));
            if (!creneau.getSalle().getId().equals(salle.getId())) {
                throw new IllegalArgumentException("Créneau non associé à cette salle");
            }
        }

        if (salle.getStatus() != StatutSalle.DISPONIBLE) {
            throw new RuntimeException("Salle non disponible");
        }

        if (reservationRepository.existsBySalleAndCreneau(salle, creneau)) {
            logger.error("Conflit: Réservation existante pour salle {} et créneau {}", salle.getId(), creneau.getId());
            throw new RuntimeException("Ce créneau est déjà réservé");
        }

        if (reservation.getNombrePersonnes() > salle.getCapacite()) {
            throw new RuntimeException("Nombre de personnes dépasse la capacité");
        }

        reservation.setSalle(salle);
        reservation.setCreneau(creneau);
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaiementStatus(PaiementStatus.EN_ATTENTE);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));


        Salle salle = reservation.getSalle();
        if (salle.getStatus() != StatutSalle.DISPONIBLE) {
            throw new RuntimeException("Salle non disponible pour confirmation");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        salle.setStatus(StatutSalle.INDISPONIBLE);
        salleRepository.save(salle);

        return reservationRepository.save(reservation);
    }

    public Reservation updateReservation(Long id, Reservation updatedDetails, String currentUsername) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        boolean isAdmin = false; // Implement admin check if needed
        if (!reservation.getClientName().equals(currentUsername) && !isAdmin) {
            throw new RuntimeException("Non autorisé à modifier cette réservation");
        }

        if (updatedDetails.getNombrePersonnes() > 0) {
            if (updatedDetails.getNombrePersonnes() > reservation.getSalle().getCapacite()) {
                throw new RuntimeException("Nombre de personnes dépasse la capacité");
            }
            reservation.setNombrePersonnes(updatedDetails.getNombrePersonnes());
        }

        if (updatedDetails.getCreneau() != null) {
            Creneau newCreneau;
            if (updatedDetails.getCreneau().getId() == null) {
                newCreneau = new Creneau();
                newCreneau.setDebut(updatedDetails.getCreneau().getDebut());
                newCreneau.setFin(updatedDetails.getCreneau().getFin());
                newCreneau.setSalle(reservation.getSalle());

                if (newCreneau.getDebut() == null || newCreneau.getFin() == null || newCreneau.getDebut().isAfter(newCreneau.getFin())) {
                    throw new IllegalArgumentException("Dates invalides pour le créneau personnalisé");
                }

                List<Creneau> existingCreneaux = creneauRepository.findBySalle(reservation.getSalle());
                for (Creneau ex : existingCreneaux) {
                    if (overlaps(newCreneau, ex)) {
                        throw new RuntimeException("Le créneau personnalisé chevauche un créneau existant");
                    }
                }

                newCreneau = creneauRepository.save(newCreneau);
            } else {
                newCreneau = creneauRepository.findById(updatedDetails.getCreneau().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Créneau non trouvé"));
                if (!newCreneau.getSalle().getId().equals(reservation.getSalle().getId())) {
                    throw new IllegalArgumentException("Créneau non associé à cette salle");
                }
            }

            if (reservationRepository.existsBySalleAndCreneau(reservation.getSalle(), newCreneau) && !newCreneau.getId().equals(reservation.getCreneau().getId())) {
                throw new RuntimeException("Ce créneau est déjà réservé");
            }

            reservation.setCreneau(newCreneau);
        }

        return reservationRepository.save(reservation);
    }

    public void annuler(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        Salle salle = reservation.getSalle();
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            salle.setStatus(StatutSalle.DISPONIBLE);
            salleRepository.save(salle);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setPaiementStatus(PaiementStatus.ANNULE);
        reservationRepository.save(reservation);
    }

    public List<Reservation> listerReservations() {
        return reservationRepository.findAll();
    }

    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    public List<Reservation> filtrerReservationsParDate(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        return reservationRepository.findByCreneauDebutBetween(startDateTime, endDateTime);
    }

    public List<Reservation> getReservationsAvecPaiementEnAttente() {
        return reservationRepository.findByPaiementStatus(PaiementStatus.EN_ATTENTE);
    }

    public List<Reservation> getReservationsPayees() {
        return reservationRepository.findByPaiementStatus(PaiementStatus.PAYE);
    }
}