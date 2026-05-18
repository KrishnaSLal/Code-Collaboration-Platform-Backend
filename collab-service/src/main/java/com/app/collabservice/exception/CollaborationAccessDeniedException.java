package com.app.collabservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class CollaborationAccessDeniedException extends RuntimeException {
    public CollaborationAccessDeniedException(String message) {
        super(message);
    }
}
