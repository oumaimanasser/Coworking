package com.esprit.microservice.ms_job_board;

import com.esprit.microservice.ms_job_board.models.User;
import com.esprit.microservice.ms_job_board.Repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
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
	public CommandLineRunner run(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// Vérifier si l'utilisateur admin n'existe pas déjà
			if (!userRepository.existsByUsername("admin")) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setEmail("admin@test.com");
				admin.setPassword(passwordEncoder.encode("admin123"));

				// Utilisation directe de l'enum Role
				Set<User.Role> roles = new HashSet<>();
				roles.add(User.Role.ROLE_ADMIN);
				admin.setRoles(roles);

				userRepository.save(admin);
				System.out.println("Admin user created with username: admin");
			}
		};
	}
}

