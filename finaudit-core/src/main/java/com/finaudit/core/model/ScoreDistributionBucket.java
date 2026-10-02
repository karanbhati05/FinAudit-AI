package com.finaudit.core.model;

/**
 * Histogram bucket for compliance score distribution across audited reports.
 */
public record ScoreDistributionBucket(
        String rangeLabel,
        int minScore,
        int maxScore,
        long count,
        double percentage,
        String color
) {
    public ScoreDistributionBucket {
        if (rangeLabel == null) rangeLabel = "";
        if (color == null) color = "#6366f1";
    }
}
