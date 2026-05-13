package com.app.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeSync Notification Service API")
                        .version("1.0.0")
                        .description("In-app notification, email notification, bulk send, and unread count APIs for CodeSync."));
    }
}
