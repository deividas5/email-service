package com.example.emailservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI emailServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Email Service API")
                        .description("Transactional email microservice — renders Thymeleaf templates and delivers via SMTP with Resilience4j retry.")
                        .version("1.0.0")
                        .contact(new Contact().name("email-service")));
    }
}
