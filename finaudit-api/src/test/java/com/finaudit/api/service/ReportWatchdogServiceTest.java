package com.finaudit.api.service;

import com.finaudit.api.entity.Report;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.ReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportWatchdogServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportWatchdogService watchdogService;

    @Test
    @DisplayName("Watchdog should find stuck reports older than 2 minutes and mark them as FAILED")
    void testReclaimStuckReports() {
        Report stuckReport = new Report(1L, "stuck_invoice.pdf", "storage/reports/1/stuck_invoice.pdf");
        stuckReport.setId(42L);
        stuckReport.setStatus(ReportStatus.AUDITING);
        stuckReport.setUploadedAt(Instant.now().minusSeconds(180)); // 3 minutes ago

        when(reportRepository.findByStatusInAndUploadedAtBefore(
                eq(List.of(ReportStatus.PARSING, ReportStatus.AUDITING)),
                any(Instant.class)
        )).thenReturn(List.of(stuckReport));

        watchdogService.reclaimStuckReports();

        assertThat(stuckReport.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(stuckReport.getErrorReason()).contains("Pipeline watchdog timeout");
        verify(reportRepository).save(stuckReport);
    }

    @Test
    @DisplayName("Watchdog should do nothing if no reports are stuck")
    void testNoStuckReports() {
        when(reportRepository.findByStatusInAndUploadedAtBefore(
                eq(List.of(ReportStatus.PARSING, ReportStatus.AUDITING)),
                any(Instant.class)
        )).thenReturn(List.of());

        watchdogService.reclaimStuckReports();

        verify(reportRepository, never()).save(any());
    }
}
