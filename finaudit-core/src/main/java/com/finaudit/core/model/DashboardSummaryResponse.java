package com.finaudit.core.model;

import java.util.List;
import java.util.Map;

/**
 * Aggregated portfolio-level audit intelligence response.
 * Includes top-line stats, risk distribution, spend/risk trend line,
 * compliance score histogram, and governing policy violation leaderboard.
 */
public record DashboardSummaryResponse(
        long totalReportsAudited,
        double averageComplianceScore,
        double totalSpendAudited,
        Map<String, Long> countByRiskLevel,
        List<PolicyViolationCount> topViolations,
        List<SpendRiskTrendPoint> spendAndRiskTrend,
        List<ScoreDistributionBucket> complianceScoreDistribution,
        String dateRange
) {
    public DashboardSummaryResponse {
        if (countByRiskLevel == null) {
            countByRiskLevel = Map.of();
        }
        if (topViolations == null) {
            topViolations = List.of();
        }
        if (spendAndRiskTrend == null) {
            spendAndRiskTrend = List.of();
        }
        if (complianceScoreDistribution == null) {
            complianceScoreDistribution = List.of();
        }
        if (dateRange == null) {
            dateRange = "30d";
        }
    }

    /**
     * Backwards-compatible constructor for existing tests and callers.
     */
    public DashboardSummaryResponse(
            long totalReportsAudited,
            double averageComplianceScore,
            Map<String, Long> countByRiskLevel,
            List<PolicyViolationCount> topViolations
    ) {
        this(totalReportsAudited, averageComplianceScore, 0.0, countByRiskLevel, topViolations, List.of(), List.of(), "all");
    }
}
