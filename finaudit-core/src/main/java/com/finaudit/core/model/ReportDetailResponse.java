package com.finaudit.core.model;

import java.time.Instant;
import java.util.List;

public record ReportDetailResponse(
        Long id,
        Long ownerId,
        String originalFilename,
        String storagePath,
        ReportStatus status,
        Instant uploadedAt,
        Instant auditedAt,
        String errorReason,
        Integer complianceScore,
        RiskLevel riskLevel,
        String auditSummary,
        List<ReportLineItemDto> lineItems,
        List<ResolvedFindingResponse> findings
) {
    public ReportDetailResponse {
        if (lineItems == null) {
            lineItems = List.of();
        }
        if (findings == null) {
            findings = List.of();
        }
    }
}
