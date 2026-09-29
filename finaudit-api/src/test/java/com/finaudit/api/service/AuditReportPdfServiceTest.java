package com.finaudit.api.service;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.Report;
import com.finaudit.core.model.FindingSeverity;
import com.finaudit.core.model.ReportStatus;
import com.finaudit.core.model.RiskLevel;
import com.finaudit.core.model.RuleSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditReportPdfServiceTest {

    private final AuditReportPdfService pdfService = new AuditReportPdfService();

    @Test
    @DisplayName("Should generate a valid, non-empty PDF document starting with standard %PDF- magic bytes")
    void shouldGenerateValidNonEmptyPdf() {
        Report report = new Report(1L, "Q3_Corporate_Expenses.pdf", "storage/demo/Q3_Corporate_Expenses.pdf");
        report.setId(42L);
        report.setStatus(ReportStatus.COMPLETE);
        report.setAuditedAt(Instant.now());

        AuditRun auditRun = new AuditRun(42L, 78, RiskLevel.MEDIUM, "{\"summary\":\"Corporate travel audit complete.\"}");
        auditRun.setId(101L);
        auditRun.setCompletedAt(Instant.now());

        AuditFinding finding1 = new AuditFinding(
                42L,
                1L,
                RuleSource.SEMANTIC,
                FindingSeverity.HIGH,
                "Airfare exceeds business class regional allowance without executive sign-off.",
                "Corporate Travel Policy - Section 4.1"
        );
        AuditFinding finding2 = new AuditFinding(
                42L,
                2L,
                RuleSource.DETERMINISTIC,
                FindingSeverity.CRITICAL,
                "Duplicate invoice ID detected across accounts for identical vendor amount.",
                "Anti-Fraud Policy - Section 1.1"
        );

        byte[] pdfBytes = pdfService.generateAuditReportPdf(report, auditRun, List.of(finding1, finding2));

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        // Standard PDF magic bytes check (%PDF-)
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5), StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Should generate valid PDF even when report has zero findings (clean audit)")
    void shouldGenerateValidPdfForCleanReport() {
        Report report = new Report(2L, "Clean_Invoices.pdf", "storage/reports/2/Clean_Invoices.pdf");
        report.setId(99L);
        report.setStatus(ReportStatus.COMPLETE);
        report.setAuditedAt(Instant.now());

        AuditRun auditRun = new AuditRun(99L, 100, RiskLevel.LOW, "{\"summary\":\"No findings.\"}");
        auditRun.setId(102L);

        byte[] pdfBytes = pdfService.generateAuditReportPdf(report, auditRun, List.of());

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(500);
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }
}
