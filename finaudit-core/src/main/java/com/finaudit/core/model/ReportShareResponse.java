package com.finaudit.core.model;

public record ReportShareResponse(
        Long reportId,
        String token,
        String shareUrl,
        long expiresInSeconds
) {}
