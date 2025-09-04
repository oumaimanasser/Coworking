package com.esprit.microservice.ms_job_board.Controllers;

import com.esprit.microservice.ms_job_board.Repositories.UserRepository;
import com.esprit.microservice.ms_job_board.Repositories.RoleRepository;
import com.esprit.microservice.ms_job_board.Repositories.PasswordResetTokenRepository;
import com.esprit.microservice.ms_job_board.Services.JwtService;
import com.esprit.microservice.ms_job_board.models.User;
import com.esprit.microservice.ms_job_board.models.Role;
import com.esprit.microservice.ms_job_board.models.PasswordResetToken;
import com.esprit.microservice.ms_job_board.Services.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(EmailService emailService,
                          AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody Map<String, String> registerRequest) {
        try {
            String username = registerRequest.get("username");
            String email = registerRequest.get("email");
            String password = registerRequest.get("password");

            System.out.println("Received registration request: " + username + ", " + email);

            if (username == null || email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tous les champs sont obligatoires"));
            }

            if (userRepository.existsByUsername(username)) {
                System.out.println("Username already exists: " + username);
                return ResponseEntity.badRequest().body(Map.of("message", "Username déjà utilisé !"));
            }
            if (userRepository.existsByEmail(email)) {
                System.out.println("Email already exists: : " + email);
                return ResponseEntity.badRequest().body(Map.of("message", "Email déjà utilisé !"));
            }

            // Assigner automatiquement le rôle ROLE_CLIENT
            Role clientRole = roleRepository.findByName("ROLE_CLIENT")
                    .orElseThrow(() -> new RuntimeException("Rôle ROLE_CLIENT non trouvé"));

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRoles(Set.of(clientRole));

            User savedUser = userRepository.save(user);
            System.out.println("User registered successfully: " + savedUser.getUsername());

            return ResponseEntity.ok(Map.of("message", "Client enregistré avec succès !"));

        } catch (RuntimeException e) {
            System.out.println("Registration error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            System.out.println("Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'enregistrement: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String email = loginRequest.get("email");
            String password = loginRequest.get("password");

            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Les champs 'email' et 'password' sont obligatoires"));
            }

            // Authentifier directement avec l'email
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Récupérer l'utilisateur après authentification réussie
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé après authentification"));

            // Get authorities (roles)
            List<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Générer le token JWT avec username, email, roles
            String token = jwtService.generateToken(user.getUsername(), user.getEmail(), authorities);

            return ResponseEntity.ok(Map.of(
                    "message", "Connexion réussie !",
                    "token", token,
                    "email", user.getEmail(),
                    "username", user.getUsername(),
                    "authorities", authorities
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Email ou mot de passe incorrect"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la connexion: " + e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "L'email est obligatoire"));
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Générer un token de réinitialisation
                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = new PasswordResetToken();
                resetToken.setToken(token);
                resetToken.setUser(user);
                resetToken.setExpiryDate(LocalDateTime.now().plusHours(24)); // 24 heures d'expiration

                passwordResetTokenRepository.save(resetToken);

                // Envoyer l'email
                String resetLink = "http://localhost:4200/reset-password?token=" + token;
                String emailContent = "<p>Bonjour,</p>"
                        + "<p>Vous avez demandé à réinitialiser votre mot de passe.</p>"
                        + "<p>Cliquez sur le lien ci-dessous pour changer votre mot de passe :</p>"
                        + "<p><a href=\"" + resetLink + "\">Réinitialiser mon mot de passe</a></p>"
                        + "<p>Ce lien expirera dans 24 heures.</p>"
                        + "<p>Si vous n'avez pas demandé cette réinitialisation, ignorez simplement cet email.</p>";

                emailService.sendHtmlEmail(email, "Réinitialisation de votre mot de passe", emailContent);

                return ResponseEntity.ok(Map.of("message", "Email de réinitialisation envoyé !"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Aucun utilisateur trouvé avec cet email"));
        } catch (Exception e) {
            System.err.println("Error in forgotPassword: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'envoi de l'email: " + e.getMessage()));
        }
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        try {
            Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);

            if (resetToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token invalide"));
            }

            PasswordResetToken tokenEntity = resetToken.get();

            if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token expiré"));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Token valide",
                    "email", tokenEntity.getUser().getEmail(),
                    "valid", true
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la validation du token: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> resetRequest) {
        try {
            String token = resetRequest.get("token");
            String newPassword = resetRequest.get("newPassword");

            if (token == null || newPassword == null) {
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

            // Supprimer le token utilisé
            passwordResetTokenRepository.delete(tokenEntity);

            return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la réinitialisation: " + e.getMessage()));
        }
    }

    // Endpoint de test
    @PostMapping("/test")
    public ResponseEntity<String> testEndpoint(@RequestBody Map<String, String> request) {
        System.out.println("Test endpoint called with: " + request);
        return ResponseEntity.ok("Test successful - Backend is working");
    }
}