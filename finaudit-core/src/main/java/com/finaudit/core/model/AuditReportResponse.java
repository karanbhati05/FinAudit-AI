package com.finaudit.core.model;

import java.time.Instant;
import java.util.List;

public record AuditReportResponse(
        Long reportId,
        int complianceScore,
        RiskLevel riskLevel,
        Instant startedAt,
        Instant completedAt,
        String summary,
        List<AuditFindingResponse> findings
) {
    public AuditReportResponse {
        if (findings == null) {
            findings = List.of();
        }
    }
}
