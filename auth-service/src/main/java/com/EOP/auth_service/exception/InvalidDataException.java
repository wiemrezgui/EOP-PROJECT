package com.EOP.auth_service.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidDataException extends AuthenticationException {
    public InvalidDataException(String msg) {
        super(msg);
    }
}
