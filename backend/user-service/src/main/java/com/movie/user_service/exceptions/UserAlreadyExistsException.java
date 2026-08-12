package com.movie.user_service.exceptions;

import java.lang.RuntimeException;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message){
        super(message);
    }
}
