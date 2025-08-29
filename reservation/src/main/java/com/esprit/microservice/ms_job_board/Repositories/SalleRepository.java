package com.esprit.microservice.ms_job_board.Repositories;
import com.esprit.microservice.ms_job_board.models.Salle;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalleRepository extends JpaRepository<Salle, Long> {
}