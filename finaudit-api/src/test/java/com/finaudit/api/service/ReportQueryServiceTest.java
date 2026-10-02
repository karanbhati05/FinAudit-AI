package com.finaudit.api.service;

import com.finaudit.api.entity.*;
import com.finaudit.api.repository.*;
import com.finaudit.api.security.UserPrincipal;
import com.finaudit.core.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportQueryServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportLineItemRepository lineItemRepository;

    @Mock
    private AuditRunRepository auditRunRepository;

    @Mock
    private AuditFindingRepository auditFindingRepository;

    @Mock
    private CompliancePolicyRepository compliancePolicyRepository;

    @InjectMocks
    private ReportQueryService reportQueryService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getReports() should fetch paginated reports with compliance score and risk level")
    void shouldReturnPaginatedReports() {
        Report report = new Report(1L, "expenses.pdf", "storage/path");
        report.setId(10L);
        report.setStatus(ReportStatus.COMPLETE);

        AuditRun run = new AuditRun(10L, 88, RiskLevel.LOW, "Audit summary");
        when(reportRepository.findAll(org.mockito.ArgumentMatchers.nullable(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));
        when(auditRunRepository.findByReportId(10L)).thenReturn(Optional.of(run));

        Page<ReportSummaryDto> page = reportQueryService.getReports(null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        ReportSummaryDto dto = page.getContent().get(0);
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.complianceScore()).isEqualTo(88);
        assertThat(dto.riskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    @DisplayName("getReportDetail() should resolve policy references to full title and bodyText")
    void shouldResolvePolicyReferencesInDetail() {
        Report report = new Report(1L, "expenses.pdf", "storage/path");
        report.setId(10L);
        report.setStatus(ReportStatus.COMPLETE);

        ReportLineItem item = new ReportLineItem(10L, "INV-1", "United Airlines", new BigDecimal("1500.00"), "USD", "TRAVEL", "Flight SFO", 1);
        item.setId(50L);

        AuditFinding finding = new AuditFinding(10L, 50L, RuleSource.SEMANTIC, FindingSeverity.HIGH, "Business class flight", "Clause 4.2");
        finding.setId(100L);

        AuditRun run = new AuditRun(10L, 75, RiskLevel.MEDIUM, "Audit summary");

        CompliancePolicy policy = new CompliancePolicy("Clause 4.2: Flight Booking Standards", "Economy required for domestic flights.", "Travel", java.time.LocalDate.now());

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(lineItemRepository.findByReportId(10L)).thenReturn(List.of(item));
        when(auditRunRepository.findByReportId(10L)).thenReturn(Optional.of(run));
        when(auditFindingRepository.findByReportId(10L)).thenReturn(List.of(finding));
        when(compliancePolicyRepository.findAll()).thenReturn(List.of(policy));

        Optional<ReportDetailResponse> detailOpt = reportQueryService.getReportDetail(10L);

        assertThat(detailOpt).isPresent();
        ReportDetailResponse detail = detailOpt.get();
        assertThat(detail.id()).isEqualTo(10L);
        assertThat(detail.findings()).hasSize(1);
        assertThat(detail.totalLineItemCount()).isEqualTo(1);
        assertThat(detail.flaggedLineItemCount()).isEqualTo(1);
        assertThat(detail.totalSpend()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(detail.totalFlaggedAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(detail.flaggedSummary()).isEqualTo("1 of 1 line items flagged, $1,500.00 total flagged amount");
        assertThat(detail.categorySpend()).hasSize(1);
        assertThat(detail.categorySpend().get(0).category()).isEqualTo("TRAVEL");

        ResolvedFindingResponse resolvedFinding = detail.findings().get(0);
        assertThat(resolvedFinding.policyTitle()).isEqualTo("Clause 4.2: Flight Booking Standards");
        assertThat(resolvedFinding.policyBodyText()).isEqualTo("Economy required for domestic flights.");
        assertThat(resolvedFinding.isHeuristic()).isFalse();
    }

    @Test
    @DisplayName("getReportDetail() should throw AccessDeniedException when VIEWER tries to access another user's report")
    void viewerAccessingAnotherReportShouldThrow() {
        Report report = new Report(999L, "confidential.pdf", "storage/path");
        report.setId(20L);

        UserPrincipal viewerPrincipal = new UserPrincipal(101L, "viewer@corp.com", "pass", UserRole.VIEWER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(viewerPrincipal, null, viewerPrincipal.getAuthorities())
        );

        when(reportRepository.findById(20L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportQueryService.getReportDetail(20L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied: Viewers can only view reports where they are the owner");
    }

    @Test
    @DisplayName("getReportDetail() should permit VIEWER when accessing their own report")
    void viewerAccessingOwnReportShouldSucceed() {
        Report report = new Report(101L, "my-expenses.pdf", "storage/path");
        report.setId(20L);

        UserPrincipal viewerPrincipal = new UserPrincipal(101L, "viewer@corp.com", "pass", UserRole.VIEWER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(viewerPrincipal, null, viewerPrincipal.getAuthorities())
        );

        when(reportRepository.findById(20L)).thenReturn(Optional.of(report));
        when(lineItemRepository.findByReportId(20L)).thenReturn(List.of());
        when(auditRunRepository.findByReportId(20L)).thenReturn(Optional.empty());
        when(auditFindingRepository.findByReportId(20L)).thenReturn(List.of());
        when(compliancePolicyRepository.findAll()).thenReturn(List.of());

        Optional<ReportDetailResponse> detail = reportQueryService.getReportDetail(20L);
        assertThat(detail).isPresent();
    }
}
