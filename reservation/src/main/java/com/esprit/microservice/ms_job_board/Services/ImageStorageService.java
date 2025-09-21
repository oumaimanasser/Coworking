package com.esprit.microservice.ms_job_board.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageStorageService.class);

    private final Path rootLocation = Paths.get("uploads");

    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                logger.error("Attempted to store empty file");
                throw new RuntimeException("Failed to store empty file.");
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                logger.error("Invalid file type: {}. Only images are allowed", contentType);
                throw new IllegalArgumentException("Only image files are allowed");
            }

            // Create directory if it doesn't exist
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
                logger.info("Created upload directory: {}", rootLocation);
            }

            // Generate unique filename
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.-]", "_");
            Path destinationFile = rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            Files.copy(file.getInputStream(), destinationFile);
            logger.info("Stored file {} at {}", filename, destinationFile);
            return filename;
        } catch (IOException e) {
            logger.error("Failed to store file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public void init() {
        try {
            Files.createDirectories(rootLocation);
            logger.info("Initialized upload directory: {}", rootLocation);
        } catch (IOException e) {
            logger.error("Could not initialize upload directory: {}", rootLocation, e);
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public void delete(String filename) {
        try {
            if (filename != null && !filename.isEmpty()) {
                Path file = rootLocation.resolve(filename).normalize().toAbsolutePath();
                if (Files.deleteIfExists(file)) {
                    logger.info("Deleted file: {}", file);
                } else {
                    logger.warn("File {} not found for deletion", file);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", filename, e);
            throw new RuntimeException("Failed to delete file: " + filename, e);
        }
    }

    public byte[] loadImage(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            logger.error("Filename cannot be null or empty");
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        Path file = rootLocation.resolve(filename).normalize().toAbsolutePath();
        if (!Files.exists(file)) {
            logger.error("Image file not found: {}", file);
            throw new IOException("Image file not found: " + filename);
        }
        logger.debug("Loaded image: {}", file);
        return Files.readAllBytes(file);
    }
}