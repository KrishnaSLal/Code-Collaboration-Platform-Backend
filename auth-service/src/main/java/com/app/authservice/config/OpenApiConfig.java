package com.app.authservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeSync Auth Service API")
                        .version("1.0.0")
                        .description("Authentication, OAuth, password reset, and payment APIs for CodeSync."));
    }
}
