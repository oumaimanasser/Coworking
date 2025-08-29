package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.Services.CreneauService;
import com.esprit.microservice.ms_job_board.Repositories.SalleRepository;
import com.esprit.microservice.ms_job_board.Repositories.CreneauRepository;
import com.esprit.microservice.ms_job_board.models.Creneau;
import com.esprit.microservice.ms_job_board.models.Salle;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // Ajouter un créneau
    @PostMapping
    public Creneau addCreneau(@RequestBody Creneau creneau) {
        Salle salle = salleRepository.findById(creneau.getSalle().getId())
                .orElseThrow(() -> new RuntimeException("Salle not found"));
        creneau.setSalle(salle);
        return creneauRepository.save(creneau);
    }

    // Récupérer tous les créneaux
    @GetMapping
    public List<Creneau> getAll() {
        return creneauRepository.findAll();
    }

    // Récupérer les créneaux disponibles
    @GetMapping("/disponibles")
    public List<Creneau> getCreneauxDisponibles() {
        return creneauService.rechercherDisponibles();
    }

    // Récupérer un créneau par ID
    @GetMapping("/{id}")
    public Creneau getCreneauById(@PathVariable Long id) {
        return creneauRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Créneau not found with id: " + id));
    }

    // Supprimer un créneau
    @DeleteMapping("/{id}")
    public void deleteCreneau(@PathVariable Long id) {
        creneauRepository.deleteById(id);
    }

    // Mettre à jour un créneau
    @PutMapping("/{id}")
    public Creneau updateCreneau(@PathVariable Long id, @RequestBody Creneau creneauDetails) {
        Creneau creneau = creneauRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Créneau not found with id: " + id));

        creneau.setDebut(creneauDetails.getDebut());
        creneau.setFin(creneauDetails.getFin());

        if (creneauDetails.getSalle() != null && creneauDetails.getSalle().getId() != null) {
            Salle salle = salleRepository.findById(creneauDetails.getSalle().getId())
                    .orElseThrow(() -> new RuntimeException("Salle not found"));
            creneau.setSalle(salle);
        }

        return creneauRepository.save(creneau);
    }
}