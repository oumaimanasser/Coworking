package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.Services.CreneauService;
import com.esprit.microservice.ms_job_board.Repositories.SalleRepository;
import com.esprit.microservice.ms_job_board.Repositories.CreneauRepository;
import com.esprit.microservice.ms_job_board.models.Creneau;
import com.esprit.microservice.ms_job_board.models.Salle;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/creneaux")
public class CreneauController {

    private final CreneauRepository creneauRepository;
    private final SalleRepository salleRepository;
    private final CreneauService creneauService;

    public CreneauController(CreneauRepository creneauRepository,
                             SalleRepository salleRepository,
                             CreneauService creneauService) {
        this.creneauRepository = creneauRepository;
        this.salleRepository = salleRepository;
        this.creneauService = creneauService;
    }

    private boolean overlaps(Creneau newCreneau, Creneau existing) {
        return !(newCreneau.getFin().isBefore(existing.getDebut()) || newCreneau.getDebut().isAfter(existing.getFin()));
    }

    @PostMapping
    public ResponseEntity<?> addCreneau(@Valid @RequestBody Creneau creneau) {
        try {
            Salle salle = salleRepository.findById(creneau.getSalle().getId())
                    .orElseThrow(() -> new RuntimeException("Salle not found"));

            // Check for overlapping creneaux
            List<Creneau> existingCreneaux = creneauRepository.findBySalle(salle);
            for (Creneau ex : existingCreneaux) {
                if (overlaps(creneau, ex)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Créneau chevauche un créneau existant"));
                }
            }

            creneau.setSalle(salle);
            Creneau saved = creneauRepository.save(creneau);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public List<Creneau> getAll() {
        return creneauRepository.findAll();
    }

    @GetMapping("/disponibles")
    public List<Creneau> getCreneauxDisponibles() {
        return creneauService.rechercherDisponibles();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Creneau> getCreneauById(@PathVariable Long id) {
        return creneauRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("Créneau not found with id: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCreneau(@PathVariable Long id) {
        try {
            creneauRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Créneau supprimé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCreneau(@PathVariable Long id, @Valid @RequestBody Creneau creneauDetails) {
        try {
            Creneau creneau = creneauRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Créneau not found with id: " + id));

            Salle salle = salleRepository.findById(creneauDetails.getSalle().getId())
                    .orElseThrow(() -> new RuntimeException("Salle not found"));

            creneau.setDebut(creneauDetails.getDebut());
            creneau.setFin(creneauDetails.getFin());
            creneau.setSalle(salle);

            // Check for overlapping creneaux
            List<Creneau> existingCreneaux = creneauRepository.findBySalle(salle);
            for (Creneau ex : existingCreneaux) {
                if (ex.getId() != id && overlaps(creneau, ex)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Créneau chevauche un créneau existant"));
                }
            }

            Creneau updated = creneauRepository.save(creneau);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}