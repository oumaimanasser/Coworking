package com.esprit.microservice.ms_job_board.models;


import com.esprit.microservice.ms_job_board.models.Role;
import com.esprit.microservice.ms_job_board.Repositories.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
@Component
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataLoader(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(new Role("ROLE_ADMIN"));
        }
        if (roleRepository.findByName("ROLE_CLIENT").isEmpty()) {
            roleRepository.save(new Role("ROLE_CLIENT"));
        }
    }
}
