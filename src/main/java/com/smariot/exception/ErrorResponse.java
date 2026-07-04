package com.smariot.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Standard JSON error body returned by the GlobalExceptionHandler.
 */
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant timestamp;
    private final String error;
    private final String message;
    private final String path;

    public ErrorResponse(Instant timestamp, String error, String message, String path) {
        this.timestamp = timestamp;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}
