package com.movie.apigateway.error;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/internal/gateway-fallback")
public class GatewayFallbackController {

    @RequestMapping("/{serviceId}")
    ResponseEntity<ApiErrorResponse> downstreamUnavailable(
            @PathVariable String serviceId, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "DOWNSTREAM_UNAVAILABLE",
                        "The requested service is temporarily unavailable.",
                        request.getRequestURI()));
    }
}
