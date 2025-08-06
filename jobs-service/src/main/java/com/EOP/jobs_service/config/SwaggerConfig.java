package com.EOP.jobs_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.Collections;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jobs Service API")
                        .version("1.0.0")
                        .description("API for managing jobs and applications")
                        .termsOfService("https://jobs-service.com/terms")
                        .contact(new Contact()
                                .name("API Support")
                                .url("https://jobs-service.com/contact")
                                .email("support@jobs-service.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from authentication service")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("Public APIs (No Auth Required)")
                .pathsToMatch("/api/jobs/candidates/apply")
                .addOpenApiCustomizer(openApi -> {
                    // Clear any security requirements for public endpoints
                    openApi.setSecurity(Collections.emptyList());
                })
                .build();
    }

    @Bean
    public GroupedOpenApi privateApi() {
        return GroupedOpenApi.builder()
                .group("private")
                .displayName("Private APIs (JWT Required)")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/jobs/candidates/apply")
                .addOpenApiCustomizer(openApi -> {
                    // Ensure security requirement is added
                    if (openApi.getSecurity() == null) {
                        openApi.setSecurity(new ArrayList<>());
                    }
                    openApi.addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
                })
                .build();
    }

    @Bean
    @Primary
    public ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}