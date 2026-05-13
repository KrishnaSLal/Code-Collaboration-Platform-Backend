package com.app.executionservice.exception;

public class ExecutionJobNotFoundException extends RuntimeException {
    public ExecutionJobNotFoundException(String message) {
        super(message);
    }
}