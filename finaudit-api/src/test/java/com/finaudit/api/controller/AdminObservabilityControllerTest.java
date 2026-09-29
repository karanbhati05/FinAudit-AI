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
}
