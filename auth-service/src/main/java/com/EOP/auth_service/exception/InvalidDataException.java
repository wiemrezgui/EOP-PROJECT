package com.EOP.auth_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDataException extends AuthenticationException {
    public InvalidDataException(String msg) {
        super(msg);
    }
}
