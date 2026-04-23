package com.smartcampus.exception;

/**
 * Thrown when a POST reading is attempted on a sensor with "MAINTENANCE" or "OFFLINE" status.
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
