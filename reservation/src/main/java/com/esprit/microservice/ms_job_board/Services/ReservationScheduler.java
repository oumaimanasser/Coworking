package com.esprit.microservice.ms_job_board.Services;

import com.esprit.microservice.ms_job_board.Repositories.ReservationRepository;
import com.esprit.microservice.ms_job_board.Repositories.SalleRepository;
import com.esprit.microservice.ms_job_board.models.Reservation;
import com.esprit.microservice.ms_job_board.models.ReservationStatus;
import com.esprit.microservice.ms_job_board.models.StatutSalle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final SalleRepository salleRepository;
    private final EmailService emailService;

    public ReservationScheduler(ReservationRepository reservationRepository,
                                SalleRepository salleRepository,
                                EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.salleRepository = salleRepository;
        this.emailService = emailService;
    }

    // Scheduler exécuté toutes les heures pour annuler les réservations PENDING depuis plus de 24h
    @Scheduled(fixedRate = 3600000)
    public void cancelPendingReservations() {
        List<Reservation> pendingReservations = reservationRepository.findByStatus(ReservationStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();

        for (Reservation r : pendingReservations) {
            // Vérifie si la réservation PENDING date de plus de 24h
            if (r.getCreneau() != null && r.getCreneau().getDebut().plusHours(24).isBefore(now)) {
                r.setStatus(ReservationStatus.CANCELLED);
                reservationRepository.save(r);

                // Met à jour le statut de la salle à DISPONIBLE
                if (r.getSalle() != null) {
                    r.getSalle().setStatus(StatutSalle.DISPONIBLE);
                    salleRepository.save(r.getSalle());
                }

                // Envoi d'un email automatique
                String dateStr = r.getCreneau() != null
                        ? r.getCreneau().getDebut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "N/A";

                String html = "<html><body style='font-family:Arial,sans-serif;'>" +
                        "<h2 style='color:#E74C3C;'>Annulation automatique</h2>" +
                        "<p>Bonjour " + r.getClientName() + ",</p>" +
                        "<p>Votre réservation pour le créneau du <strong>" + dateStr + "</strong> a été annulée automatiquement car non confirmée.</p>" +
                        "<p>Merci !</p></body></html>";

                emailService.sendHtmlEmail(
                        r.getClientName() + "@example.com", // ou utilisez r.getUser().getEmail() si vous liez un User
                        "Annulation automatique de réservation",
                        html
                );
            }
        }
    }
}
