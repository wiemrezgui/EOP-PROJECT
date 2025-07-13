package com.EOP.auth_service.exceptions;

import org.springframework.security.core.AuthenticationException;

public class UserExistsException extends AuthenticationException {
    public UserExistsException(String message) {
        super(message);
    }
}
