package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.models.PaiementStatus;
import com.esprit.microservice.ms_job_board.models.Reservation;
import com.esprit.microservice.ms_job_board.models.User;
import com.esprit.microservice.ms_job_board.Services.UserService;
import com.esprit.microservice.ms_job_board.Repositories.ReservationRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.esprit.microservice.ms_job_board.Repositories.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Admin-only endpoints
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Profile endpoints for authenticated users
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Non authentifié"));
        }

        String email = authentication.getName().toLowerCase();
        User user = userService.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody Map<String, Object> updates) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Non authentifié"));
        }

        String email = authentication.getName().toLowerCase();
        User user = userService.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (updates.containsKey("username") && updates.get("username") != null) {
            String username = (String) updates.get("username");
            if (username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le nom d'utilisateur ne peut pas être vide"));
            }
            user.setUsername(username);
        }
        if (updates.containsKey("email") && updates.get("email") != null) {
            String newEmail = ((String) updates.get("email")).toLowerCase();
            if (!newEmail.equals(user.getEmail()) && userService.findByEmailIgnoreCase(newEmail).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cet email est déjà utilisé"));
            }
            user.setEmail(newEmail);
        }
        if (updates.containsKey("password") && updates.get("password") != null) {
            String password = (String) updates.get("password");
            if (password.length() < 8) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le mot de passe doit contenir au moins 8 caractères"));
            }
            user.setPassword(passwordEncoder.encode(password));
        }

        userService.save(user);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profil mis à jour avec succès");
        response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reservations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserReservations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Non authentifié"));
        }

        String email = authentication.getName().toLowerCase();
        User user = userService.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Query reservations by client_email instead of client_name
        List<Reservation> reservations = reservationRepository.findByClientEmail(email);

        long totalReservations = reservations.size();
        long paid = reservations.stream().filter(r -> r.getPaiementStatus() == PaiementStatus.PAYE).count();
        long unpaid = totalReservations - paid;

        Map<String, Object> response = new HashMap<>();
        response.put("reservations", reservations);
        response.put("total", totalReservations);
        response.put("paid", paid);
        response.put("unpaid", unpaid);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/reservations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserReservationsById(@PathVariable Long id) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Reservation> reservations = reservationRepository.findByClientEmail(user.getEmail());

        long totalReservations = reservations.size();
        long paid = reservations.stream().filter(r -> r.getPaiementStatus() == PaiementStatus.PAYE).count();
        long unpaid = totalReservations - paid;

        Map<String, Object> response = new HashMap<>();
        response.put("reservations", reservations);
        response.put("total", totalReservations);
        response.put("paid", paid);
        response.put("unpaid", unpaid);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reservations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteReservation(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Non authentifié"));
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));
        String email = authentication.getName().toLowerCase();
        User user = userService.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!reservation.getClientEmail().equals(user.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Vous n'êtes pas autorisé à supprimer cette réservation"));
        }

        reservationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Réservation supprimée avec succès"));
    }
}