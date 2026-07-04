package com.smariot.exception;

/**
 * Thrown when incoming sensor data fails business-level validation
 * (e.g. physically implausible temperature/humidity values).
 */
public class InvalidSensorDataException extends RuntimeException {

    public InvalidSensorDataException(String message) {
        super(message);
    }
}
