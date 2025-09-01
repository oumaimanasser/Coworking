package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.Services.EmailService;
import com.esprit.microservice.ms_job_board.Services.ReservationService;
import com.esprit.microservice.ms_job_board.models.Reservation;
import com.esprit.microservice.ms_job_board.models.User;
import com.esprit.microservice.ms_job_board.Repositories.UserRepository;
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

    private final ReservationService reservationService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public ReservationController(ReservationService reservationService,
                                 EmailService emailService,
                                 UserRepository userRepository) {
        this.reservationService = reservationService;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody Reservation reservationRequest) {
        try {
            System.out.println("📦 Received reservation request: " + reservationRequest.toString());

            // Récupérer l'authentification actuelle
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                System.out.println("❌ No authentication found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            String username = authentication.getName();
            System.out.println("🔍 Authenticated user: " + username);

            // Validation des paramètres de base
            if (reservationRequest.getSalle() == null || reservationRequest.getSalle().getId() == null ||
                    reservationRequest.getCreneau() == null || reservationRequest.getCreneau().getId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Paramètres manquants: salle et creneau sont requis"));
            }

            // Recherche de l'utilisateur authentifié dans la base de données
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                System.out.println("❌ User not found in database: " + username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Utilisateur non trouvé dans la base de données: " + username));
            }

            User user = userOpt.get();
            System.out.println("✅ User found in database: " + user.getUsername() + ", email: " + user.getEmail());

            // Définir les informations du client à partir de l'utilisateur authentifié
            reservationRequest.setClientName(user.getUsername());
            reservationRequest.setClientEmail(user.getEmail());

            // Créer la réservation
            Reservation reservation = reservationService.createReservation(reservationRequest);
            System.out.println("✅ Reservation created successfully with ID: " + reservation.getId());

            // Envoi de l'email de confirmation
            try {
                sendConfirmationEmail(reservation, user);
                System.out.println("✅ Confirmation email sent to: " + user.getEmail());
            } catch (Exception emailError) {
                System.err.println("⚠️ Erreur lors de l'envoi de l'email: " + emailError.getMessage());
                // On continue même si l'email échoue
            }

            return ResponseEntity.ok(reservation);

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            System.err.println("❌ Business logic error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error in createReservation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur interne lors de la création de la réservation"));
        }
    }

    private void sendConfirmationEmail(Reservation reservation, User user) throws Exception {
        String subject = "✅ Confirmation de votre réservation de salle";

        // Formatage des dates et heures
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
                    <p>© 2024 Système de Réservation de Salles</p>
                </div>
            </div>
        </body>
        </html>
        """,
                user.getUsername(),
                reservation.getSalle().getNom(),
                reservation.getSalle().getCapacite(),
                reservation.getSalle().getPrix(), // Ajout du prix
                dateStr,
                heureDebut,
                heureFin,
                reservation.getNombrePersonnes(), // Ajout du nombre de personnes
                user.getEmail(),
                reservation.getId()
        );

        emailService.sendHtmlEmail(user.getEmail(), subject, content);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        try {
            // Vérifier l'authentification
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            // Optionnel : vérifier que l'utilisateur annule sa propre réservation
            Optional<Reservation> reservationOpt = reservationService.getReservationById(id);
            if (reservationOpt.isPresent()) {
                String currentUsername = authentication.getName();
                String reservationOwner = reservationOpt.get().getClientName();

                // Seul le propriétaire de la réservation peut l'annuler
                if (!currentUsername.equals(reservationOwner)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Vous ne pouvez annuler que vos propres réservations"));
                }
            }

            reservationService.annuler(id);
            System.out.println("✅ Reservation " + id + " cancelled by " + authentication.getName());

            return ResponseEntity.ok(Map.of("message", "Réservation annulée avec succès"));
        } catch (Exception e) {
            System.err.println("❌ Error cancelling reservation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'annulation: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllReservations() {
        try {
            // Vérifier l'authentification
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            List<Reservation> reservations = reservationService.listerReservations();
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            System.err.println("❌ Error getting reservations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération des réservations: " + e.getMessage()));
        }
    }

    @GetMapping("/my-reservations")
    public ResponseEntity<?> getMyReservations() {
        try {
            // Récupérer l'utilisateur authentifié
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            String username = authentication.getName();

            // Récupérer seulement les réservations de l'utilisateur connecté
            List<Reservation> userReservations = reservationService.listerReservations()
                    .stream()
                    .filter(r -> r.getClientName().equals(username))
                    .toList();

            return ResponseEntity.ok(userReservations);
        } catch (Exception e) {
            System.err.println("❌ Error getting user reservations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération de vos réservations: " + e.getMessage()));
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<?> filterReservationsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            // Vérifier l'authentification
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            List<Reservation> reservations = reservationService.filtrerReservationsParDate(start, end);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            System.err.println("❌ Error filtering reservations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors du filtrage: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationById(@PathVariable Long id) {
        try {
            // Vérifier l'authentification
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
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
            System.err.println("❌ Error getting reservation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération: " + e.getMessage()));
        }
    }
    @GetMapping("/paiements/en-attente")
    public ResponseEntity<?> getReservationsAvecPaiementEnAttente() {
        try {
            // Vérifier l'authentification
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            List<Reservation> reservations = reservationService.getReservationsAvecPaiementEnAttente();
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            System.err.println("❌ Error getting reservations with pending payment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération des réservations avec paiement en attente"));
        }
    }

    @GetMapping("/paiements/payees")
    public ResponseEntity<?> getReservationsPayees() {
        try {
            // Vérifier l'authentification
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentification requise"));
            }

            List<Reservation> reservations = reservationService.getReservationsPayees();
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            System.err.println("❌ Error getting paid reservations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération des réservations payées"));
        }
    }

}