package com.movie.user_service.security;

import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private final JwtProperties jwtProperties;

    public JwtConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey()));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(signingKey(), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()),
                audienceValidator()));
        return decoder;
    }

    @Bean
    ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    private byte[] signingKey() {
        try {
            byte[] key = Base64.getDecoder().decode(jwtProperties.jwtSigningKey());
            if (key.length < 32) {
                throw new IllegalStateException("The JWT signing key must contain at least 256 bits.");
            }
            return key;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("The JWT signing key must be Base64 encoded.", exception);
        }
    }

    private OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> audienceValidator() {
        return jwt -> jwt.getAudience().contains(jwtProperties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "The token was not issued for ShowHub.", null));
    }

}
