package com.finaudit.core.model;

import java.util.List;

public record AuditReportResult(
        int complianceScore,
        String riskLevel,
        List<AuditFindingDto> flaggedItems,
        String summary
) {
    public AuditReportResult {
        if (flaggedItems == null) {
            flaggedItems = List.of();
        }
    }
}
