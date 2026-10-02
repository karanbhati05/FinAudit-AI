package com.finaudit.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
        List<ResolvedFindingResponse> findings,
        int totalLineItemCount,
        int flaggedLineItemCount,
        BigDecimal totalSpend,
        BigDecimal totalFlaggedAmount,
        String flaggedSummary,
        List<CategorySpendDto> categorySpend
) {
    public ReportDetailResponse {
        if (lineItems == null) {
            lineItems = List.of();
        }
        if (findings == null) {
            findings = List.of();
        }
        if (categorySpend == null) {
            categorySpend = List.of();
        }
        if (totalSpend == null) {
            totalSpend = BigDecimal.ZERO;
        }
        if (totalFlaggedAmount == null) {
            totalFlaggedAmount = BigDecimal.ZERO;
        }
        if (flaggedSummary == null) {
            flaggedSummary = String.format(Locale.US, "%d of %d line items flagged, $%,.2f total flagged amount",
                    flaggedLineItemCount, totalLineItemCount, totalFlaggedAmount);
        }
    }

    public ReportDetailResponse(
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
        this(
                id,
                ownerId,
                originalFilename,
                storagePath,
                status,
                uploadedAt,
                auditedAt,
                errorReason,
                complianceScore,
                riskLevel,
                auditSummary,
                lineItems,
                findings,
                computeTotalLineItemCount(lineItems),
                computeFlaggedLineItemCount(lineItems, findings),
                computeTotalSpend(lineItems),
                computeTotalFlaggedAmount(lineItems, findings),
                computeFlaggedSummary(lineItems, findings),
                computeCategorySpend(lineItems)
        );
    }

    public static int computeTotalLineItemCount(List<ReportLineItemDto> lineItems) {
        return lineItems != null ? lineItems.size() : 0;
    }

    public static int computeFlaggedLineItemCount(List<ReportLineItemDto> lineItems, List<ResolvedFindingResponse> findings) {
        if (findings == null || findings.isEmpty()) {
            return 0;
        }
        Set<Long> flaggedIds = findings.stream()
                .map(ResolvedFindingResponse::lineItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (lineItems != null && !lineItems.isEmpty()) {
            int count = (int) lineItems.stream()
                    .filter(item -> item.id() != null && flaggedIds.contains(item.id()))
                    .count();
            return Math.max(count, Math.min(flaggedIds.size(), lineItems.size()));
        }
        return flaggedIds.size();
    }

    public static BigDecimal computeTotalSpend(List<ReportLineItemDto> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return lineItems.stream()
                .map(ReportLineItemDto::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal computeTotalFlaggedAmount(List<ReportLineItemDto> lineItems, List<ResolvedFindingResponse> findings) {
        if (lineItems == null || lineItems.isEmpty() || findings == null || findings.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Set<Long> flaggedIds = findings.stream()
                .map(ResolvedFindingResponse::lineItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return lineItems.stream()
                .filter(item -> item.id() != null && flaggedIds.contains(item.id()))
                .map(ReportLineItemDto::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static String computeFlaggedSummary(List<ReportLineItemDto> lineItems, List<ResolvedFindingResponse> findings) {
        int flagged = computeFlaggedLineItemCount(lineItems, findings);
        int total = computeTotalLineItemCount(lineItems);
        BigDecimal flaggedAmt = computeTotalFlaggedAmount(lineItems, findings);
        return String.format(Locale.US, "%d of %d line items flagged, $%,.2f total flagged amount",
                flagged, total, flaggedAmt);
    }

    public static List<CategorySpendDto> computeCategorySpend(List<ReportLineItemDto> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            return List.of();
        }
        BigDecimal totalSpend = computeTotalSpend(lineItems);

        Map<String, List<ReportLineItemDto>> grouped = lineItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    String cat = item.category();
                    if (cat == null || cat.trim().isEmpty()) {
                        return "Uncategorized";
                    }
                    return cat.trim();
                }, LinkedHashMap::new, Collectors.toList()));

        List<CategorySpendDto> list = new ArrayList<>();
        for (Map.Entry<String, List<ReportLineItemDto>> entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<ReportLineItemDto> items = entry.getValue();
            BigDecimal amount = items.stream()
                    .map(ReportLineItemDto::amount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            double percentage = 0.0;
            if (totalSpend.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalSpend, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            list.add(new CategorySpendDto(category, amount, items.size(), percentage));
        }

        list.sort(Comparator.comparing(CategorySpendDto::amount).reversed());
        return Collections.unmodifiableList(list);
    }
}
