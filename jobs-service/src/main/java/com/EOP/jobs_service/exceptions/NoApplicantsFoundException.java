package com.EOP.jobs_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoApplicantsFoundException extends RuntimeException {
    public NoApplicantsFoundException(String message) {
        super(message);
    }
}
