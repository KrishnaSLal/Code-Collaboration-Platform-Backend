package com.app.versionservice.exception;

public class SnapshotNotFoundException extends RuntimeException {
    public SnapshotNotFoundException(String message) {
        super(message);
    }
}