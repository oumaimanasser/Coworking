package com.esprit.microservice.ms_job_board.Repositories;


import com.esprit.microservice.ms_job_board.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

}
