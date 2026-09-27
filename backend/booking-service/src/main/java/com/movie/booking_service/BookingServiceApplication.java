package com.movie.booking_service;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.movie.booking_service.config.BookingProperties;
import com.movie.booking_service.config.InternalApiProperties;
import com.movie.booking_service.config.JwtProperties;
import com.movie.booking_service.config.PaymentEventProperties;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties({ BookingProperties.class, InternalApiProperties.class,
        JwtProperties.class, PaymentEventProperties.class })
public class BookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
