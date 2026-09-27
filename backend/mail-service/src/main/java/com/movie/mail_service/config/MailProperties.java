package com.movie.mail_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "showhub.mail")
public record MailProperties(
        @NotBlank String exchange,
        @NotBlank String routingKey,
        @NotBlank String queue,
        @NotBlank String deadLetterExchange,
        @NotBlank String deadLetterQueue,
        @NotBlank String from) {
}
