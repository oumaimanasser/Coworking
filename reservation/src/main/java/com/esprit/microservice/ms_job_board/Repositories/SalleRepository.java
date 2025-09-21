package com.esprit.microservice.ms_job_board.Repositories;
import com.esprit.microservice.ms_job_board.models.Salle;

import jakarta.persistence.OneToMany;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalleRepository extends JpaRepository<Salle, Long> {
 
}