package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.Repositories.ReservationRepository;
import com.esprit.microservice.ms_job_board.Repositories.UserRepository;
import com.esprit.microservice.ms_job_board.Services.EmailService;
import com.esprit.microservice.ms_job_board.Services.ReservationService;
import com.esprit.microservice.ms_job_board.models.Reservation;
import com.esprit.microservice.ms_job_board.models.ReservationStatus;
import com.esprit.microservice.ms_job_board.models.User;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    private final ReservationService reservationService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    @Autowired
    public ReservationController(ReservationService reservationService,
                                 EmailService emailService,
                                 UserRepository userRepository,
                                 ReservationRepository reservationRepository) {
        this.reservationService = reservationService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    @PostMapping
    public ResponseEntity<?> createReservation(@Valid @RequestBody Reservation reservationRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Tentative de création de réservation sans authentification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                logger.error("Utilisateur non trouvé: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Utilisateur non trouvé dans la base de données: " + username));
            }

            User user = userOpt.get();
            reservationRequest.setClientName(user.getUsername());
            reservationRequest.setClientEmail(user.getEmail());

            Reservation reservation = reservationService.createReservation(reservationRequest);
            // Optional: Send email on creation (pending status)
            sendPendingEmail(reservation, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(reservation);

        } catch (IllegalArgumentException e) {
            logger.error("Erreur de validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Conflit lors de la création: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur interne: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur interne lors de la création de la réservation"));
        }
    }

    private void sendPendingEmail(Reservation reservation, User user) {
        try {
            String subject = "📬 Nouvelle réservation en attente";
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            String dateStr = reservation.getCreneau().getDebut().toLocalDate().format(dateFormatter);
            String heureDebut = reservation.getCreneau().getDebut().format(timeFormatter);
            String heureFin = reservation.getCreneau().getFin().format(timeFormatter);

            String content = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f0ad4e; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f8f9fa; padding: 30px; border-radius: 0 0 5px 5px; }
                    .detail-box { background-color: white; padding: 20px; margin: 20px 0; border-left: 4px solid #f0ad4e; border-radius: 5px; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    h1 { margin: 0; }
                    .pending-icon { font-size: 24px; margin-right: 10px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1><span class="pending-icon">📬</span>Réservation en attente</h1>
                    </div>
                    <div class="content">
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Votre réservation est en attente de confirmation.</p>
                        <div class="detail-box">
                            <h3>📋 Détails de votre réservation :</h3>
                            <ul>
                                <li><strong>🏢 Salle :</strong> %s (Capacité: %d personnes)</li>
                                <li><strong>💰 Prix :</strong> %.2f €</li>
                                <li><strong>📅 Date :</strong> %s</li>
                                <li><strong>⏰ Horaire :</strong> %s - %s</li>
                                <li><strong>👥 Nombre de personnes :</strong> %d</li>
                                <li><strong>📧 Email de contact :</strong> %s</li>
                                <li><strong>🆔 Numéro de réservation :</strong> #%d</li>
                            </ul>
                        </div>
                        <p><strong>Prochaines étapes :</strong> Vous recevrez une confirmation par email une fois votre réservation validée.</p>
                    </div>
                    <div class="footer">
                        <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                        <p>© 2025 Système de Réservation de Salles</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                    user.getUsername(),
                    reservation.getSalle().getNom(),
                    reservation.getSalle().getCapacite(),
                    reservation.getSalle().getPrix(),
                    dateStr,
                    heureDebut,
                    heureFin,
                    reservation.getNombrePersonnes(),
                    user.getEmail(),
                    reservation.getId()
            );

            emailService.sendHtmlEmail(user.getEmail(), subject, content);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de réservation en attente: {}", e.getMessage(), e);
        }
    }

    @PutMapping("/confirm/{id}")
    public ResponseEntity<?> confirmReservation(@PathVariable Long id) {
        try {
            Reservation reservation = reservationService.confirmReservation(id);
            Optional<User> userOpt = userRepository.findByUsername(reservation.getClientName());
            User user = userOpt.orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            sendConfirmationEmail(reservation, user);
            return ResponseEntity.ok(Map.of("message", "Réservation confirmée avec succès", "reservation", reservation));
        } catch (Exception e) {
            logger.error("Erreur lors de la confirmation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Rest of the endpoints remain unchanged
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReservation(@PathVariable Long id, @Valid @RequestBody Reservation updatedDetails) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Tentative de modification sans authentification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentification requise"));
            }

            String currentUsername = authentication.getName();
            Reservation updated = reservationService.updateReservation(id, updatedDetails, currentUsername);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Erreur lors de la modification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Tentative d'annulation sans authentification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            Optional<Reservation> reservationOpt = reservationService.getReservationById(id);
            if (reservationOpt.isPresent()) {
                String currentUsername = authentication.getName();
                String reservationOwner = reservationOpt.get().getClientName();
                if (!currentUsername.equals(reservationOwner)) {
                    logger.warn("Tentative d'annulation par un non-propriétaire: {}", currentUsername);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Vous ne pouvez annuler que vos propres réservations"));
                }
            }

            reservationService.annuler(id);
            return ResponseEntity.ok(Map.of("message", "Réservation annulée avec succès"));
        } catch (Exception e) {
            logger.error("Erreur lors de l'annulation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'annulation: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllReservations() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Tentative d'accès aux réservations sans authentification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            List<Reservation> reservations = reservationService.listerReservations();
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des réservations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération des réservations: " + e.getMessage()));
        }
    }

    @GetMapping("/my-reservations")
    public ResponseEntity<?> getMyReservations() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Tentative d'accès aux réservations personnelles sans authentification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            String username = authentication.getName();
            List<Reservation> userReservations = reservationService.listerReservations()
                    .stream()
                    .filter(r -> r.getClientName().equals(username))
                    .toList();
            return ResponseEntity.ok(userReservations);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des réservations personnelles: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération de vos réservations: " + e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingReservations() {
        try {
            List<Reservation> pending = reservationRepository.findByStatus(ReservationStatus.PENDING);
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des réservations en attente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération des réservations en attente: " + e.getMessage()));
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<?> filterReservationsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Tentative de filtrage des réservations sans authentification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            List<Reservation> reservations = reservationService.filtrerReservationsParDate(start, end);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            logger.error("Erreur lors du filtrage: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors du filtrage: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Tentative d'accès à une réservation sans authentification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            Optional<Reservation> reservation = reservationService.getReservationById(id);
            if (reservation.isPresent()) {
                return ResponseEntity.ok(reservation.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Réservation non trouvée"));
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération: " + e.getMessage()));
        }
    }

    @GetMapping("/paiements/en-attente")
    public ResponseEntity<?> getReservationsAvecPaiementEnAttente() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Tentative d'accès aux paiements en attente sans authentification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            List<Reservation> reservations = reservationService.getReservationsAvecPaiementEnAttente();
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des paiements en attente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération des réservations avec paiement en attente"));
        }
    }

    @GetMapping("/paiements/payees")
    public ResponseEntity<?> getReservationsPayees() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Tentative d'accès aux paiements payés sans authentification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            List<Reservation> reservations = reservationService.getReservationsPayees();
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des paiements payés: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération des réservations payées"));
        }
    }

    private void sendConfirmationEmail(Reservation reservation, User user) throws Exception {
        String subject = "✅ Confirmation de votre réservation de salle";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String dateStr = reservation.getCreneau().getDebut().toLocalDate().format(dateFormatter);
        String heureDebut = reservation.getCreneau().getDebut().format(timeFormatter);
        String heureFin = reservation.getCreneau().getFin().format(timeFormatter);

        String content = String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background-color: #667eea; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                .content { background-color: #f8f9fa; padding: 30px; border-radius: 0 0 5px 5px; }
                .detail-box { background-color: white; padding: 20px; margin: 20px 0; border-left: 4px solid #667eea; border-radius: 5px; }
                .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                h1 { margin: 0; }
                .success-icon { font-size: 24px; margin-right: 10px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1><span class="success-icon">✅</span>Réservation Confirmée</h1>
                </div>
                <div class="content">
                    <p>Bonjour <strong>%s</strong>,</p>
                    <p>Votre réservation a été <strong>confirmée avec succès</strong> !</p>
                    <div class="detail-box">
                        <h3>📋 Détails de votre réservation :</h3>
                        <ul>
                            <li><strong>🏢 Salle :</strong> %s (Capacité: %d personnes)</li>
                            <li><strong>💰 Prix :</strong> %.2f €</li>
                            <li><strong>📅 Date :</strong> %s</li>
                            <li><strong>⏰ Horaire :</strong> %s - %s</li>
                            <li><strong>👥 Nombre de personnes :</strong> %d</li>
                            <li><strong>📧 Email de contact :</strong> %s</li>
                            <li><strong>🆔 Numéro de réservation :</strong> #%d</li>
                        </ul>
                    </div>
                    <p><strong>Important :</strong> Merci de vous présenter à l'heure prévue. En cas d'empêchement, n'hésitez pas à nous contacter.</p>
                    <p>Nous vous souhaitons une excellente réunion !</p>
                </div>
                <div class="footer">
                    <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                    <p>© 2025 Système de Réservation de Salles</p>
                </div>
            </div>
        </body>
        </html>
        """,
                user.getUsername(),
                reservation.getSalle().getNom(),
                reservation.getSalle().getCapacite(),
                reservation.getSalle().getPrix(),
                dateStr,
                heureDebut,
                heureFin,
                reservation.getNombrePersonnes(),
                user.getEmail(),
                reservation.getId()
        );

        emailService.sendHtmlEmail(user.getEmail(), subject, content);
    }
}