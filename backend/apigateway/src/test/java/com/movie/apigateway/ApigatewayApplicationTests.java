package com.movie.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "showhub.security.local.jwt-signing-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "showhub.security.local.issuer=https://showhub.local/user-service"
})
@AutoConfigureMockMvc
class ApigatewayApplicationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void configuresThePhaseThreeServiceRoutes() {
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[0].id"))
                .isEqualTo("user-service");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[0].filters[0]"))
                .isEqualTo("StripPrefix=2");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[0].filters[1]"))
                .isEqualTo("PrefixPath=/users");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[1].id"))
                .isEqualTo("booking-service-seat-status");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[2].id"))
                .isEqualTo("theater-service");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[2].filters[1]"))
                .isEqualTo("PrefixPath=/theaters");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[3].id"))
                .isEqualTo("movie-service");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[3].filters[1]"))
                .isEqualTo("PrefixPath=/movies");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[4].id"))
                .isEqualTo("catalog-service");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[5].id"))
                .isEqualTo("booking-service");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[6].id"))
                .isEqualTo("payment-service");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[7].id"))
                .isEqualTo("theater-show-catalog");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[7].filters[1]"))
                .isEqualTo("PrefixPath=/theaters");
    }

    @Test
    void protectsPrivateRoutesWhileLeavingHealthPublic() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void exposesPrometheusMetricsForScraping() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    void acceptsAValidShowHubAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/not-a-resource")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + localToken(Instant.now().plusSeconds(60))))
                .andExpect(status().isNotFound());
    }

    @Test
    void ignoresLegacyAccessTokenCookiesOnPublicEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .cookie(new Cookie("auth-token", localToken(Instant.now().minusSeconds(120)))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnExpiredShowHubAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/not-a-resource")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + localToken(Instant.now().minusSeconds(120))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsTokensWithGoogleIssuerEvenWhenTheyUseTheLocalSigningKey() throws Exception {
        mockMvc.perform(get("/api/v1/not-a-resource")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + signedToken(
                                        Instant.now().plusSeconds(60),
                                        "https://accounts.google.com",
                                        "google-client-id")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private String localToken(Instant expiresAt) {
        return signedToken(expiresAt, "https://showhub.local/user-service", "showhub-api");
    }

    private String signedToken(Instant expiresAt, String issuer, String audience) {
        Instant issuedAt = expiresAt.minusSeconds(300);
        byte[] key = Base64.getDecoder().decode("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(new SecretKeySpec(key, "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(java.util.List.of(audience))
                .subject("7e4d6c0a-d224-4796-91c1-47edb54d50b5")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", "ada@example.com")
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

}
