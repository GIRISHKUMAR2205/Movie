package com.movie.user_service.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class SignupDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsPasswordConfirmationThatDoesNotMatch() {
        SignupDto request = validRequest();
        request.setConfirmPassword("different-password");

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("passwordConfirmationMatching")));
    }

    @Test
    void acceptsValidSignupRequest() {
        assertTrue(validator.validate(validRequest()).isEmpty());
    }

    private SignupDto validRequest() {
        SignupDto request = new SignupDto();
        request.setName("Ada Lovelace");
        request.setEmail("ada@example.com");
        request.setPassword("correct-horse-battery-staple");
        request.setConfirmPassword("correct-horse-battery-staple");
        return request;
    }
}
