package com.movie.user_service.exceptions;

import java.lang.RuntimeException;

public class AlreadyExistsException extends RuntimeException {
    public AlreadyExistsException(String message){
        super(message);
    }
}
