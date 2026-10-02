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
        Instant createdAt,
        boolean isHeuristic
) {
    public ResolvedFindingResponse(
            Long id,
            Long lineItemId,
            RuleSource ruleSource,
            FindingSeverity severity,
            String description,
            String policyReference,
            String policyTitle,
            String policyBodyText,
            Instant createdAt
    ) {
        this(
                id,
                lineItemId,
                ruleSource,
                severity,
                description,
                policyReference,
                policyTitle,
                policyBodyText,
                createdAt,
                isHeuristicFinding(policyReference, policyTitle)
        );
    }

    private static boolean isHeuristicFinding(String policyReference, String policyTitle) {
        if (policyReference == null || policyReference.isBlank() || policyReference.equalsIgnoreCase("null")) {
            return true;
        }
        String refLower = policyReference.toLowerCase();
        if (refLower.contains("no policy") || refLower.contains("heuristic") || refLower.contains("unmatched")) {
            return true;
        }
        return policyTitle == null || policyTitle.isBlank();
    }
}
