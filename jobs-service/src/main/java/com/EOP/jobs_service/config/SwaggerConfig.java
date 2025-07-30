package com.EOP.jobs_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSchemas("JobApplicationRequest", new ObjectSchema()
                                .addProperty("email", new StringSchema().example("test@example.com"))
                                .addProperty("jobId", new IntegerSchema().example(1)))
                        .addSchemas("MultipartRequest", new ObjectSchema()
                                .addProperty("applicationRequest", new Schema<Object>().$ref("#/components/schemas/JobApplicationRequest"))
                                .addProperty("resume", new StringSchema().format("binary"))));
    }

}
