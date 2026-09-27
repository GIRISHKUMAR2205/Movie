package com.movie.mail_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.movie.mail_service.config.MailProperties;

@SpringBootApplication
@EnableConfigurationProperties(MailProperties.class)
public class MailServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailServiceApplication.class, args);
    }
}
