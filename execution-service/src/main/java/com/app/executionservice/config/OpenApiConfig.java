package com.app.executionservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI executionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeSync Execution Service API")
                        .version("1.0.0")
                        .description("Code execution, runtime language, job status, and execution statistics APIs for CodeSync."));
    }
}
