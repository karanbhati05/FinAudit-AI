package com.finaudit.api.controller;

import com.finaudit.api.service.DashboardService;
import com.finaudit.core.model.DashboardSummaryResponse;
import com.finaudit.core.model.PolicyViolationCount;
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
    @DisplayName("GET /api/dashboard/summary should return aggregated audit metrics and top violations")
    void shouldReturnDashboardSummary() throws Exception {
        DashboardSummaryResponse response = new DashboardSummaryResponse(
                42L,
                84.5,
                Map.of("LOW", 25L, "MEDIUM", 12L, "HIGH", 5L),
                List.of(
                        new PolicyViolationCount("Section 2: International Travel", 7L),
                        new PolicyViolationCount("Section 4: Entertainment & Dining", 4L)
                )
        );

        when(dashboardService.getSummary()).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/summary").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReportsAudited").value(42))
                .andExpect(jsonPath("$.averageComplianceScore").value(84.5))
                .andExpect(jsonPath("$.countByRiskLevel.LOW").value(25))
                .andExpect(jsonPath("$.countByRiskLevel.MEDIUM").value(12))
                .andExpect(jsonPath("$.countByRiskLevel.HIGH").value(5))
                .andExpect(jsonPath("$.topViolations[0].policyReference").value("Section 2: International Travel"))
                .andExpect(jsonPath("$.topViolations[0].count").value(7))
                .andExpect(jsonPath("$.topViolations[1].policyReference").value("Section 4: Entertainment & Dining"))
                .andExpect(jsonPath("$.topViolations[1].count").value(4));
    }
}
