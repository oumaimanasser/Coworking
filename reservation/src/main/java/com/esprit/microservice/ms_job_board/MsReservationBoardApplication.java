package com.esprit.microservice.ms_job_board;

import com.esprit.microservice.ms_job_board.Repositories.RoleRepository;
import com.esprit.microservice.ms_job_board.Repositories.UserRepository;
import com.esprit.microservice.ms_job_board.models.Role;
import com.esprit.microservice.ms_job_board.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.esprit.microservice.ms_job_board")
public class MsReservationBoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsReservationBoardApplication.class, args);
	}


	// 🔹 Personnalisation du port de l'application
	@Bean
	public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
		return factory -> factory.setPort(9090);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	@Bean
	public CommandLineRunner run(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder encoder) {
		return args -> {
			if (!userRepo.existsByUsername("admin")) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setEmail("admin@test.com");
				admin.setPassword(encoder.encode("admin123"));

				// Récupération du rôle depuis l'Optional
				Role adminRole = roleRepo.findByName("ROLE_ADMIN")
						.orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

				admin.setRoles(Set.of(adminRole));
				userRepo.save(admin);
			}
		};
	}

}
