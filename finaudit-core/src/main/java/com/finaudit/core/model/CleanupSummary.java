package com.finaudit.core.model;

import java.time.Instant;

/**
 * Summary metrics of a scheduled or triggered storage hygiene and demo reset run.
 */
public record CleanupSummary(
        int expiredNonDemoReportsDeleted,
        int demoVisitorReportsDeleted,
        int canonicalReportsCount,
        long totalStorageBytesRemaining,
        int dailyGeminiCallsToday,
        Instant executedAt
) {}
