package com.finaudit.core.model;

import java.time.Instant;

public record ResolvedFindingResponse(
        Long id,
        Long lineItemId,
        RuleSource ruleSource,
        FindingSeverity severity,
        String description,
        String policyReference,
        String policyTitle,
        String policyBodyText,
        Instant createdAt
) {}
