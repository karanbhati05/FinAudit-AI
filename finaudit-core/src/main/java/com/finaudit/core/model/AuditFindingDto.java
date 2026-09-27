package com.finaudit.core.model;

public record AuditFindingDto(
        String ruleSource,
        String severity,
        String description,
        String policyReference,
        Long lineItemId
) {}
