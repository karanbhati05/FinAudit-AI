package com.finaudit.api.controller;

import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.api.service.ReportParsingService;
import com.finaudit.api.service.ReportQueryService;
import com.finaudit.api.storage.StorageService;
import com.finaudit.core.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportRepository reportRepository;

    @MockitoBean
    private ReportLineItemRepository lineItemRepository;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private ReportParsingService reportParsingService;

    @MockitoBean
    private AuditRunRepository auditRunRepository;

    @MockitoBean
    private AuditFindingRepository auditFindingRepository;

    @MockitoBean
    private ReportQueryService reportQueryService;

    @Test
    @DisplayName("GET /api/reports should handle pagination edge case: empty result")
    void shouldHandleEmptyPageResult() throws Exception {
        when(reportQueryService.getReports(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/reports?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    @DisplayName("GET /api/reports should handle pagination edge case: last page with partial elements")
    void shouldHandleLastPageResult() throws Exception {
        ReportSummaryDto item = new ReportSummaryDto(
                11L, 1L, "report_11.pdf", ReportStatus.COMPLETE,
                Instant.now(), Instant.now(), 95, RiskLevel.LOW
        );

        when(reportQueryService.getReports(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(2, 5), 11));

        mockMvc.perform(get("/api/reports?page=2&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(11))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.number").value(2));
    }

    @Test
    @DisplayName("GET /api/reports/{id} should return full report detail with resolved policy citation")
    void shouldReturnFullReportDetail() throws Exception {
        ReportLineItemDto lineItem = new ReportLineItemDto(
                101L, "INV-001", "Acme", new BigDecimal("150.00"), "USD",
                "LODGING", "Hotel night $150", 1
        );

        ResolvedFindingResponse finding = new ResolvedFindingResponse(
                1L, 101L, RuleSource.SEMANTIC, FindingSeverity.LOW,
                "Exceeded daily allowance slightly",
                "Section 1: Lodging Caps",
                "Section 1: Travel & Lodging Daily Caps",
                "All business travel lodging is capped at $150 USD per night.",
                Instant.now()
        );

        ReportDetailResponse detail = new ReportDetailResponse(
                5L, 2L, "travel_exp.pdf", "storage/5/travel_exp.pdf",
                ReportStatus.COMPLETE, Instant.now(), Instant.now(), null,
                92, RiskLevel.LOW, "Clean audit with 1 low finding",
                List.of(lineItem), List.of(finding)
        );

        when(reportQueryService.getReportDetail(5L)).thenReturn(Optional.of(detail));

        mockMvc.perform(get("/api/reports/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.complianceScore").value(92))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.lineItems[0].invoiceId").value("INV-001"))
                .andExpect(jsonPath("$.findings[0].policyTitle").value("Section 1: Travel & Lodging Daily Caps"))
                .andExpect(jsonPath("$.findings[0].policyBodyText").value(org.hamcrest.Matchers.containsString("capped at $150 USD")));
    }

    @Test
    @DisplayName("GET /api/reports/{id} should return 404 when report not found")
    void shouldReturn404WhenReportNotFound() throws Exception {
        when(reportQueryService.getReportDetail(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/reports/999"))
                .andExpect(status().isNotFound());
    }
}
