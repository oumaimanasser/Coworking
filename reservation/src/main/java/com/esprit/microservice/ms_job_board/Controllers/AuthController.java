package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.Services.EmailService;
import com.esprit.microservice.ms_job_board.Services.JwtService;
import com.esprit.microservice.ms_job_board.models.PasswordResetToken;
import com.esprit.microservice.ms_job_board.models.User;
import com.esprit.microservice.ms_job_board.Repositories.PasswordResetTokenRepository;
import com.esprit.microservice.ms_job_board.Repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(EmailService emailService,
                          AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody Map<String, String> registerRequest,
                                                            @RequestParam(required = false) String role) {
        try {
            String username = registerRequest.get("username");
            String email = registerRequest.get("email");
            String password = registerRequest.get("password");

            // Manual validation
            if (username == null || username.trim().isEmpty() || username.length() < 3 || username.length() > 50) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le nom d'utilisateur doit contenir entre 3 et 50 caractères"));
            }
            if (email == null || email.trim().isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return ResponseEntity.badRequest().body(Map.of("message", "L'email doit être valide"));
            }
            if (password == null || password.trim().isEmpty() || password.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le mot de passe doit contenir au moins 6 caractères"));
            }

            if (userRepository.existsByUsername(username)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Username déjà utilisé !"));
            }
            if (userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email déjà utilisé !"));
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRoles(Set.of("admin".equalsIgnoreCase(role) ? User.Role.ROLE_ADMIN : User.Role.ROLE_CLIENT));

            User savedUser = userRepository.save(user);
            logger.debug("User saved with id: {} and roles: {}", savedUser.getId(), savedUser.getRoles());

            // Générer le token après l'enregistrement
            String token = jwtService.generateToken(savedUser.getUsername(), savedUser.getEmail(), savedUser.getRoles().stream().map(User.Role::name).collect(Collectors.toList()));

            // Préparer la réponse avec le token
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Utilisateur enregistré avec succès !");
            response.put("userId", String.valueOf(savedUser.getId()));
            response.put("token", token);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'enregistrement: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String email = loginRequest.get("email");
            String password = loginRequest.get("password");

            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Les champs 'email' et 'password' sont obligatoires"));
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            String token = jwtService.generateToken(user.getUsername(), user.getEmail(), user.getRoles().stream().map(User.Role::name).collect(Collectors.toList()));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Connexion réussie !");
            response.put("token", token);
            response.put("email", user.getEmail());
            response.put("username", user.getUsername());
            response.put("roles", user.getRoles().stream().map(User.Role::name).collect(Collectors.toList()));

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Email ou mot de passe incorrect"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la connexion: " + e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "L'email est obligatoire"));
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = new PasswordResetToken();
                resetToken.setToken(token);
                resetToken.setUser(user);
                resetToken.setExpiryDate(LocalDateTime.now().plusHours(24));
                passwordResetTokenRepository.save(resetToken);

                String resetLink = "http://localhost:4200/reset-password?token=" + token;
                String emailContent = "<p>Bonjour " + user.getUsername() + ",</p>"
                        + "<p>Vous avez demandé à réinitialiser votre mot de passe.</p>"
                        + "<p>Cliquez sur le lien ci-dessous pour changer votre mot de passe :</p>"
                        + "<p><a href=\"" + resetLink + "\">Réinitialiser mon mot de passe</a></p>"
                        + "<p>Ce lien expirera dans 24 heures.</p>"
                        + "<p>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p>";

                emailService.sendHtmlEmail(email, "Réinitialisation de votre mot de passe", emailContent);

                return ResponseEntity.ok(Map.of("message", "Email de réinitialisation envoyé !"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Aucun utilisateur trouvé avec cet email"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'envoi de l'email: " + e.getMessage()));
        }
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<Map<String, Object>> validateResetToken(@RequestParam String token) {
        try {
            Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);

            if (resetToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token invalide", "valid", false));
            }

            PasswordResetToken tokenEntity = resetToken.get();
            if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token expiré", "valid", false));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Token valide",
                    "email", tokenEntity.getUser().getEmail(),
                    "valid", true
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la validation du token: " + e.getMessage(), "valid", false));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> resetRequest) {
        try {
            String token = resetRequest.get("token");
            String newPassword = resetRequest.get("newPassword");

            if (token == null || token.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le token et le nouveau mot de passe sont obligatoires"));
            }

            Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);

            if (resetToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token invalide"));
            }

            PasswordResetToken tokenEntity = resetToken.get();
            if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token expiré"));
            }

            User user = tokenEntity.getUser();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            passwordResetTokenRepository.delete(tokenEntity);

            return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la réinitialisation: " + e.getMessage()));
        }
    }

    @PostMapping("/test")
    public ResponseEntity<String> testEndpoint(@RequestBody Map<String, String> request) {
        System.out.println("Test endpoint called with: " + request);
        return ResponseEntity.ok("Test successful - Backend is working at " + LocalDateTime.now());
    }
}
