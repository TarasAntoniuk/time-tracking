package com.tarasantoniuk.time_tracking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {


        Server remoteServer = new Server()
            .url("https://timetracking.tarasantoniuk.com")
            .description("Production server (HTTPS)");
        Server localServer = new Server()
                .url("http://localhost:8081")
                .description("Production server (HTTPS)");
        return new OpenAPI()
                .servers(List.of(
                        remoteServer
                        , localServer

                ));
    }
}