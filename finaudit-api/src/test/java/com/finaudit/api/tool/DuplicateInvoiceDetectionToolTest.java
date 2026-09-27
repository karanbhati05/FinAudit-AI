package com.finaudit.api.tool;

import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.DuplicateCheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuplicateInvoiceDetectionToolTest {

    @Mock
    private ReportLineItemRepository lineItemRepository;

    @Mock
    private ReportRepository reportRepository;

    private DuplicateInvoiceDetectionTool tool;

    @BeforeEach
    void setUp() {
        tool = new DuplicateInvoiceDetectionTool(lineItemRepository, reportRepository);
    }

    @Test
    @DisplayName("Should detect duplicate invoice from previous report excluding current report ID")
    void shouldDetectDuplicateFromPreviousReport() {
        tool.setCurrentReportId(2L); // Currently auditing report 2

        ReportLineItem priorItem = new ReportLineItem(
                1L, "INV-2024-001", "Acme Supplies",
                new BigDecimal("500.00"), "USD", "OFFICE", "Supplies", 1
        );

        Report priorReport = new Report(10L, "january_report.pdf", "storage/1/jan.pdf");
        priorReport.setId(1L);
        priorReport.setUploadedAt(Instant.parse("2024-01-15T10:00:00Z"));

        when(lineItemRepository.findByVendorIgnoreCaseAndInvoiceIdIgnoreCaseAndReportIdNot(
                eq("Acme Supplies"), eq("INV-2024-001"), eq(2L)
        )).thenReturn(List.of(priorItem));

        when(reportRepository.findById(1L)).thenReturn(Optional.of(priorReport));

        DuplicateCheckResult result = tool.checkDatabaseForDuplicateInvoice("Acme Supplies", "INV-2024-001");

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.originalReportId()).isEqualTo(1L);
        assertThat(result.originalReportDate()).isNotNull();

        tool.clearCurrentReportId();
    }

    @Test
    @DisplayName("Should return isDuplicate=false when no previous matching invoice exists")
    void shouldReturnFalseForUniqueInvoice() {
        tool.setCurrentReportId(2L);

        when(lineItemRepository.findByVendorIgnoreCaseAndInvoiceIdIgnoreCaseAndReportIdNot(
                eq("New Vendor"), eq("INV-UNIQUE-99"), eq(2L)
        )).thenReturn(List.of());

        DuplicateCheckResult result = tool.checkDatabaseForDuplicateInvoice("New Vendor", "INV-UNIQUE-99");

        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.originalReportId()).isNull();
        assertThat(result.originalReportDate()).isNull();

        tool.clearCurrentReportId();
    }
}
