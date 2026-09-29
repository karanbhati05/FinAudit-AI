package com.finaudit.api.service;

import com.finaudit.api.config.DemoDataSeeder;
import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.User;
import com.finaudit.api.repository.*;
import com.finaudit.api.storage.StorageService;
import com.finaudit.core.model.CleanupSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Nightly scheduled maintenance service that:
 * 1. Purges non-demo reports older than retention threshold (default 30 days) to prevent disk/DB saturation on free tier.
 * 2. Cleans up non-canonical visitor uploads from the public demo account (demo@finaudit.ai) and ensures the 4 canonical sample reports exist.
 * 3. Deletes cascade DB rows (findings, audit runs, line items, report) and physical storage directory.
 * 4. Logs a daily maintenance summary.
 */
@Service
public class ScheduledCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCleanupService.class);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportLineItemRepository lineItemRepository;
    private final AuditFindingRepository auditFindingRepository;
    private final AuditRunRepository auditRunRepository;
    private final StorageService storageService;
    private final DemoDataSeeder demoDataSeeder;
    private final CostGuardrailService costGuardrailService;
    private final int retentionDays;

    public ScheduledCleanupService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            ReportLineItemRepository lineItemRepository,
            AuditFindingRepository auditFindingRepository,
            AuditRunRepository auditRunRepository,
            StorageService storageService,
            DemoDataSeeder demoDataSeeder,
            CostGuardrailService costGuardrailService,
            @Value("${finaudit.cleanup.retention-days:30}") int retentionDays
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.lineItemRepository = lineItemRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.auditRunRepository = auditRunRepository;
        this.storageService = storageService;
        this.demoDataSeeder = demoDataSeeder;
        this.costGuardrailService = costGuardrailService;
        this.retentionDays = retentionDays;
    }

    /**
     * Executes nightly at 02:00 UTC (or triggered on-demand via admin endpoint).
     */
    @Scheduled(cron = "${finaudit.cleanup.cron:0 0 2 * * *}", zone = "UTC")
    @Transactional
    public CleanupSummary runScheduledCleanup() {
        log.info("[MAINTENANCE] Starting scheduled cleanup and storage hygiene job (retentionDays={}).", retentionDays);

        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        Optional<User> demoUserOpt = userRepository.findByEmail(DemoDataSeeder.DEMO_EMAIL);

        int expiredNonDemoCount = 0;
        int demoVisitorCount = 0;

        // 1. Purge expired non-demo reports (> 30 days)
        List<Report> expiredReports = demoUserOpt.isPresent()
                ? reportRepository.findByUploadedAtBeforeAndOwnerIdNot(cutoff, demoUserOpt.get().getId())
                : reportRepository.findByUploadedAtBefore(cutoff);

        for (Report report : expiredReports) {
            log.info("[MAINTENANCE] Purging expired non-demo report ID {} ({}) uploaded at {}",
                    report.getId(), report.getOriginalFilename(), report.getUploadedAt());
            deleteReportCascade(report);
            expiredNonDemoCount++;
        }

        // 2. Clean up demo account visitor uploads & restore canonical reports
        if (demoUserOpt.isPresent()) {
            User demoUser = demoUserOpt.get();
            List<Report> demoReports = reportRepository.findByOwnerId(demoUser.getId());

            for (Report demoReport : demoReports) {
                if (!DemoDataSeeder.CANONICAL_DEMO_FILENAMES.contains(demoReport.getOriginalFilename())) {
                    log.info("[MAINTENANCE] Purging visitor upload from demo account: report ID {} ({})",
                            demoReport.getId(), demoReport.getOriginalFilename());
                    deleteReportCascade(demoReport);
                    demoVisitorCount++;
                }
            }

            // Restore any canonical reports if they were missing or deleted
            demoDataSeeder.ensureCanonicalReportsExist(demoUser);
        }

        // 3. Gather stats
        int canonicalCount = demoUserOpt.map(u -> (int) reportRepository.findByOwnerId(u.getId()).stream()
                .filter(r -> DemoDataSeeder.CANONICAL_DEMO_FILENAMES.contains(r.getOriginalFilename()))
                .count()).orElse(0);

        long totalStorageBytesRemaining = storageService.getTotalStorageBytes();
        int dailyGeminiCallsToday = costGuardrailService.getDailyGeminiCallCount();

        CleanupSummary summary = new CleanupSummary(
                expiredNonDemoCount,
                demoVisitorCount,
                canonicalCount,
                totalStorageBytesRemaining,
                dailyGeminiCallsToday,
                Instant.now()
        );

        log.info("[MAINTENANCE] Scheduled cleanup complete. Expired reports purged: {}, Demo visitor reports purged: {}, " +
                        "Canonical demo reports active: {}, Disk usage: {} bytes, Gemini calls today: {}",
                summary.expiredNonDemoReportsDeleted(),
                summary.demoVisitorReportsDeleted(),
                summary.canonicalReportsCount(),
                summary.totalStorageBytesRemaining(),
                summary.dailyGeminiCallsToday());

        return summary;
    }

    private void deleteReportCascade(Report report) {
        Long reportId = report.getId();
        auditFindingRepository.deleteAll(auditFindingRepository.findByReportId(reportId));
        auditRunRepository.findByReportId(reportId).ifPresent(auditRunRepository::delete);
        lineItemRepository.deleteAll(lineItemRepository.findByReportId(reportId));
        reportRepository.delete(report);
        storageService.delete(reportId);
    }
}
