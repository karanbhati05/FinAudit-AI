package com.finaudit.core.model;

public record PolicyViolationCount(
        String policyReference,
        long count
) {}
