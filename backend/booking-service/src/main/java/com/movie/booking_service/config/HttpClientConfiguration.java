package com.movie.booking_service.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {
    @Bean
    RestClient theaterRestClient(RestClient.Builder builder, InternalApiProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        return builder.baseUrl(properties.theaterServiceUrl())
                .requestFactory(requestFactory)
                .defaultHeader("X-ShowHub-Internal-Key", properties.apiKey())
                .build();
    }
}
