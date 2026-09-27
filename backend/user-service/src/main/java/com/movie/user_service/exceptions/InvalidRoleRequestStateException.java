package com.movie.user_service.exceptions;

public class InvalidRoleRequestStateException extends RuntimeException {
    public InvalidRoleRequestStateException(String message) {
        super(message);
    }
}
