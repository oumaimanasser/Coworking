package com.esprit.microservice.ms_job_board.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final Path rootLocation = Paths.get("uploads");

    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }

            // Créer le répertoire s'il n'existe pas
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }

            // Générer un nom de fichier unique
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path destinationFile = rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();

            Files.copy(file.getInputStream(), destinationFile);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public void delete(String filename) {
        try {
            if (filename != null && !filename.isEmpty()) {
                Path file = rootLocation.resolve(filename).normalize().toAbsolutePath();
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + filename, e);
        }
    }
}