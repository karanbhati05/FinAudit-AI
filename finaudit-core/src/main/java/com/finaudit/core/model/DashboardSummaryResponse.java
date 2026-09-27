package com.finaudit.core.model;

import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
        long totalReportsAudited,
        double averageComplianceScore,
        Map<String, Long> countByRiskLevel,
        List<PolicyViolationCount> topViolations
) {
    public DashboardSummaryResponse {
        if (countByRiskLevel == null) {
            countByRiskLevel = Map.of();
        }
        if (topViolations == null) {
            topViolations = List.of();
        }
    }
}
