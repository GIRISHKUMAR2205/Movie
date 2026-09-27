package com.movie.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.movie.user_service.entity.EmailVerificationProperties;
import com.movie.user_service.entity.RefreshTokenProperties;
import com.movie.user_service.entity.OutboxRelayProperties;
import com.movie.user_service.security.RefreshTokenCookieProperties;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties({
        EmailVerificationProperties.class,
        OutboxRelayProperties.class,
        RefreshTokenProperties.class,
        RefreshTokenCookieProperties.class
})
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}
}
