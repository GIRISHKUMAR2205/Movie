package com.movie.payment_service.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AuditingConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
