package com.EOP.auth_service.exception;

import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserExistsException extends AuthenticationException {
    public UserExistsException(String message) {
        super(message);
    }
}
