package com.finaudit.api.service;

import com.finaudit.api.entity.Report;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.ReportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ReportWatchdogService {

    private static final Logger log = LoggerFactory.getLogger(ReportWatchdogService.class);
    private final ReportRepository reportRepository;

    public ReportWatchdogService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * Watchdog runs every 30 seconds to catch reports that have been stuck in PARSING or AUDITING
     * for longer than 2 minutes (e.g. due to process crashes, unhandled LLM timeouts, or network loss).
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void reclaimStuckReports() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(2));
        List<Report> stuckReports = reportRepository.findByStatusInAndUploadedAtBefore(
                List.of(ReportStatus.PARSING, ReportStatus.AUDITING),
                cutoff
        );

        if (!stuckReports.isEmpty()) {
            log.warn("ReportWatchdog detected {} report(s) exceeding 2-minute pipeline timeout threshold.", stuckReports.size());
            for (Report report : stuckReports) {
                log.warn("Watchdog timeout triggered for report ID: {} (status: {}, uploadedAt: {}). Marking as FAILED.",
                        report.getId(), report.getStatus(), report.getUploadedAt());
                report.setStatus(ReportStatus.FAILED);
                report.setErrorReason("Pipeline watchdog timeout: Report processing exceeded 2 minutes. Please retry the audit.");
                reportRepository.save(report);
            }
        }
    }
}
