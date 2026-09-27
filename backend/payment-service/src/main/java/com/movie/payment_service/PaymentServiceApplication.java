package com.movie.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.movie.payment_service.config.InternalApiProperties;
import com.movie.payment_service.config.PaymentOutboxProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({ InternalApiProperties.class, PaymentOutboxProperties.class })
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
