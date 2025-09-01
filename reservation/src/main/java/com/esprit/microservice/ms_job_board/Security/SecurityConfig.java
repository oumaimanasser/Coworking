package com.esprit.microservice.ms_job_board.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(UserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Auth provider
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Authentication manager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // Auth routes accessibles à tous
                        .requestMatchers("/auth/**").permitAll()

                        // Salles routes
                        .requestMatchers(HttpMethod.GET, "/salles/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/salles/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/salles/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/salles/**").hasAuthority("ADMIN")

                        // Images des salles accessibles à tous
                        .requestMatchers(HttpMethod.GET, "/salles/images/**").permitAll()

                        // Creneaux routes
                        .requestMatchers(HttpMethod.GET, "/creneaux/disponibles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/creneaux/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/creneaux/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/creneaux/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/creneaux/**").hasAuthority("ADMIN")

                        // Reservations
                        .requestMatchers(HttpMethod.POST, "/reservations").authenticated() // USER peut créer
                        .requestMatchers(HttpMethod.GET, "/reservations/my-reservations").authenticated() // USER peut consulter ses résas
                        .requestMatchers(HttpMethod.GET, "/reservations/**").hasAuthority("ADMIN") // ADMIN peut consulter toutes les résas
                        .requestMatchers(HttpMethod.PUT, "/reservations/**").hasAuthority("ADMIN") // ADMIN peut modifier
                        .requestMatchers(HttpMethod.DELETE, "/reservations/**").hasAuthority("ADMIN") // ADMIN peut supprimer

                        // Paiements (admin only)
                        .requestMatchers(HttpMethod.POST, "/paiements/confirmer/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/paiements/en-attente").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/paiements/payees").hasAuthority("ADMIN")

                        // Toutes les autres requêtes
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS configuration pour Angular
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:4200")); // Angular dev
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}