package com.finaudit.api.service;

import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.core.model.DashboardSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AuditRunRepository auditRunRepository;

    @Mock
    private AuditFindingRepository auditFindingRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("getSummary() should calculate metrics, risk breakdown, and top policy violations accurately")
    void shouldCalculateSummaryMetrics() {
        when(auditRunRepository.countAllAudited()).thenReturn(20L);
        when(auditRunRepository.getAverageComplianceScore()).thenReturn(85.46);

        List<Object[]> riskRows = new ArrayList<>();
        riskRows.add(new Object[]{"LOW", 12L});
        riskRows.add(new Object[]{"MEDIUM", 5L});
        riskRows.add(new Object[]{"HIGH", 3L});
        when(auditRunRepository.countByRiskLevelGrouped()).thenReturn(riskRows);

        List<Object[]> topRows = new ArrayList<>();
        topRows.add(new Object[]{"Clause 4.2", 7L});
        topRows.add(new Object[]{"Clause 7.1", 4L});
        when(auditFindingRepository.findTopPolicyViolations(any(Pageable.class))).thenReturn(topRows);

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.totalReportsAudited()).isEqualTo(20L);
        assertThat(summary.averageComplianceScore()).isEqualTo(85.5);
        assertThat(summary.countByRiskLevel().get("LOW")).isEqualTo(12L);
        assertThat(summary.countByRiskLevel().get("MEDIUM")).isEqualTo(5L);
        assertThat(summary.countByRiskLevel().get("HIGH")).isEqualTo(3L);
        assertThat(summary.topViolations()).hasSize(2);
        assertThat(summary.topViolations().get(0).policyReference()).isEqualTo("Clause 4.2");
        assertThat(summary.topViolations().get(0).count()).isEqualTo(7L);
    }
}
