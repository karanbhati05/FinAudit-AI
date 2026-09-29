package com.finaudit.api.exception;

public class CapacityExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public CapacityExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
