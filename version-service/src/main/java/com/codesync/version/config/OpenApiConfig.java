package com.codesync.version.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI versionOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("CodeSync Version Service API")
                .description("Snapshots and Myers diffs")
                .version("v1"));
    }
}
