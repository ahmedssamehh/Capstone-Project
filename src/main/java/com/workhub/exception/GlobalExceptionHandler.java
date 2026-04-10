package com.workhub.exception;

import com.workhub.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler using @ControllerAdvice
 * 
 * Provides consistent JSON error responses across the application
 * 
 * Response format:
 * {
 *   "message": "Error message",
 *   "details": [...]
 * }
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid
     * 
     * Returns 400 Bad Request with validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        
        log.warn("Validation failed: {}", ex.getMessage());

        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Validation failed")
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle entity not found errors
     * 
     * Returns 404 Not Found
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            IllegalArgumentException ex) {
        
        log.warn("Entity not found or invalid argument: {}", ex.getMessage());

        // Check if it's a "not found or access denied" error
        if (ex.getMessage() != null && ex.getMessage().contains("not found or access denied")) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .message("Entity not found")
                    .details(List.of(ex.getMessage()))
                    .build();

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        // Check if it's a tenant mismatch error (unauthorized access)
        if (ex.getMessage() != null && 
            (ex.getMessage().contains("does not match current tenant") ||
             ex.getMessage().contains("does not belong to current tenant"))) {
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .message("Unauthorized access")
                    .details(List.of("Access denied to resource from different tenant"))
                    .build();

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        // Check if it's a duplicate key error
        if (ex.getMessage() != null && ex.getMessage().contains("already exists")) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .message("Duplicate resource")
                    .details(List.of(ex.getMessage()))
                    .build();

            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        // Generic bad request
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Invalid request")
                .details(List.of(ex.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle unauthorized access (tenant context not set)
     * 
     * Returns 401 Unauthorized
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
            IllegalStateException ex) {
        
        log.error("Unauthorized access: {}", ex.getMessage());

        // Check if it's a tenant context issue
        if (ex.getMessage() != null && ex.getMessage().contains("Tenant context is not set")) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .message("Unauthorized access")
                    .details(List.of("Authentication required", "Please provide valid JWT token"))
                    .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        // Generic bad request
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Invalid state")
                .details(List.of(ex.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle Spring Security authentication exceptions
     * 
     * Returns 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex) {
        
        log.error("Authentication failed: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Unauthorized access")
                .details(List.of("Authentication failed", ex.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle Spring Security access denied exceptions
     * 
     * Returns 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex) {
        
        log.error("Access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Unauthorized access")
                .details(List.of("Access denied", "Insufficient permissions"))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle all other unexpected exceptions
     * 
     * Returns 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex) {
        
        log.error("Unexpected error: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Internal server error")
                .details(List.of("An unexpected error occurred", "Please try again later"))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Format field error for validation response
     */
    private String formatFieldError(FieldError fieldError) {
        return String.format("%s: %s", fieldError.getField(), fieldError.getDefaultMessage());
    }
}

