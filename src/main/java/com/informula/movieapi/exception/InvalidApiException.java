package com.informula.movieapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidApiException extends RuntimeException {

    public InvalidApiException(String message) {
        super(message);
    }
}
