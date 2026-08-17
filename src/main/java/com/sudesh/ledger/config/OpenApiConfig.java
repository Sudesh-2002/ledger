package com.sudesh.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Ledger — CQRS + Event Sourcing Banking API")
                .description("Command side writes to an append-only event store; " +
                        "query side is an eventually-consistent projection rebuilt from events via Kafka.")
                .version("v1"));
    }
}