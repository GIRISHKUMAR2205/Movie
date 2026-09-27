package com.movie.user_service.dto;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatusCode;

/**
 * Stable response envelope forwarded unchanged by the API gateway.
 * Tokens are intentionally never included in this response body.
 */
public record GatewayResponseDto<T>(
        Instant timestamp,
        int status,
        boolean success,
        String code,
        String message,
        T data,
        Map<String, String> errors) {

    public static <T> GatewayResponseDto<T> success(
            HttpStatusCode status, String code, String message, T data) {
        return new GatewayResponseDto<>(Instant.now(), status.value(), true, code, message, data, null);
    }

    public static GatewayResponseDto<Void> failure(
            HttpStatusCode status, String code, String message) {
        return failure(status, code, message, null);
    }

    public static GatewayResponseDto<Void> failure(
            HttpStatusCode status, String code, String message, Map<String, String> errors) {
        return new GatewayResponseDto<>(Instant.now(), status.value(), false, code, message, null, errors);
    }
}
