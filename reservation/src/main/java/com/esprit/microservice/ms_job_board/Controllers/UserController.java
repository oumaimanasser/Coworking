package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.models.PaiementStatus;
import com.esprit.microservice.ms_job_board.models.Reservation;
import com.esprit.microservice.ms_job_board.models.User;
import com.esprit.microservice.ms_job_board.Services.UserService;
import com.esprit.microservice.ms_job_board.Repositories.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ReservationRepository reservationRepository;

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

    @GetMapping("/{id}/reservations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserReservations(@PathVariable Long id) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Reservation> reservations = reservationRepository.findByClientName(user.getUsername());

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
}