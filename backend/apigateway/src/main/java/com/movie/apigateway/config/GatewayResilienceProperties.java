package com.movie.apigateway.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "showhub.gateway.resilience")
public record GatewayResilienceProperties(
        int rateLimitForPeriod,
        Duration rateLimitRefreshPeriod,
        int maxRateLimitKeys,
        int maxConcurrentCalls,
        Duration maxWaitDuration,
        int slidingWindowSize,
        int minimumNumberOfCalls,
        float failureRateThreshold,
        Duration openStateDuration,
        Duration timeout,
        int retryAttempts) {
}
