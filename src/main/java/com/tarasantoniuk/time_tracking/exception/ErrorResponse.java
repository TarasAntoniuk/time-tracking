package com.tarasantoniuk.time_tracking.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response format for all API exceptions.
 * Provides consistent error structure across the application.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * HTTP status code
     */
    private int status;

    /**
     * Error type/category (e.g., "NOT_FOUND", "VALIDATION_ERROR")
     */
    private String error;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Timestamp when the error occurred
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * API path where the error occurred
     */
    private String path;

    /**
     * Optional: List of validation errors (for @Valid failures)
     */
    private List<ValidationError> validationErrors;

    /**
     * Nested class for validation error details
     */
    @Data
    @Builder
    public static class ValidationError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}