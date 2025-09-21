package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.Repositories.SalleRepository;
import com.esprit.microservice.ms_job_board.models.Salle;
import com.esprit.microservice.ms_job_board.models.StatutSalle;
import com.esprit.microservice.ms_job_board.Services.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.core.io.Resource;

import org.springframework.http.HttpHeaders;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/salles")
public class SalleController {

    private static final Logger logger = LoggerFactory.getLogger(SalleController.class);

    private final SalleRepository salleRepository;
    private final ImageStorageService imageStorageService;
    private final Path rootLocation = Paths.get("uploads");

    public SalleController(SalleRepository salleRepository, ImageStorageService imageStorageService) {
        this.salleRepository = salleRepository;
        this.imageStorageService = imageStorageService;
        this.imageStorageService.init();
    }

    @GetMapping
    public ResponseEntity<List<Salle>> getAllSalles() {
        List<Salle> salles = salleRepository.findAll();
        logger.debug("Retrieved {} salles", salles.size());
        return ResponseEntity.ok(salles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Salle> getSalleById(@PathVariable Long id) {
        return salleRepository.findById(id)
                .map(salle -> {
                    logger.debug("Retrieved Salle with ID: {}", id);
                    return ResponseEntity.ok(salle);
                })
                .orElseGet(() -> {
                    logger.warn("Salle with ID {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Salle> createSalle(
            @RequestParam("nom") String nom,
            @RequestParam("prix") double prix,
            @RequestParam("capacite") int capacite,
            @RequestParam(value = "status", defaultValue = "DISPONIBLE") StatutSalle status,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        Salle salle = new Salle();
        salle.setNom(nom);
        salle.setPrix(prix);
        salle.setCapacite(capacite);
        salle.setStatus(status);

        if (image != null && !image.isEmpty()) {
            try {
                String imagePath = imageStorageService.store(image);
                Path filePath = rootLocation.resolve(imagePath);
                if (Files.exists(filePath)) {
                    salle.setImagePath(imagePath);
                    logger.info("Stored image {} for new Salle at {}", imagePath, filePath);
                } else {
                    logger.error("Failed to verify image storage at {} for new Salle", filePath);
                    throw new IOException("Image storage verification failed");
                }
            } catch (RuntimeException e) {
                logger.error("Failed to store image for new Salle: {}", e.getMessage());
                throw new IOException("Failed to store image", e);
            }
        }

        Salle saved = salleRepository.save(salle);
        logger.info("Created Salle with ID: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                if (contentType == null || !contentType.startsWith("image/")) {
                    contentType = "application/octet-stream"; // Fallback for unknown types
                    logger.warn("Could not determine content type for file: {}, using fallback", filename);
                }
                logger.info("Serving image: {} with Content-Type: {}", filename, contentType);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                logger.warn("Image file {} does not exist or is not readable", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (  IOException e) {
            logger.error("Error serving image {}: {}", filename, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSalle(@PathVariable Long id) {
        return salleRepository.findById(id)
                .map(salle -> {
                    try {
                        if (salle.getImagePath() != null) {
                            try {
                                imageStorageService.delete(salle.getImagePath());
                                logger.info("Deleted image {} for Salle ID {}", salle.getImagePath(), id);
                            } catch (RuntimeException e) {
                                logger.warn("Failed to delete image {} for Salle ID {}: {}", salle.getImagePath(), id, e.getMessage());
                            }
                        }
                        salleRepository.deleteById(id);
                        logger.info("Successfully deleted Salle with ID: {}", id);
                        return ResponseEntity.ok("Salle with ID " + id + " deleted successfully");
                    } catch (Exception e) {
                        logger.error("Failed to delete Salle ID {}: {}", id, e.getMessage());
                        throw new RuntimeException("Deletion failed, transaction rolled back", e);
                    }
                })
                .orElseGet(() -> {
                    logger.warn("Salle with ID {} not found for deletion", id);
                    return ResponseEntity.status(404).body("Salle with ID " + id + " not found");
                });
    }
}