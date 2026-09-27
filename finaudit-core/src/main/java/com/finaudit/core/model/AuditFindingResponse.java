package com.finaudit.core.model;

import java.time.Instant;

public record AuditFindingResponse(
        Long id,
        Long lineItemId,
        RuleSource ruleSource,
        FindingSeverity severity,
        String description,
        String policyReference,
        Instant createdAt
) {}
