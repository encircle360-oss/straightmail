package com.encircle360.oss.straightmail.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
    info = @Info(
        title = "Straightmail",
        version = "0.0.1",
        description = ""
    ),
    servers = {
        @Server(
            url = "http://localhost:50003",
            description = ""
        )
    }
)
@Configuration
public class ApiConfig {
}
