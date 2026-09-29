package com.finaudit.api.service;

import com.finaudit.api.config.DemoDataSeeder;
import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.entity.User;
import com.finaudit.api.repository.*;
import com.finaudit.api.storage.StorageService;
import com.finaudit.core.model.CleanupSummary;
import com.finaudit.core.model.FindingSeverity;
import com.finaudit.core.model.RiskLevel;
import com.finaudit.core.model.RuleSource;
import com.finaudit.core.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledCleanupServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportLineItemRepository lineItemRepository;
    @Mock
    private AuditFindingRepository auditFindingRepository;
    @Mock
    private AuditRunRepository auditRunRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private DemoDataSeeder demoDataSeeder;
    @Mock
    private CostGuardrailService costGuardrailService;

    private ScheduledCleanupService cleanupService;

    private final User demoUser = new User("demo@finaudit.ai", "hash", UserRole.AUDITOR);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(demoUser, "id", 1L);
        cleanupService = new ScheduledCleanupService(
                reportRepository,
                userRepository,
                lineItemRepository,
                auditFindingRepository,
                auditRunRepository,
                storageService,
                demoDataSeeder,
                costGuardrailService,
                30
        );
    }

    @Test
    @DisplayName("should purge expired non-demo reports and physical storage files")
    void shouldPurgeExpiredNonDemoReportsAndPhysicalStorage() {
        when(userRepository.findByEmail(DemoDataSeeder.DEMO_EMAIL)).thenReturn(Optional.of(demoUser));

        Report expiredReport = new Report(2L, "Old_Fiscal_Report_2025.pdf", "storage/reports/88/Old_Fiscal_Report_2025.pdf");
        ReflectionTestUtils.setField(expiredReport, "id", 88L);
        expiredReport.setUploadedAt(Instant.now().minus(35, ChronoUnit.DAYS));

        when(reportRepository.findByUploadedAtBeforeAndOwnerIdNot(any(Instant.class), eq(1L)))
                .thenReturn(List.of(expiredReport));

        AuditFinding finding = new AuditFinding(88L, null, RuleSource.SEMANTIC, FindingSeverity.HIGH, "Old violation", "Policy A");
        AuditRun auditRun = new AuditRun(88L, 65, RiskLevel.MEDIUM, "{}");
        ReportLineItem lineItem = new ReportLineItem(88L, "INV-001", "Vendor X", null, "USD", "General", "raw", 1);

        when(auditFindingRepository.findByReportId(88L)).thenReturn(List.of(finding));
        when(auditRunRepository.findByReportId(88L)).thenReturn(Optional.of(auditRun));
        when(lineItemRepository.findByReportId(88L)).thenReturn(List.of(lineItem));

        when(reportRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(storageService.getTotalStorageBytes()).thenReturn(2048L);
        when(costGuardrailService.getDailyGeminiCallCount()).thenReturn(5);

        CleanupSummary summary = cleanupService.runScheduledCleanup();

        assertThat(summary.expiredNonDemoReportsDeleted()).isEqualTo(1);
        assertThat(summary.demoVisitorReportsDeleted()).isZero();
        assertThat(summary.totalStorageBytesRemaining()).isEqualTo(2048L);
        assertThat(summary.dailyGeminiCallsToday()).isEqualTo(5);

        // Verify cascade deletion
        verify(auditFindingRepository).deleteAll(List.of(finding));
        verify(auditRunRepository).delete(auditRun);
        verify(lineItemRepository).deleteAll(List.of(lineItem));
        verify(reportRepository).delete(expiredReport);
        verify(storageService).delete(88L);
    }

    @Test
    @DisplayName("should purge visitor uploads on demo account and ensure canonical demo reports exist")
    void shouldPurgeVisitorUploadsOnDemoAccountAndEnsureCanonicalReports() {
        when(userRepository.findByEmail(DemoDataSeeder.DEMO_EMAIL)).thenReturn(Optional.of(demoUser));
        when(reportRepository.findByUploadedAtBeforeAndOwnerIdNot(any(Instant.class), eq(1L)))
                .thenReturn(List.of());

        Report visitorReport = new Report(1L, "Recruiter_Uploaded_Invoice.pdf", "storage/reports/99/Recruiter_Uploaded_Invoice.pdf");
        ReflectionTestUtils.setField(visitorReport, "id", 99L);

        Report canonicalReport1 = new Report(1L, "Q3_Executive_Travel_Claim.pdf", "storage/demo/Q3_Executive_Travel_Claim.pdf");
        ReflectionTestUtils.setField(canonicalReport1, "id", 100L);

        Report canonicalReport2 = new Report(1L, "Vendor_Payment_Batch_Sept.txt", "storage/demo/Vendor_Payment_Batch_Sept.txt");
        ReflectionTestUtils.setField(canonicalReport2, "id", 101L);

        when(reportRepository.findByOwnerId(1L)).thenReturn(List.of(visitorReport, canonicalReport1, canonicalReport2));
        when(storageService.getTotalStorageBytes()).thenReturn(4096L);
        when(costGuardrailService.getDailyGeminiCallCount()).thenReturn(10);

        CleanupSummary summary = cleanupService.runScheduledCleanup();

        assertThat(summary.expiredNonDemoReportsDeleted()).isZero();
        assertThat(summary.demoVisitorReportsDeleted()).isEqualTo(1);
        assertThat(summary.canonicalReportsCount()).isEqualTo(2);

        // Verify visitor report was purged
        verify(reportRepository).delete(visitorReport);
        verify(storageService).delete(99L);

        // Canonical reports must NOT be deleted
        verify(reportRepository, never()).delete(canonicalReport1);
        verify(reportRepository, never()).delete(canonicalReport2);
        verify(storageService, never()).delete(100L);
        verify(storageService, never()).delete(101L);

        // Verify canonical report restoration was checked
        verify(demoDataSeeder).ensureCanonicalReportsExist(demoUser);
    }

    @Test
    @DisplayName("should execute cleanly when demo user does not yet exist")
    void shouldExecuteCleanlyWhenDemoUserNotCreatedYet() {
        when(userRepository.findByEmail(DemoDataSeeder.DEMO_EMAIL)).thenReturn(Optional.empty());
        when(reportRepository.findByUploadedAtBefore(any(Instant.class))).thenReturn(List.of());
        when(storageService.getTotalStorageBytes()).thenReturn(0L);
        when(costGuardrailService.getDailyGeminiCallCount()).thenReturn(0);

        CleanupSummary summary = cleanupService.runScheduledCleanup();

        assertThat(summary.expiredNonDemoReportsDeleted()).isZero();
        assertThat(summary.demoVisitorReportsDeleted()).isZero();
        assertThat(summary.canonicalReportsCount()).isZero();
        verifyNoInteractions(demoDataSeeder);
    }
}
