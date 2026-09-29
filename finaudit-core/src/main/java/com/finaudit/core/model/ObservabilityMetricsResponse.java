package com.finaudit.core.model;

public record ObservabilityMetricsResponse(
        long reportsProcessedToday,
        long failedReportsLast24h,
        double averageAuditCompletionTimeSeconds,
        int geminiCallsToday,
        int geminiDailyLimit,
        String circuitBreakerStatus,
        int activeConcurrencySlots,
        int maxConcurrencySlots,
        int maxUploadsPerUserDaily,
        int maxUploadsPerIpDaily,
        long systemUptimeSeconds
) {}
