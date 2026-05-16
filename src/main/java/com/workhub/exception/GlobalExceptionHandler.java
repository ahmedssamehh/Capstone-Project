package com.workhub.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        return build(HttpStatus.BAD_REQUEST, "Validation failed", details, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null, request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(
            DuplicateResourceException ex,
            WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(
            ResponseStatusException ex,
            WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return build(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(), null, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {
        log.warn("Invalid request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Invalid request", null, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleUnauthorizedAccess(
            IllegalStateException ex,
            WebRequest request) {
        if (ex.getMessage() != null && ex.getMessage().contains("Tenant context is not set")) {
            return build(HttpStatus.UNAUTHORIZED, "Authentication required", null, request);
        }
        return build(HttpStatus.BAD_REQUEST, "Invalid state", null, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed", null, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied", null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(
            Exception ex,
            WebRequest request) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", null, request);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            List<String> details,
            WebRequest request
    ) {
        String path = null;
        String correlationId = null;
        if (request instanceof ServletWebRequest servletWebRequest) {
            path = servletWebRequest.getRequest().getRequestURI();
            Object attribute = servletWebRequest.getRequest().getAttribute("correlationId");
            correlationId = attribute != null ? attribute.toString() : null;
        }

        ApiError errorResponse = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .details(details)
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }

    private String formatFieldError(FieldError fieldError) {
        return String.format("%s: %s", fieldError.getField(), fieldError.getDefaultMessage());
    }
}

