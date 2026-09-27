package com.movie.apigateway.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.movie.apigateway.config.GatewayCorsProperties;
import com.movie.apigateway.error.ApiErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties({ LocalJwtProperties.class, GatewayCorsProperties.class })
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final LocalJwtProperties localJwtProperties;
    private final GatewayCorsProperties corsProperties;
    private final ObjectMapper objectMapper;
    private  static final Logger log=LoggerFactory.getLogger(SecurityConfiguration.class);

    @Bean
    @Order(1)
    SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .securityMatcher(
                "/api/v1/users/signup",
                "/api/v1/users/login",
                "/api/v1/users/refresh",
                "/api/v1/users/oauth2/**",
                "/api/v1/users/login/oauth2/**"
        )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus", "/v3/api-docs/**", "/swagger-ui/**", "/swagger.html")
                        .permitAll()
                        // .requestMatchers(HttpMethod.POST, "/api/v1/users/signup", "/api/v1/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/users/email-verifications/verify",
                                "/api/v1/users/oauth2/**",
                                "/api/v1/users/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhooks/sandbox").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/movies/**", "/api/v1/concerts/**", "/api/v1/venues/**", "/api/v1/shows/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/theaters/*/schedule").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationManagerResolver(authenticationManagerResolver())
                        .authenticationEntryPoint(this::writeUnauthorized))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(this::writeUnauthorized)
                        .accessDeniedHandler((request, response, denied) ->
                                writeError(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied.")))
                .build();
    }

    @Bean
public OncePerRequestFilter requestLoggingFilter() {
    return new OncePerRequestFilter() {
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {

            System.out.println(
                "USER SERVICE REQUEST: "
                + request.getMethod()
                + " "
                + request.getRequestURI()
            );

            filterChain.doFilter(request, response);
        }
    };
}


    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Accept", "Authorization", "Content-Type", "Idempotency-Key",
                "X-Correlation-Id", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Location", "X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder());
        provider.setJwtAuthenticationConverter(jwtAuthenticationConverter());
        return request -> provider::authenticate;
    }

    private JwtDecoder jwtDecoder() {
        byte[] key = Base64.getDecoder().decode(localJwtProperties.jwtSigningKey());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(localJwtProperties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(localJwtProperties.issuer()), audienceValidator));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> jwt.getClaimAsStringList("roles") == null
                ? List.of()
                : jwt.getClaimAsStringList("roles").stream()
                        .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                        .toList());
        return converter;
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        writeError(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED_API", "Authentication is required.");
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ApiErrorResponse(Instant.now(), status.value(), code, message, request.getRequestURI()));
    }
}
