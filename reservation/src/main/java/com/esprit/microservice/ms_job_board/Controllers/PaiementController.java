package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.Services.PaiementService;
import com.esprit.microservice.ms_job_board.models.Reservation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/paiements")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @PostMapping("/confirmer/{reservationId}")
    public ResponseEntity<?> confirmerPaiementSurPlace(@PathVariable Long reservationId) {
        try {
            Reservation reservation = paiementService.confirmerPaiementSurPlace(reservationId);
            return ResponseEntity.ok(Map.of(
                    "message", "Paiement confirmé avec succès",
                    "reservation", reservation
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Erreur interne lors de la confirmation du paiement"));
        }
    }

    @PostMapping("/annuler/{reservationId}")
    public ResponseEntity<?> annulerPaiement(@PathVariable Long reservationId) {
        try {
            Reservation reservation = paiementService.annulerPaiement(reservationId);
            return ResponseEntity.ok(Map.of(
                    "message", "Paiement annulé avec succès",
                    "reservation", reservation
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Erreur interne lors de l'annulation du paiement"));
        }
    }
}