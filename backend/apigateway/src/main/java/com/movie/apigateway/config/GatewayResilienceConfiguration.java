package com.movie.apigateway.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4jBulkheadConfigurationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

@Configuration
@EnableConfigurationProperties(GatewayResilienceProperties.class)
public class GatewayResilienceConfiguration {

    @Bean(destroyMethod = "shutdown")
    @Qualifier("gatewayResilienceExecutor")
    ExecutorService gatewayResilienceExecutor(GatewayResilienceProperties properties) {
        return Executors.newFixedThreadPool(
                properties.maxConcurrentCalls(), new CustomizableThreadFactory("gateway-resilience-"));
    }

    @Bean
    GatewayResilienceConfigurer gatewayResilienceConfigurer(
            Resilience4JCircuitBreakerFactory circuitBreakerFactory,
            @Qualifier("gatewayResilienceExecutor") ExecutorService executorService,
            GatewayResilienceProperties properties) {
        circuitBreakerFactory.configureExecutorService(executorService);
        circuitBreakerFactory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(properties.slidingWindowSize())
                        .minimumNumberOfCalls(properties.minimumNumberOfCalls())
                        .failureRateThreshold(properties.failureRateThreshold())
                        .waitDurationInOpenState(properties.openStateDuration())
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(properties.timeout())
                        .cancelRunningFuture(true)
                        .build())
                .build());
        circuitBreakerFactory.getBulkheadProvider().configureDefault(id ->
                new Resilience4jBulkheadConfigurationBuilder()
                        .bulkheadConfig(BulkheadConfig.custom()
                                .maxConcurrentCalls(properties.maxConcurrentCalls())
                                .maxWaitDuration(properties.maxWaitDuration())
                                .build())
                        .build());
        return new GatewayResilienceConfigurer();
    }

    /**
     * Holds the factory configuration as a bean so Spring applies it before route filters
     * lazily create their circuit breakers.
     */
    static final class GatewayResilienceConfigurer {
    }
}
