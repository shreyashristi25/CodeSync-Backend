package com.codesync.collab.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI collabServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("CodeSync Collab Service API")
                .description("Collaborative editing and presence")
                .version("v1"));
    }
}
