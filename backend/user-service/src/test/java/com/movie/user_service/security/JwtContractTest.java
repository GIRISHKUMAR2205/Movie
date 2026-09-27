package com.movie.user_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import com.movie.user_service.service.JwtService;

class JwtContractTest {

    private static final String ISSUER = "https://showhub.local/user-service";
    private static final String AUDIENCE = "showhub-api";
    private static final String SIGNING_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(ISSUER, AUDIENCE, SIGNING_KEY, Duration.ofMinutes(15));
        JwtConfig config = new JwtConfig(properties);
        jwtService = new JwtService(config.jwtEncoder(), config.jwtDecoder(), properties);
    }

    @Test
    void generatedTokenContainsAndValidatesIssuerAudienceAndRoles() {
        String token = jwtService.generateToken(new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        Jwt decoded = jwtService.verifyToken(token);

        assertEquals(ISSUER, decoded.getIssuer().toString());
        assertEquals(List.of(AUDIENCE), decoded.getAudience());
        assertEquals(List.of("ROLE_USER"), decoded.getClaimAsStringList("roles"));
    }

    @Test
    void decoderRejectsTokenForAnotherAudience() {
        JwtProperties validProperties = new JwtProperties(ISSUER, AUDIENCE, SIGNING_KEY, Duration.ofMinutes(15));
        JwtConfig validConfig = new JwtConfig(validProperties);
        String token = new JwtService(validConfig.jwtEncoder(), validConfig.jwtDecoder(), validProperties)
                .generateToken(new UsernamePasswordAuthenticationToken("user@example.com", null, List.of()));

        JwtConfig wrongAudienceConfig = new JwtConfig(
                new JwtProperties(ISSUER, "another-service", SIGNING_KEY, Duration.ofMinutes(15)));

        assertThrows(JwtException.class, () -> wrongAudienceConfig.jwtDecoder().decode(token));
    }

}
