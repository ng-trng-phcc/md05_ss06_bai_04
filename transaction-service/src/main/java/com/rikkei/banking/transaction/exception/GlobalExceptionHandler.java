package com.rikkei.banking.transaction.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request", request);
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ApiError> handleInvalidTransfer(InvalidTransferException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(SourceAccountNotFoundException.class)
    public ResponseEntity<ApiError> handleSourceAccountNotFound(SourceAccountNotFoundException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(UpstreamServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleUnavailable(UpstreamServiceUnavailableException ex,
                                                      HttpServletRequest request) {
        log.error("Upstream account-service unavailable: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    public ResponseEntity<ApiError> handleTimeout(UpstreamTimeoutException ex, HttpServletRequest request) {
        log.error("Upstream account-service timeout: {}", ex.getMessage());
        return build(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage(), request);
    }

    @ExceptionHandler(UpstreamFailureException.class)
    public ResponseEntity<ApiError> handleUpstreamFailure(UpstreamFailureException ex, HttpServletRequest request) {
        log.error("Unexpected upstream failure: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message,
                        request.getRequestURI(), MDC.get("requestId")));
    }
}
