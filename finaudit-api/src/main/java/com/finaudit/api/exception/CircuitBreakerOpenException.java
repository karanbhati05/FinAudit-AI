package com.finaudit.api.exception;

public class CircuitBreakerOpenException extends RuntimeException {
    private final long retryAfterSeconds;

    public CircuitBreakerOpenException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
