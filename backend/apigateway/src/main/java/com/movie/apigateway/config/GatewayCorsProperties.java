package com.movie.apigateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "showhub.gateway.cors")
public record GatewayCorsProperties(List<String> allowedOrigins) {

    public GatewayCorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
