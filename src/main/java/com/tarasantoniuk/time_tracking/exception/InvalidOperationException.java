package com.tarasantoniuk.time_tracking.exception;

/**
 * Exception thrown when an operation cannot be performed due to business logic constraints.
 * Returns HTTP 422 (Unprocessable Entity) status code.
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }

    public InvalidOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}