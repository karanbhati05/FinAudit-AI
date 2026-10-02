package com.finaudit.api.service;

import com.finaudit.core.model.RiskLevel;
import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.DashboardSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private AuditRunRepository auditRunRepository;

    @Mock
    private AuditFindingRepository auditFindingRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("getSummary() should calculate metrics, risk breakdown, score distribution, and spend trend accurately")
    void shouldCalculateSummaryMetrics() {
        // Setup 4 mock report rows simulating the 4 demo reports
        List<Object[]> reportRows = new ArrayList<>();
        // row: [id, timestamp, riskLevel, complianceScore, spendAmount]
        Instant now = Instant.now();
        reportRows.add(new Object[]{101L, now.minus(2, ChronoUnit.HOURS), RiskLevel.MEDIUM, 72, new BigDecimal("5975.00")});
        reportRows.add(new Object[]{102L, now.minus(5, ChronoUnit.HOURS), RiskLevel.HIGH, 35, new BigDecimal("47420.00")});
        reportRows.add(new Object[]{103L, now.minus(1, ChronoUnit.DAYS), RiskLevel.LOW, 96, new BigDecimal("12900.00")});
        reportRows.add(new Object[]{104L, now.minus(2, ChronoUnit.DAYS), RiskLevel.HIGH, 58, new BigDecimal("8470.00")});

        when(reportRepository.findReportSpendAndRiskMetricsAfter(any(Instant.class))).thenReturn(reportRows);

        List<Object[]> topRows = new ArrayList<>();
        topRows.add(new Object[]{"Clause 4.1: Air Travel Class", 3L});
        topRows.add(new Object[]{"Clause 1.1: Duplicate Detection", 2L});
        when(auditFindingRepository.findTopPolicyViolationsAfter(any(Instant.class), any(Pageable.class))).thenReturn(topRows);

        DashboardSummaryResponse summary = dashboardService.getSummary("30d");

        assertThat(summary.totalReportsAudited()).isEqualTo(4L);
        // Average of 72, 35, 96, 58 = 261 / 4 = 65.25 -> 65.3
        assertThat(summary.averageComplianceScore()).isEqualTo(65.3);
        // Total spend: 5975 + 47420 + 12900 + 8470 = 74765.00
        assertThat(summary.totalSpendAudited()).isEqualTo(74765.00);

        // Risk breakdown
        assertThat(summary.countByRiskLevel().get("LOW")).isEqualTo(1L);
        assertThat(summary.countByRiskLevel().get("MEDIUM")).isEqualTo(1L);
        assertThat(summary.countByRiskLevel().get("HIGH")).isEqualTo(2L);

        // Score distribution histogram (5 tiers)
        assertThat(summary.complianceScoreDistribution()).hasSize(5);
        // 0-49: score 35 -> 1
        assertThat(summary.complianceScoreDistribution().get(0).count()).isEqualTo(1L);
        // 50-69: score 58 -> 1
        assertThat(summary.complianceScoreDistribution().get(1).count()).isEqualTo(1L);
        // 70-79: score 72 -> 1
        assertThat(summary.complianceScoreDistribution().get(2).count()).isEqualTo(1L);
        // 80-89: none -> 0
        assertThat(summary.complianceScoreDistribution().get(3).count()).isEqualTo(0L);
        // 90-100: score 96 -> 1
        assertThat(summary.complianceScoreDistribution().get(4).count()).isEqualTo(1L);

        // Spend and risk trend line
        assertThat(summary.spendAndRiskTrend()).isNotEmpty();
        assertThat(summary.dateRange()).isEqualTo("30d");

        // Top policy leaderboard
        assertThat(summary.topViolations()).hasSize(2);
        assertThat(summary.topViolations().get(0).policyReference()).isEqualTo("Clause 4.1: Air Travel Class");
        assertThat(summary.topViolations().get(0).count()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getSummary('7d') should generate 7 daily buckets for spend and risk trend")
    void shouldFilterByDateRange7Days() {
        when(reportRepository.findReportSpendAndRiskMetricsAfter(any(Instant.class))).thenReturn(List.of());
        when(auditFindingRepository.findTopPolicyViolationsAfter(any(Instant.class), any(Pageable.class))).thenReturn(List.of());

        DashboardSummaryResponse summary = dashboardService.getSummary("7d");

        assertThat(summary.dateRange()).isEqualTo("7d");
        assertThat(summary.totalReportsAudited()).isEqualTo(0L);
        assertThat(summary.totalSpendAudited()).isEqualTo(0.0);
        // 7 daily points generated even with 0 reports, ensuring charts render cleanly
        assertThat(summary.spendAndRiskTrend()).hasSize(7);
        assertThat(summary.spendAndRiskTrend().get(0).totalSpend()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Empty state should render non-null collections and zeroed metrics without crashing")
    void shouldHandleEmptyDataSensibly() {
        when(reportRepository.findReportSpendAndRiskMetricsAll()).thenReturn(List.of());
        when(auditFindingRepository.findTopPolicyViolationsAll(any(Pageable.class))).thenReturn(List.of());

        DashboardSummaryResponse summary = dashboardService.getSummary("all");

        assertThat(summary.totalReportsAudited()).isEqualTo(0L);
        assertThat(summary.averageComplianceScore()).isEqualTo(0.0);
        assertThat(summary.totalSpendAudited()).isEqualTo(0.0);
        assertThat(summary.countByRiskLevel()).containsEntry("LOW", 0L);
        assertThat(summary.countByRiskLevel()).containsEntry("MEDIUM", 0L);
        assertThat(summary.countByRiskLevel()).containsEntry("HIGH", 0L);
        assertThat(summary.topViolations()).isEmpty();
        assertThat(summary.complianceScoreDistribution()).hasSize(5);
        assertThat(summary.complianceScoreDistribution().get(0).count()).isEqualTo(0L);
        assertThat(summary.dateRange()).isEqualTo("all");
    }
}
