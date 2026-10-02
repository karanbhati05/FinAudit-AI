package com.finaudit.core.model;

/**
 * Data point for the spend and risk trend time-series chart.
 * Represents aggregated spend and risk-level breakdown for a time bucket (day, week, or month).
 */
public record SpendRiskTrendPoint(
        String label,
        String date,
        double totalSpend,
        long lowRisk,
        long mediumRisk,
        long highRisk,
        long reportCount
) {
    public SpendRiskTrendPoint {
        if (label == null) label = "";
        if (date == null) date = "";
    }
}
