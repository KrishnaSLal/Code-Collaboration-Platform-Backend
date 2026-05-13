package com.app.collabservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI collabServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeSync Collaboration Service API")
                        .version("1.0.0")
                        .description("Collaboration sessions, participants, cursor updates, and access APIs for CodeSync."));
    }
}
