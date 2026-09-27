package com.movie.apigateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "showhub.security.local")
public record LocalJwtProperties(String issuer, String audience, String jwtSigningKey) {

    public LocalJwtProperties {
        issuer = issuer == null || issuer.isBlank() ? "https://showhub.local/user-service" : issuer;
        audience = audience == null || audience.isBlank() ? "showhub-api" : audience;
    }
}
