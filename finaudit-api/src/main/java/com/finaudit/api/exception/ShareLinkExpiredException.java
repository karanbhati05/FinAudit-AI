package com.finaudit.api.exception;

public class ShareLinkExpiredException extends RuntimeException {
    public ShareLinkExpiredException(String message) {
        super(message);
    }
}
