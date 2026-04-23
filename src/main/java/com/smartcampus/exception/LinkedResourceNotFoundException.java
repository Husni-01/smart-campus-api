package com.smartcampus.exception;

/**
 * Thrown when a POST payload references a resource (e.g., a roomId) that does not exist.
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
