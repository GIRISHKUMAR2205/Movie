package com.movie.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "showhub.security.local")
public record JwtProperties(String issuer, String audience, String jwtSigningKey) {
}
