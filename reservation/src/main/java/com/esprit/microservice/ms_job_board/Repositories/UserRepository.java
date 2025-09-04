package com.esprit.microservice.ms_job_board.Repositories;

import com.esprit.microservice.ms_job_board.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Méthode principale pour trouver par username (retourne Optional)
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // Méthode alternative si besoin du premier utilisateur
    @Query("SELECT u FROM User u WHERE u.username = :username ORDER BY u.id LIMIT 1")
    Optional<User> findFirstByUsername(@Param("username") String username);

      }
