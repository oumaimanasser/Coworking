package com.esprit.microservice.ms_job_board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

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
}
