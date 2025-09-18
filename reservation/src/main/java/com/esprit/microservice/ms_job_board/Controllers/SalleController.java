package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.Repositories.SalleRepository;
import com.esprit.microservice.ms_job_board.models.Salle;
import com.esprit.microservice.ms_job_board.models.StatutSalle;
import com.esprit.microservice.ms_job_board.Services.ImageStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

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
    public List<Salle> getAllSalles() {
        return salleRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Salle> getSalleById(@PathVariable Long id) {
        return salleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Salle> createSalle(
            @RequestParam("nom") String nom,
            @RequestParam("prix") double prix,
            @RequestParam("capacite") int capacite,
            @RequestParam(value = "status", defaultValue = "DISPONIBLE") StatutSalle status,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        Salle salle = new Salle();
        salle.setNom(nom);
        salle.setPrix(prix);
        salle.setCapacite(capacite);
        salle.setStatus(status);

        if (image != null && !image.isEmpty()) {
            String imagePath = imageStorageService.store(image);
            salle.setImagePath(imagePath);
        }

        Salle saved = salleRepository.save(salle);
        return ResponseEntity.ok(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Salle> updateSalle(
            @PathVariable Long id,
            @RequestParam("nom") String nom,
            @RequestParam("prix") double prix,
            @RequestParam("capacite") int capacite,
            @RequestParam("status") StatutSalle status,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        return salleRepository.findById(id)
                .map(existingSalle -> {
                    existingSalle.setNom(nom);
                    existingSalle.setPrix(prix);
                    existingSalle.setCapacite(capacite);
                    existingSalle.setStatus(status);

                    if (image != null && !image.isEmpty()) {
                        if (existingSalle.getImagePath() != null) {
                            imageStorageService.delete(existingSalle.getImagePath());
                        }
                        String imagePath = imageStorageService.store(image);
                        existingSalle.setImagePath(imagePath);
                    }

                    return ResponseEntity.ok(salleRepository.save(existingSalle));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSalle(@PathVariable Long id) {
        return salleRepository.findById(id)
                .map(salle -> {
                    if (salle.getImagePath() != null) {
                        imageStorageService.delete(salle.getImagePath());
                    }
                    salleRepository.deleteById(id);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
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