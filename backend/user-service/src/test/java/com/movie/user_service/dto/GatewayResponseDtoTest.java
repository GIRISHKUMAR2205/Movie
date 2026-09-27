package com.movie.user_service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GatewayResponseDtoTest {

    @Test
    void successUsesTheHttpStatusAndPlacesPayloadInData() {
        GatewayResponseDto<String> response = GatewayResponseDto.success(
                HttpStatus.CREATED, "SIGNUP_SUCCESS", "Account created successfully.", "user-data");

        assertTrue(response.success());
        assertEquals(201, response.status());
        assertEquals("user-data", response.data());
        assertNull(response.errors());
    }

    @Test
    void failureNeverContainsDataAndCanExposeSafeFieldErrors() {
        GatewayResponseDto<Void> response = GatewayResponseDto.failure(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed.",
                Map.of("email", "must be a well-formed email address"));

        assertFalse(response.success());
        assertEquals(400, response.status());
        assertNull(response.data());
        assertEquals("must be a well-formed email address", response.errors().get("email"));
    }
}
