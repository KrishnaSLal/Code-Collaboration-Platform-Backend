package com.app.versionservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI versionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeSync Version Service API")
                        .version("1.0.0")
                        .description("Snapshots, branches, diffs, restore, and tagging APIs for CodeSync."));
    }
}
