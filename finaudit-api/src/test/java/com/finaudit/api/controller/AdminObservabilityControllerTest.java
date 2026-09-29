package com.finaudit.api.controller;

import com.finaudit.api.service.ObservabilityService;
import com.finaudit.core.model.ObservabilityMetricsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminObservabilityController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminObservabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObservabilityService observabilityService;

    @MockitoBean
    private com.finaudit.api.service.ScheduledCleanupService scheduledCleanupService;

    @Test
    @DisplayName("GET /api/admin/metrics should return aggregate observability metrics")
    void shouldReturnObservabilityMetrics() throws Exception {
        ObservabilityMetricsResponse response = new ObservabilityMetricsResponse(
                12L,
                0L,
                4.5,
                18,
                100,
                "CLOSED",
                0,
                2,
                5,
                10,
                3600L
        );

        when(observabilityService.getObservabilityMetrics()).thenReturn(response);

        mockMvc.perform(get("/api/admin/metrics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportsProcessedToday").value(12))
                .andExpect(jsonPath("$.circuitBreakerStatus").value("CLOSED"))
                .andExpect(jsonPath("$.geminiCallsToday").value(18))
                .andExpect(jsonPath("$.averageAuditCompletionTimeSeconds").value(4.5));
    }

    @Test
    @DisplayName("POST /api/admin/maintenance/cleanup should execute storage hygiene and return summary")
    void shouldTriggerScheduledCleanup() throws Exception {
        com.finaudit.core.model.CleanupSummary summary = new com.finaudit.core.model.CleanupSummary(
                3,
                1,
                4,
                1048576L,
                12,
                java.time.Instant.now()
        );

        when(scheduledCleanupService.runScheduledCleanup()).thenReturn(summary);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/maintenance/cleanup")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiredNonDemoReportsDeleted").value(3))
                .andExpect(jsonPath("$.demoVisitorReportsDeleted").value(1))
                .andExpect(jsonPath("$.canonicalReportsCount").value(4))
                .andExpect(jsonPath("$.totalStorageBytesRemaining").value(1048576));
    }
}
