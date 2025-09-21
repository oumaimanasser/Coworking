package com.esprit.microservice.ms_job_board.Repositories;

import com.esprit.microservice.ms_job_board.models.Creneau;
import com.esprit.microservice.ms_job_board.models.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CreneauRepository extends JpaRepository<Creneau, Long> {
    List<Creneau> findByDebutAfter(LocalDateTime dateTime);

    @Query("SELECT c FROM Creneau c WHERE c.salle = :salle")
    List<Creneau> findBySalle(Salle salle);
}
