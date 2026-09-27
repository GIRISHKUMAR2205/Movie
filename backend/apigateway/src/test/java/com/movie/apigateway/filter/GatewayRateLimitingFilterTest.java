package com.movie.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.movie.apigateway.config.GatewayResilienceProperties;

import tools.jackson.databind.ObjectMapper;

class GatewayRateLimitingFilterTest {

    @Test
    void rejectsRequestsBeyondTheCallerLimit() throws Exception {
        GatewayRateLimitingFilter filter = new GatewayRateLimitingFilter(
                new GatewayResilienceProperties(
                        1, Duration.ofMinutes(1), 10, 2, Duration.ZERO,
                        10, 5, 50, Duration.ofSeconds(30), Duration.ofSeconds(5), 1),
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/movies/42");

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(request, firstResponse, new MockFilterChain());

        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        filter.doFilter(request, limitedResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(limitedResponse.getStatus()).isEqualTo(429);
        assertThat(limitedResponse.getHeader("Retry-After")).isEqualTo("60");
        assertThat(limitedResponse.getContentAsString()).contains("RATE_LIMITED");
    }
}
