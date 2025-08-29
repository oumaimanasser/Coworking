package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.Repositories.SalleRepository;
import com.esprit.microservice.ms_job_board.models.Salle;
import com.esprit.microservice.ms_job_board.models.StatutSalle;
import com.esprit.microservice.ms_job_board.services.ImageStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/salles")
public class SalleController {

    private final SalleRepository salleRepository;
    private final ImageStorageService imageStorageService;
    private final Path rootLocation = Paths.get("uploads");

    public SalleController(SalleRepository salleRepository, ImageStorageService imageStorageService) {
        this.salleRepository = salleRepository;
        this.imageStorageService = imageStorageService;
        this.imageStorageService.init();
    }

    @GetMapping
    public List<Salle> getAll() {
        return salleRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Salle> getById(@PathVariable Long id) {
        return salleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint pour créer une salle avec image
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Salle addWithImage(
            @RequestParam("nom") String nom,
            @RequestParam("capacite") int capacite,
            @RequestParam(value = "status", defaultValue = "DISPONIBLE") StatutSalle status,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        Salle salle = new Salle();
        salle.setNom(nom);
        salle.setCapacite(capacite);
        salle.setStatus(status);

        if (image != null && !image.isEmpty()) {
            String imagePath = imageStorageService.store(image);
            salle.setImagePath(imagePath);
        }

        return salleRepository.save(salle);
    }

    // Endpoint pour créer une salle sans image (compatibilité)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Salle add(@RequestBody Salle salle) {
        return salleRepository.save(salle);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return salleRepository.findById(id)
                .map(salle -> {
                    // Supprimer l'image associée si elle existe
                    if (salle.getImagePath() != null) {
                        imageStorageService.delete(salle.getImagePath());
                    }
                    salleRepository.deleteById(id);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint pour mettre à jour une salle avec image
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Salle> updateWithImage(
            @PathVariable Long id,
            @RequestParam("nom") String nom,
            @RequestParam("capacite") int capacite,
            @RequestParam("status") StatutSalle status,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        return salleRepository.findById(id)
                .map(existingSalle -> {
                    // Supprimer l'ancienne image si elle existe et qu'une nouvelle image est fournie
                    if (image != null && !image.isEmpty()) {
                        if (existingSalle.getImagePath() != null) {
                            imageStorageService.delete(existingSalle.getImagePath());
                        }
                        String imagePath = imageStorageService.store(image);
                        existingSalle.setImagePath(imagePath);
                    }

                    existingSalle.setNom(nom);
                    existingSalle.setCapacite(capacite);
                    existingSalle.setStatus(status);

                    return ResponseEntity.ok(salleRepository.save(existingSalle));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint pour mettre à jour une salle sans image (compatibilité)
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Salle> update(@PathVariable Long id, @RequestBody Salle salleDetails) {
        return salleRepository.findById(id)
                .map(existingSalle -> {
                    existingSalle.setNom(salleDetails.getNom());
                    existingSalle.setCapacite(salleDetails.getCapacite());
                    existingSalle.setStatus(salleDetails.getStatus());
                    // Note: l'image n'est pas modifiée dans cette méthode
                    return ResponseEntity.ok(salleRepository.save(existingSalle));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint pour servir les images
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
}