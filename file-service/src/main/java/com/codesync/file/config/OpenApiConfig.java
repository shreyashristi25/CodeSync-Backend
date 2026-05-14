package com.codesync.file.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI fileServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("CodeSync File Service API")
                .description("Workspace file CRUD and tree")
                .version("v1"));
    }
}
