package com.EOP.auth_service.exceptions;

import org.springframework.security.core.AuthenticationException;

public class UserNotFoundException extends AuthenticationException {
    public UserNotFoundException(String msg) {
        super(msg);
    }
}