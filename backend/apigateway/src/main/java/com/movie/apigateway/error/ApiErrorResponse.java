package com.movie.apigateway.error;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path) {
}
