package com.movie.theater_service.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.movie.theater_service.error.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties({ JwtProperties.class, InternalApiProperties.class })
public class SecurityConfiguration {

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfiguration(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/internal/v1/shows/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/shows").permitAll()
                        .requestMatchers(HttpMethod.GET, "/{theaterId}/schedule").permitAll()
                        .anyRequest().hasAuthority("ROLE_THEATER_ADMIN"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(this::unauthorized))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(this::unauthorized)
                        .accessDeniedHandler((request, response, error) ->
                                writeError(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied.")))
                .build();
    }

    private JwtDecoder jwtDecoder() {
        byte[] key = Base64.getDecoder().decode(jwtProperties.jwtSigningKey());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(jwtProperties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()), audienceValidator));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> jwt.getClaimAsStringList("roles") == null ? List.of()
                : jwt.getClaimAsStringList("roles").stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
        return converter;
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, Exception error) throws IOException {
        writeError(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required.");
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ApiErrorResponse(Instant.now(), status.value(), code, message, request.getRequestURI()));
    }
}
