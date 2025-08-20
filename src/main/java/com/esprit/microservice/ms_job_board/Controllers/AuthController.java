package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.models.User;
import com.esprit.microservice.ms_job_board.Services.EmailService;
import com.esprit.microservice.ms_job_board.Services.UserService;
import com.esprit.microservice.ms_job_board.Security.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.GrantedAuthority;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;

    // Use constructor injection to avoid circular dependencies
    public AuthController(UserService userService,
                          EmailService emailService,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.registerUser(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'inscription: " + e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            var userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                String token = userService.createPasswordResetToken(userOpt.get());
                String link = "http://localhost:9090/auth/reset-password?token=" + token;
                emailService.sendEmail(email, "Réinitialisation mot de passe",
                        "Cliquez sur ce lien pour réinitialiser votre mot de passe : " + link);
                return ResponseEntity.ok("Email envoyé !");
            }
            return ResponseEntity.badRequest().body("Email non trouvé");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        try {
            if (userService.resetPassword(token, newPassword)) {
                return ResponseEntity.ok("Mot de passe réinitialisé !");
            }
            return ResponseEntity.badRequest().body("Token invalide ou expiré");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la réinitialisation: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Retourner plus d'informations sur la connexion réussie
            return ResponseEntity.ok(Map.of(
                    "message", "Connexion réussie !",
                    "username", authentication.getName(),
                    "authorities", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList())
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Nom d'utilisateur ou mot de passe incorrect");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la connexion: " + e.getMessage());
        }
    }
}