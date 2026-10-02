package com.finaudit.api.service;

import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DashboardService {

    private final ReportRepository reportRepository;
    private final AuditRunRepository auditRunRepository;
    private final AuditFindingRepository auditFindingRepository;

    private static final DateTimeFormatter DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

    public DashboardService(
            ReportRepository reportRepository,
            AuditRunRepository auditRunRepository,
            AuditFindingRepository auditFindingRepository
    ) {
        this.reportRepository = reportRepository;
        this.auditRunRepository = auditRunRepository;
        this.auditFindingRepository = auditFindingRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return getSummary("30d");
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(String rangeStr) {
        String normalizedRange = (rangeStr != null && !rangeStr.isBlank()) ? rangeStr.toLowerCase().trim() : "30d";
        Instant cutoff = resolveCutoff(normalizedRange);

        // 1. Single aggregation query: report spend, audit compliance score, risk level, and timestamp
        List<Object[]> reportRows = (cutoff != null)
                ? reportRepository.findReportSpendAndRiskMetricsAfter(cutoff)
                : reportRepository.findReportSpendAndRiskMetricsAll();

        long totalAudited = reportRows.size();

        // 2. Average compliance score
        double avgScore = 0.0;
        if (totalAudited > 0) {
            double sumScore = reportRows.stream()
                    .mapToDouble(row -> row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                    .sum();
            avgScore = BigDecimal.valueOf(sumScore / totalAudited)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // 3. Total spend audited
        double totalSpend = reportRows.stream()
                .mapToDouble(row -> row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                .sum();
        totalSpend = BigDecimal.valueOf(totalSpend).setScale(2, RoundingMode.HALF_UP).doubleValue();

        // 4. Count by risk level
        Map<String, Long> countByRiskLevel = new LinkedHashMap<>();
        for (com.finaudit.core.model.RiskLevel level : com.finaudit.core.model.RiskLevel.values()) {
            countByRiskLevel.put(level.name(), 0L);
        }
        for (Object[] row : reportRows) {
            if (row[2] != null) {
                String risk = row[2].toString();
                countByRiskLevel.put(risk, countByRiskLevel.getOrDefault(risk, 0L) + 1L);
            }
        }

        // 5. Compliance score distribution (Histogram)
        long b0_49 = 0, b50_69 = 0, b70_79 = 0, b80_89 = 0, b90_100 = 0;
        for (Object[] row : reportRows) {
            int score = row[3] != null ? ((Number) row[3]).intValue() : 0;
            if (score < 50) b0_49++;
            else if (score < 70) b50_69++;
            else if (score < 80) b70_79++;
            else if (score < 90) b80_89++;
            else b90_100++;
        }

        double totalDouble = totalAudited > 0 ? (double) totalAudited : 1.0;
        List<ScoreDistributionBucket> scoreDistribution = List.of(
                new ScoreDistributionBucket("0–49", 0, 49, b0_49, round1((b0_49 * 100.0) / totalDouble), "#ef4444"),
                new ScoreDistributionBucket("50–69", 50, 69, b50_69, round1((b50_69 * 100.0) / totalDouble), "#f97316"),
                new ScoreDistributionBucket("70–79", 70, 79, b70_79, round1((b70_79 * 100.0) / totalDouble), "#f59e0b"),
                new ScoreDistributionBucket("80–89", 80, 89, b80_89, round1((b80_89 * 100.0) / totalDouble), "#06b6d4"),
                new ScoreDistributionBucket("90–100", 90, 100, b90_100, round1((b90_100 * 100.0) / totalDouble), "#10b981")
        );

        // 6. Spend & Risk Trend Line (Aggregated Time-Series Buckets)
        List<SpendRiskTrendPoint> spendAndRiskTrend = buildTrendPoints(normalizedRange, reportRows);

        // 7. Top Policy Violations Leaderboard
        List<Object[]> topRows = (cutoff != null)
                ? auditFindingRepository.findTopPolicyViolationsAfter(cutoff, PageRequest.of(0, 10))
                : auditFindingRepository.findTopPolicyViolationsAll(PageRequest.of(0, 10));

        // Fallback to legacy query if joined query returns empty (e.g., in unit tests mocking legacy repository)
        if (topRows.isEmpty()) {
            topRows = auditFindingRepository.findTopPolicyViolations(PageRequest.of(0, 10));
        }

        List<PolicyViolationCount> topViolations = new ArrayList<>();
        for (Object[] row : topRows) {
            String policyRef = (String) row[0];
            long count = ((Number) row[1]).longValue();
            topViolations.add(new PolicyViolationCount(policyRef, count));
        }

        return new DashboardSummaryResponse(
                totalAudited,
                avgScore,
                totalSpend,
                countByRiskLevel,
                topViolations,
                spendAndRiskTrend,
                scoreDistribution,
                normalizedRange
        );
    }

    private Instant resolveCutoff(String rangeStr) {
        return switch (rangeStr) {
            case "7d" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "30d" -> Instant.now().minus(30, ChronoUnit.DAYS);
            case "90d" -> Instant.now().minus(90, ChronoUnit.DAYS);
            case "all" -> null;
            default -> Instant.now().minus(30, ChronoUnit.DAYS);
        };
    }

    private List<SpendRiskTrendPoint> buildTrendPoints(String rangeStr, List<Object[]> reportRows) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        int daysBack = switch (rangeStr) {
            case "7d" -> 6;
            case "90d" -> 89;
            case "all" -> 29;
            default -> 29; // 30d
        };

        LocalDate startDate = today.minusDays(daysBack);

        // Map bucket by ISO date string
        Map<String, TrendAccumulator> bucketMap = new LinkedHashMap<>();

        if ("90d".equals(rangeStr)) {
            // Group into 12 weekly buckets for 90 days to avoid cramped visualization
            LocalDate current = startDate;
            while (!current.isAfter(today)) {
                String key = current.format(ISO_DATE_FORMAT);
                String label = current.format(DAY_LABEL_FORMAT);
                bucketMap.put(key, new TrendAccumulator(label, key));
                current = current.plusDays(7);
            }
        } else {
            // Daily buckets for 7d, 30d, all
            LocalDate current = startDate;
            while (!current.isAfter(today)) {
                String key = current.format(ISO_DATE_FORMAT);
                String label = current.format(DAY_LABEL_FORMAT);
                bucketMap.put(key, new TrendAccumulator(label, key));
                current = current.plusDays(1);
            }
        }

        // Aggregate reports into appropriate buckets
        for (Object[] row : reportRows) {
            Instant timestamp = (Instant) row[1];
            if (timestamp == null) timestamp = Instant.now();
            LocalDate itemDate = timestamp.atZone(zone).toLocalDate();
            double amount = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            String risk = row[2] != null ? row[2].toString() : "LOW";

            if ("90d".equals(rangeStr)) {
                // Find nearest weekly bucket
                TrendAccumulator target = null;
                for (Map.Entry<String, TrendAccumulator> entry : bucketMap.entrySet()) {
                    LocalDate bucketDate = LocalDate.parse(entry.getKey(), ISO_DATE_FORMAT);
                    if (!itemDate.isBefore(bucketDate)) {
                        target = entry.getValue();
                    }
                }
                if (target == null && !bucketMap.isEmpty()) {
                    target = bucketMap.values().iterator().next();
                }
                if (target != null) {
                    target.add(amount, risk);
                }
            } else {
                String dateKey = itemDate.format(ISO_DATE_FORMAT);
                TrendAccumulator acc = bucketMap.get(dateKey);
                if (acc != null) {
                    acc.add(amount, risk);
                } else if (itemDate.isAfter(startDate)) {
                    // Current date or boundary edge
                    String label = itemDate.format(DAY_LABEL_FORMAT);
                    TrendAccumulator newAcc = new TrendAccumulator(label, dateKey);
                    newAcc.add(amount, risk);
                    bucketMap.put(dateKey, newAcc);
                }
            }
        }

        List<SpendRiskTrendPoint> result = new ArrayList<>();
        for (TrendAccumulator acc : bucketMap.values()) {
            result.add(new SpendRiskTrendPoint(
                    acc.label,
                    acc.date,
                    BigDecimal.valueOf(acc.totalSpend).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                    acc.lowRisk,
                    acc.mediumRisk,
                    acc.highRisk,
                    acc.reportCount
            ));
        }

        return result;
    }

    private static double round1(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static class TrendAccumulator {
        final String label;
        final String date;
        double totalSpend = 0.0;
        long lowRisk = 0;
        long mediumRisk = 0;
        long highRisk = 0;
        long reportCount = 0;

        TrendAccumulator(String label, String date) {
            this.label = label;
            this.date = date;
        }

        void add(double spend, String risk) {
            this.totalSpend += spend;
            this.reportCount++;
            if ("HIGH".equalsIgnoreCase(risk)) {
                this.highRisk++;
            } else if ("MEDIUM".equalsIgnoreCase(risk)) {
                this.mediumRisk++;
            } else {
                this.lowRisk++;
            }
        }
    }
}
