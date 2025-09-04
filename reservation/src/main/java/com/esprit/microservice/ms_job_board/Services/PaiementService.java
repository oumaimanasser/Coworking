package com.esprit.microservice.ms_job_board.Services;

import com.esprit.microservice.ms_job_board.models.Reservation;
import com.esprit.microservice.ms_job_board.models.PaiementStatus;
import com.esprit.microservice.ms_job_board.Repositories.ReservationRepository;
import com.esprit.microservice.ms_job_board.models.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaiementService {

    private final ReservationRepository reservationRepository;
    private final EmailService emailService;

    public PaiementService(ReservationRepository reservationRepository, EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.emailService = emailService;
    }

    // Méthode pour confirmer le paiement sur place
    public Reservation confirmerPaiementSurPlace(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        // Vérifier que la réservation est confirmée
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("Seules les réservations confirmées peuvent être payées");
        }

        // Vérifier que le paiement n'a pas déjà été effectué
        if (reservation.getPaiementStatus() == PaiementStatus.PAYE) {
            throw new RuntimeException("Paiement déjà effectué pour cette réservation");
        }

        // Enregistrer le paiement
        reservation.setPaiementStatus(PaiementStatus.PAYE);
        Reservation reservationPayee = reservationRepository.save(reservation);

        // Envoyer l'email de confirmation de paiement
        try {
            envoyerEmailConfirmationPaiement(reservationPayee);
        } catch (Exception e) {
            // Loguer l'erreur mais ne pas faire échouer la transaction
            System.err.println("Erreur lors de l'envoi de l'email de confirmation: " + e.getMessage());
        }

        return reservationPayee;
    }

    private void envoyerEmailConfirmationPaiement(Reservation reservation) throws Exception {
        String subject = "✅ Confirmation de paiement pour votre réservation";

        String content = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f8f9fa; padding: 30px; border-radius: 0 0 5px 5px; }
                    .detail-box { background-color: white; padding: 20px; margin: 20px 0; border-left: 4px solid #28a745; border-radius: 5px; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    h1 { margin: 0; }
                    .success-icon { font-size: 24px; margin-right: 10px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1><span class="success-icon">💰</span>Paiement Confirmé</h1>
                    </div>
                    <div class="content">
                        <p>Bonjour <strong>%s</strong>,</p>
                        
                        <p>Votre paiement pour la réservation a été <strong>confirmé avec succès</strong> !</p>
                        
                        <div class="detail-box">
                            <h3>📋 Détails du paiement :</h3>
                            <ul>
                                <li><strong>🏢 Salle :</strong> %s</li>
                                <li><strong>👥 Nombre de personnes :</strong> %d</li>
                                <li><strong>📅 Date :</strong> %s</li>
                                <li><strong>⏰ Horaire :</strong> %s - %s</li>
                                <li><strong>🆔 Numéro de réservation :</strong> #%d</li>
                                <li><strong>💳 Mode de paiement :</strong> Sur place</li>
                                <li><strong>✅ Statut :</strong> Paiement confirmé</li>
                            </ul>
                        </div>
                        
                        <p>Nous vous remercions pour votre confiance et vous souhaitons une excellente réunion !</p>
                    </div>
                    
                    <div class="footer">
                        <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                        <p>© 2024 Système de Réservation de Salles</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                reservation.getClientName(),
                reservation.getSalle().getNom(),
                reservation.getNombrePersonnes(),
                reservation.getCreneau().getDebut().toLocalDate().toString(),
                reservation.getCreneau().getDebut().toLocalTime().toString(),
                reservation.getCreneau().getFin().toLocalTime().toString(),
                reservation.getId()
        );

        emailService.sendHtmlEmail(reservation.getClientEmail(), subject, content);
    }
    public Reservation annulerPaiement(Long reservationId) {
        // Find the reservation by ID
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation avec l'ID " + reservationId + " non trouvée"));

        // Check if payment is already cancelled
        if (reservation.getPaiementStatus() == PaiementStatus.ANNULE) {
            throw new RuntimeException("Le paiement de cette réservation est déjà annulé");
        }

        // Update paiementStatus to ANNULE
        reservation.setPaiementStatus(PaiementStatus.ANNULE);

        // Optionally, update reservation status to CANCELLED
        reservation.setStatus(ReservationStatus.CANCELLED);

        // Save and return the updated reservation
        return reservationRepository.save(reservation);
    }
}