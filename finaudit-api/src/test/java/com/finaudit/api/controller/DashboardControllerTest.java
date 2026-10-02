package com.finaudit.api.controller;

import com.finaudit.api.service.DashboardService;
import com.finaudit.core.model.DashboardSummaryResponse;
import com.finaudit.core.model.PolicyViolationCount;
import com.finaudit.core.model.ScoreDistributionBucket;
import com.finaudit.core.model.SpendRiskTrendPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    @DisplayName("GET /api/dashboard/summary should return aggregated audit metrics, score distribution, and spend trend")
    void shouldReturnDashboardSummary() throws Exception {
        DashboardSummaryResponse response = new DashboardSummaryResponse(
                42L,
                84.5,
                125000.50,
                Map.of("LOW", 25L, "MEDIUM", 12L, "HIGH", 5L),
                List.of(
                        new PolicyViolationCount("Section 2: International Travel", 7L),
                        new PolicyViolationCount("Section 4: Entertainment & Dining", 4L)
                ),
                List.of(
                        new SpendRiskTrendPoint("Oct 1", "2026-10-01", 12400.0, 1, 0, 1, 2)
                ),
                List.of(
                        new ScoreDistributionBucket("0–49", 0, 49, 5, 11.9, "#ef4444"),
                        new ScoreDistributionBucket("90–100", 90, 100, 25, 59.5, "#10b981")
                ),
                "30d"
        );

        when(dashboardService.getSummary("30d")).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/summary").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReportsAudited").value(42))
                .andExpect(jsonPath("$.averageComplianceScore").value(84.5))
                .andExpect(jsonPath("$.totalSpendAudited").value(125000.50))
                .andExpect(jsonPath("$.countByRiskLevel.LOW").value(25))
                .andExpect(jsonPath("$.countByRiskLevel.MEDIUM").value(12))
                .andExpect(jsonPath("$.countByRiskLevel.HIGH").value(5))
                .andExpect(jsonPath("$.topViolations[0].policyReference").value("Section 2: International Travel"))
                .andExpect(jsonPath("$.topViolations[0].count").value(7))
                .andExpect(jsonPath("$.spendAndRiskTrend[0].label").value("Oct 1"))
                .andExpect(jsonPath("$.spendAndRiskTrend[0].totalSpend").value(12400.0))
                .andExpect(jsonPath("$.complianceScoreDistribution[0].rangeLabel").value("0–49"))
                .andExpect(jsonPath("$.dateRange").value("30d"));
    }

    @Test
    @DisplayName("GET /api/dashboard/summary?range=7d should pass range parameter to service")
    void shouldAcceptRangeParameter() throws Exception {
        DashboardSummaryResponse response = new DashboardSummaryResponse(
                10L,
                90.0,
                25000.0,
                Map.of("LOW", 8L, "MEDIUM", 2L, "HIGH", 0L),
                List.of(),
                List.of(),
                List.of(),
                "7d"
        );

        when(dashboardService.getSummary("7d")).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/summary").param("range", "7d").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReportsAudited").value(10))
                .andExpect(jsonPath("$.dateRange").value("7d"));
    }
}
