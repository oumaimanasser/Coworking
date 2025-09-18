package com.esprit.microservice.ms_job_board.Services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:36000000}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            if (keyBytes.length < 32) {
                logger.error("JWT secret length ({}) is less than 32 bytes, which is insecure", keyBytes.length);
                throw new IllegalArgumentException("JWT secret must be at least 32 bytes long");
            }
            logger.debug("Decoded JWT secret length: {}", keyBytes.length);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to decode JWT secret: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT secret configuration", e);
        }
    }

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            logger.error("Error extracting username from JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public String extractActualUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Error parsing JWT token: {}", e.getMessage());
            throw new RuntimeException("Unable to parse JWT token", e);
        }
    }

    public String generateToken(String username, String email, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(email)
                .claim("username", username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            logger.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public List<String> extractRoles(String token) {
        try {
            List<String> roles = extractClaim(token, claims -> claims.get("roles", List.class));
            logger.debug("Extracted roles from token: {}", roles);
            return roles != null ? roles : List.of();
        } catch (Exception e) {
            logger.error("Error extracting roles from JWT token: {}", e.getMessage());
            return List.of();
        }
    }

    public List<SimpleGrantedAuthority> getAuthorities(String token) {
        List<String> roles = extractRoles(token);
        return roles.stream()
                .map(role -> {
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    return new SimpleGrantedAuthority(authority);
                })
                .collect(Collectors.toList());
    }

    public boolean hasRole(String token, String role) {
        List<String> roles = extractRoles(token);
        return roles != null && roles.contains(role);
    }
}