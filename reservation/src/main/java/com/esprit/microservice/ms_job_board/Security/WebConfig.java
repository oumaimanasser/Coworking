package com.esprit.microservice.ms_job_board.Security;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // "file:uploads/" = dossier local
        // "/files/**" = URL accessible
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:uploads/");
    }
}
