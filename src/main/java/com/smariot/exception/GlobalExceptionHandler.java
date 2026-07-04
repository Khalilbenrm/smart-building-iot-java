package com.smariot.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centralized REST exception handling, producing a consistent JSON error body:
 * { timestamp, error, message, path }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error("Validation failed on {}: {}", request.getRequestURI(), message);
        ErrorResponse body = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(InvalidSensorDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSensorData(InvalidSensorDataException ex,
                                                                   HttpServletRequest request) {
        log.error("Invalid sensor data on {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse body = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        ErrorResponse body = new ErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
