package com.finaudit.api.service;

import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.ObservabilityMetricsResponse;
import com.finaudit.core.model.ReportStatus;
import com.finaudit.core.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ObservabilityServiceTest {

    private ReportRepository reportRepository;
    private AuditRunRepository auditRunRepository;
    private CostGuardrailService costGuardrailService;
    private ObservabilityService observabilityService;

    @BeforeEach
    void setUp() {
        reportRepository = Mockito.mock(ReportRepository.class);
        auditRunRepository = Mockito.mock(AuditRunRepository.class);
        costGuardrailService = Mockito.mock(CostGuardrailService.class);

        observabilityService = new ObservabilityService(
                reportRepository,
                auditRunRepository,
                costGuardrailService
        );
    }

    @Test
    @DisplayName("Should aggregate observability metrics across reports, runs, and guardrail limits")
    void shouldAggregateObservabilityMetricsCorrectly() {
        when(reportRepository.countByUploadedAtAfter(any(Instant.class))).thenReturn(15L);
        when(reportRepository.countByStatusAndUploadedAtAfter(eq(ReportStatus.FAILED), any(Instant.class))).thenReturn(1L);

        AuditRun run1 = new AuditRun(101L, 85, RiskLevel.LOW, "{}");
        run1.setStartedAt(Instant.now().minus(10, ChronoUnit.SECONDS));
        run1.setCompletedAt(Instant.now().minus(6, ChronoUnit.SECONDS)); // duration 4s

        AuditRun run2 = new AuditRun(102L, 40, RiskLevel.HIGH, "{}");
        run2.setStartedAt(Instant.now().minus(20, ChronoUnit.SECONDS));
        run2.setCompletedAt(Instant.now().minus(14, ChronoUnit.SECONDS)); // duration 6s

        when(auditRunRepository.findCompletedRunsAfter(any(Instant.class))).thenReturn(List.of(run1, run2));

        when(costGuardrailService.getDailyGeminiCallCount()).thenReturn(24);
        when(costGuardrailService.getMaxDailyGeminiCalls()).thenReturn(100);
        when(costGuardrailService.getActiveAuditPermits()).thenReturn(1);
        when(costGuardrailService.getMaxConcurrentAudits()).thenReturn(2);
        when(costGuardrailService.getMaxUploadsPerUser()).thenReturn(5);
        when(costGuardrailService.getMaxUploadsPerIp()).thenReturn(10);

        ObservabilityMetricsResponse metrics = observabilityService.getObservabilityMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.reportsProcessedToday()).isEqualTo(15L);
        assertThat(metrics.failedReportsLast24h()).isEqualTo(1L);
        assertThat(metrics.averageAuditCompletionTimeSeconds()).isEqualTo(5.0); // (4 + 6)/2 = 5.0
        assertThat(metrics.geminiCallsToday()).isEqualTo(24);
        assertThat(metrics.geminiDailyLimit()).isEqualTo(100);
        assertThat(metrics.circuitBreakerStatus()).isEqualTo("CLOSED");
        assertThat(metrics.activeConcurrencySlots()).isEqualTo(1);
        assertThat(metrics.maxConcurrencySlots()).isEqualTo(2);
        assertThat(metrics.maxUploadsPerUserDaily()).isEqualTo(5);
        assertThat(metrics.maxUploadsPerIpDaily()).isEqualTo(10);
        assertThat(metrics.systemUptimeSeconds()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("Should report OPEN circuit breaker when Gemini quota reached")
    void shouldReportCircuitBreakerOpenWhenQuotaExceeded() {
        when(reportRepository.countByUploadedAtAfter(any(Instant.class))).thenReturn(0L);
        when(reportRepository.countByStatusAndUploadedAtAfter(eq(ReportStatus.FAILED), any(Instant.class))).thenReturn(0L);
        when(auditRunRepository.findCompletedRunsAfter(any(Instant.class))).thenReturn(List.of());
        when(auditRunRepository.findAllCompletedRuns()).thenReturn(List.of());

        when(costGuardrailService.getDailyGeminiCallCount()).thenReturn(100);
        when(costGuardrailService.getMaxDailyGeminiCalls()).thenReturn(100);

        ObservabilityMetricsResponse metrics = observabilityService.getObservabilityMetrics();

        assertThat(metrics.circuitBreakerStatus()).isEqualTo("OPEN");
        assertThat(metrics.averageAuditCompletionTimeSeconds()).isEqualTo(0.0);
    }
}
