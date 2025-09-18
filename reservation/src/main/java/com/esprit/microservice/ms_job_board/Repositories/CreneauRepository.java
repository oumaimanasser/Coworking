package com.esprit.microservice.ms_job_board.Repositories;
import com.esprit.microservice.ms_job_board.models.Creneau;


import com.esprit.microservice.ms_job_board.models.Salle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CreneauRepository extends JpaRepository<Creneau, Long> {
    List<Creneau> findByDebutAfter(LocalDateTime dateTime);
    List<Creneau> findBySalle(Salle salle);

}
