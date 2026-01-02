package com.yash.fineshyttt.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Global exception handler aligned with API_CONTRACTS.md error format.
 * Never exposes stack traces to clients.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =========================
    // NOT FOUND (404)
    // =========================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found: {} at {}", ex.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(404)
                        .error("NOT_FOUND")
                        .code("RESOURCE_NOT_FOUND")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .requestId(getRequestId(request))
                        .build());
    }

    // =========================
    // AUTHENTICATION (401)
    // =========================
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication failed: {} from IP {}",
                ex.getMessage(), request.getRemoteAddr());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(401)
                        .error("UNAUTHORIZED")
                        .code("AUTH_FAILED")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .requestId(getRequestId(request))
                        .build());
    }

    // =========================
    // FORBIDDEN (403)
    // =========================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied: {} at {}", ex.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(403)
                        .error("FORBIDDEN")
                        .code("ACCESS_DENIED")
                        .message("You do not have permission to access this resource")
                        .path(request.getRequestURI())
                        .requestId(getRequestId(request))
                        .build());
    }

    // =========================
    // VALIDATION (400)
    // =========================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ValidationError(
                        err.getField(),
                        err.getDefaultMessage(),
                        err.getRejectedValue()
                ))
                .toList();

        log.warn("Validation failed at {}: {} errors", request.getRequestURI(), errors.size());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(400)
                        .error("BAD_REQUEST")
                        .code("VALIDATION_ERROR")
                        .message("Validation failed")
                        .errors(errors)
                        .path(request.getRequestURI())
                        .requestId(getRequestId(request))
                        .build());
    }

    // =========================
    // CONSTRAINT VIOLATION (400)
    // =========================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ValidationError> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getInvalidValue()
                ))
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(400)
                        .error("BAD_REQUEST")
                        .code("CONSTRAINT_VIOLATION")
                        .message("Constraint violation")
                        .errors(errors)
                        .path(request.getRequestURI())
                        .requestId(getRequestId(request))
                        .build());
    }

    // =========================
    // ILLEGAL ARGUMENT (400)
    // =========================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Illegal argument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(400)
                        .error("BAD_REQUEST")
                        .code("INVALID_ARGUMENT")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .requestId(getRequestId(request))
                        .build());
    }

    // =========================
    // GENERIC ERROR (500)
    // =========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(500)
                        .error("INTERNAL_SERVER_ERROR")
                        .code("INTERNAL_ERROR")
                        .message("An unexpected error occurred")
                        .path(request.getRequestURI())
                        .requestId(getRequestId(request))
                        .build());
    }

    // =========================
    // HELPERS
    // =========================
    private String getRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        return (requestId != null && !requestId.isBlank())
                ? requestId
                : UUID.randomUUID().toString();
    }
}
