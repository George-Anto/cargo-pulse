package com.gantoniadis.cargopulse.security.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // Identifier for the security scheme
    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, createSecurityScheme()))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }

    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                // Name shown in the UI dropdown
                .name("JWT Authentication")
                // Type of security
                .type(SecurityScheme.Type.HTTP)
                // Standard scheme for JWT
                .scheme("bearer")
                // Format of the token
                .bearerFormat("JWT")
                .description("Enter your **JWT Bearer token** here. Example: `eyJhbGciOiJIUzI1Ni...`");
    }
}