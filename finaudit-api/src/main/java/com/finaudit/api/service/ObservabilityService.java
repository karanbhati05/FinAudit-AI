package com.finaudit.api.service;

import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.ObservabilityMetricsResponse;
import com.finaudit.core.model.ReportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityService.class);

    private final ReportRepository reportRepository;
    private final AuditRunRepository auditRunRepository;
    private final CostGuardrailService costGuardrailService;

    public ObservabilityService(
            ReportRepository reportRepository,
            AuditRunRepository auditRunRepository,
            CostGuardrailService costGuardrailService
    ) {
        this.reportRepository = reportRepository;
        this.auditRunRepository = auditRunRepository;
        this.costGuardrailService = costGuardrailService;
    }

    public ObservabilityMetricsResponse getObservabilityMetrics() {
        // 1. Reports processed today (since midnight UTC)
        Instant startOfTodayUtc = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        long processedToday = reportRepository.countByUploadedAtAfter(startOfTodayUtc);

        // 2. Count of FAILED reports in the last 24 hours
        Instant last24h = Instant.now().minus(Duration.ofHours(24));
        long failedLast24h = reportRepository.countByStatusAndUploadedAtAfter(ReportStatus.FAILED, last24h);

        // 3. Average audit completion time (seconds)
        List<AuditRun> recentRuns = auditRunRepository.findCompletedRunsAfter(last24h);
        if (recentRuns.isEmpty()) {
            recentRuns = auditRunRepository.findAllCompletedRuns();
        }

        double avgDurationSeconds = 0.0;
        if (!recentRuns.isEmpty()) {
            double totalMillis = recentRuns.stream()
                    .mapToLong(r -> Duration.between(r.getStartedAt(), r.getCompletedAt()).toMillis())
                    .average()
                    .orElse(0.0);
            avgDurationSeconds = Math.round((totalMillis / 1000.0) * 100.0) / 100.0;
        }

        // 4. Rate-limit & Circuit-breaker counters
        int geminiCallsToday = costGuardrailService != null ? costGuardrailService.getDailyGeminiCallCount() : 0;
        int geminiDailyLimit = costGuardrailService != null ? costGuardrailService.getMaxDailyGeminiCalls() : 100;
        String circuitBreakerStatus = geminiCallsToday >= geminiDailyLimit ? "OPEN" : "CLOSED";
        int activeConcurrencySlots = costGuardrailService != null ? costGuardrailService.getActiveAuditPermits() : 0;
        int maxConcurrencySlots = costGuardrailService != null ? costGuardrailService.getMaxConcurrentAudits() : 2;
        int maxUploadsPerUserDaily = costGuardrailService != null ? costGuardrailService.getMaxUploadsPerUser() : 5;
        int maxUploadsPerIpDaily = costGuardrailService != null ? costGuardrailService.getMaxUploadsPerIp() : 10;

        // 5. System uptime
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        log.debug("Computed observability metrics: processedToday={}, failed24h={}, avgDurationSec={}, geminiCalls={}/{}",
                processedToday, failedLast24h, avgDurationSeconds, geminiCallsToday, geminiDailyLimit);

        return new ObservabilityMetricsResponse(
                processedToday,
                failedLast24h,
                avgDurationSeconds,
                geminiCallsToday,
                geminiDailyLimit,
                circuitBreakerStatus,
                activeConcurrencySlots,
                maxConcurrencySlots,
                maxUploadsPerUserDaily,
                maxUploadsPerIpDaily,
                uptimeSeconds
        );
    }
}
