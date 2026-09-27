package com.finaudit.core.model;

import java.time.Instant;

public record ReportSummaryDto(
        Long id,
        Long ownerId,
        String originalFilename,
        ReportStatus status,
        Instant uploadedAt,
        Instant auditedAt,
        Integer complianceScore,
        RiskLevel riskLevel
) {}
