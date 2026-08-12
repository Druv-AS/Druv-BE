package com.dhruv.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Single place where exceptions become HTTP responses.
 *
 * <p>Replaces per-controller try/catch blocks that returned {@code 400} for every failure —
 * including genuine server errors — and echoed raw exception messages (and therefore
 * driver, schema, and stack details) back to the caller.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex) {
        // Expected, caller-facing failures: log at debug, no stack trace noise.
        log.debug("API error {}: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                fields.putIfAbsent(err.getField(),
                        err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid value"));
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("VALIDATION_FAILED", "Please correct the highlighted fields.", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("MALFORMED_REQUEST", "Request body could not be parsed."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("MISSING_PARAMETER", "Missing required parameter: " + ex.getParameterName()));
    }

    /**
     * A unique-constraint race — two concurrent registrations for the same phone or user id.
     * The service-level pre-check cannot close this window, so the database is the arbiter.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("ACCOUNT_ALREADY_EXISTS",
                        "An account already exists with this mobile number or user ID."));
    }

    /** Thrown by @PreAuthorize; the filter-chain equivalent is handled in SecurityConfig. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("FORBIDDEN", "You do not have access to this resource."));
    }

    /**
     * Anything unanticipated is a 500. The client gets a correlation id only — the detail
     * goes to the server log, never over the wire.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled exception [errorId={}]", errorId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_ERROR",
                        "Something went wrong. Quote reference " + errorId + " if this persists."));
    }
}
