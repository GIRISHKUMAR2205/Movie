package com.movie.apigateway.filter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.movie.apigateway.config.GatewayResilienceProperties;
import com.movie.apigateway.error.ApiErrorResponse;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class GatewayRateLimitingFilter extends OncePerRequestFilter {

    private final GatewayResilienceProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, RateLimiter> rateLimiters = new LinkedHashMap<>(16, 0.75f, true);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!rateLimiterFor(request).acquirePermission()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds()));
            objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                    Instant.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "RATE_LIMITED",
                    "Too many requests. Please try again shortly.",
                    request.getRequestURI()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private RateLimiter rateLimiterFor(HttpServletRequest request) {
        String key = rateLimitKey(request);
        synchronized (rateLimiters) {
            return rateLimiters.computeIfAbsent(key, ignored -> {
                evictOldestRateLimiterIfNeeded();
                return RateLimiter.of("gateway-" + Integer.toUnsignedString(key.hashCode()), rateLimiterConfig());
            });
        }
    }

    private void evictOldestRateLimiterIfNeeded() {
        if (rateLimiters.size() < properties.maxRateLimitKeys()) {
            return;
        }
        String oldestKey = rateLimiters.keySet().iterator().next();
        rateLimiters.remove(oldestKey);
    }

    private String rateLimitKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "subject:" + authentication.getName();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private RateLimiterConfig rateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(properties.rateLimitForPeriod())
                .limitRefreshPeriod(properties.rateLimitRefreshPeriod())
                .timeoutDuration(Duration.ZERO)
                .build();
    }

    private long retryAfterSeconds() {
        return Math.max(1, properties.rateLimitRefreshPeriod().toSeconds());
    }
}
