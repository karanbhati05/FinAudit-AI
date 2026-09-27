package com.finaudit.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {

    @Test
    @DisplayName("AuditFindingDto and AuditReportResult should initialize correctly and handle nulls safely")
    void shouldInitializeAuditReportResult() {
        AuditFindingDto finding = new AuditFindingDto(
                "SEMANTIC",
                "HIGH",
                "Violation description",
                "Clause 4.2",
                1L
        );

        AuditReportResult result = new AuditReportResult(
                85,
                "MEDIUM",
                List.of(finding),
                "Summary explanation"
        );

        assertThat(result.complianceScore()).isEqualTo(85);
        assertThat(result.riskLevel()).isEqualTo("MEDIUM");
        assertThat(result.flaggedItems()).hasSize(1);
        assertThat(result.flaggedItems().get(0).severity()).isEqualTo("HIGH");

        // Test null findings list fallback
        AuditReportResult nullFindingsResult = new AuditReportResult(90, "LOW", null, "Clean");
        assertThat(nullFindingsResult.flaggedItems()).isEmpty();
    }

    @Test
    @DisplayName("DashboardSummaryResponse should initialize and handle null collections safely")
    void shouldInitializeDashboardSummaryResponse() {
        DashboardSummaryResponse response = new DashboardSummaryResponse(
                10L,
                82.5,
                Map.of("LOW", 8L, "HIGH", 2L),
                List.of(new PolicyViolationCount("Clause 1", 3L))
        );

        assertThat(response.totalReportsAudited()).isEqualTo(10L);
        assertThat(response.averageComplianceScore()).isEqualTo(82.5);
        assertThat(response.countByRiskLevel()).containsEntry("LOW", 8L);
        assertThat(response.topViolations()).hasSize(1);

        // Null defensive fallback
        DashboardSummaryResponse nullResp = new DashboardSummaryResponse(0L, 0.0, null, null);
        assertThat(nullResp.countByRiskLevel()).isEmpty();
        assertThat(nullResp.topViolations()).isEmpty();
    }

    @Test
    @DisplayName("ReportDetailResponse should handle null collections defensively")
    void shouldInitializeReportDetailResponse() {
        ReportDetailResponse response = new ReportDetailResponse(
                1L,
                100L,
                "report.pdf",
                "storage/path",
                ReportStatus.COMPLETE,
                Instant.now(),
                Instant.now(),
                null,
                92,
                RiskLevel.LOW,
                "Summary",
                null,
                null
        );

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.lineItems()).isEmpty();
        assertThat(response.findings()).isEmpty();
    }

    @Test
    @DisplayName("Auth and Request records should hold values properly")
    void shouldTestAuthRecords() {
        RegisterRequest reg = new RegisterRequest("test@test.com", "pass", UserRole.AUDITOR);
        assertThat(reg.email()).isEqualTo("test@test.com");
        assertThat(reg.password()).isEqualTo("pass");
        assertThat(reg.role()).isEqualTo(UserRole.AUDITOR);

        LoginRequest login = new LoginRequest("test@test.com", "pass");
        assertThat(login.email()).isEqualTo("test@test.com");
        assertThat(login.password()).isEqualTo("pass");

        AuthResponse auth = new AuthResponse("tok", "ref", 1L, "test@test.com", UserRole.ADMIN, 9000L);
        assertThat(auth.token()).isEqualTo("tok");
        assertThat(auth.refreshToken()).isEqualTo("ref");
        assertThat(auth.role()).isEqualTo(UserRole.ADMIN);

        RefreshTokenRequest refReq = new RefreshTokenRequest("ref-tok");
        assertThat(refReq.refreshToken()).isEqualTo("ref-tok");

        ErrorResponse err = new ErrorResponse(404, "Not Found", "Missing", "/api/test");
        assertThat(err.status()).isEqualTo(404);
        assertThat(err.path()).isEqualTo("/api/test");

        ReportLineItemDto item = new ReportLineItemDto(1L, "INV-1", "Vendor", new BigDecimal("100"), "USD", "MEALS", "Dinner", 1);
        assertThat(item.vendor()).isEqualTo("Vendor");

        ReportSummaryDto summary = new ReportSummaryDto(1L, 2L, "file.pdf", ReportStatus.UPLOADED, Instant.now(), null, null, null);
        assertThat(summary.originalFilename()).isEqualTo("file.pdf");

        ReportUploadResponse upload = new ReportUploadResponse(5L, "Uploaded");
        assertThat(upload.reportId()).isEqualTo(5L);

        ReportStatusResponse status = new ReportStatusResponse(5L, ReportStatus.AUDITING, 10, null);
        assertThat(status.lineItemCount()).isEqualTo(10);

        ExtractedLineItem extracted = new ExtractedLineItem("INV-2", "V", new BigDecimal("50"), "USD", "CAT", "raw");
        ExtractedLineItemsList list = new ExtractedLineItemsList(List.of(extracted));
        assertThat(list.items()).hasSize(1);

        ResolvedFindingResponse resFinding = new ResolvedFindingResponse(1L, 2L, RuleSource.DETERMINISTIC, FindingSeverity.CRITICAL, "desc", "ref", "title", "body", Instant.now());
        assertThat(resFinding.policyTitle()).isEqualTo("title");
    }
}
