package com.movie.payment_service.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class BookingClientConfiguration {
    @Bean
    RestClient bookingRestClient(RestClient.Builder builder, InternalApiProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        return builder.baseUrl(properties.bookingServiceUrl())
                .requestFactory(requestFactory)
                .defaultHeader("X-ShowHub-Internal-Key", properties.apiKey())
                .build();
    }
}
