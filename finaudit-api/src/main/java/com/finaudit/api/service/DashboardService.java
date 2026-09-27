package com.finaudit.api.service;

import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.core.model.DashboardSummaryResponse;
import com.finaudit.core.model.PolicyViolationCount;
import com.finaudit.core.model.RiskLevel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DashboardService {

    private final AuditRunRepository auditRunRepository;
    private final AuditFindingRepository auditFindingRepository;

    public DashboardService(AuditRunRepository auditRunRepository, AuditFindingRepository auditFindingRepository) {
        this.auditRunRepository = auditRunRepository;
        this.auditFindingRepository = auditFindingRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalAudited = auditRunRepository.countAllAudited();
        Double avgScoreRaw = auditRunRepository.getAverageComplianceScore();
        double avgScore = avgScoreRaw != null
                ? BigDecimal.valueOf(avgScoreRaw).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        // Group by risk level
        Map<String, Long> countByRiskLevel = new LinkedHashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            countByRiskLevel.put(level.name(), 0L);
        }

        List<Object[]> riskCounts = auditRunRepository.countByRiskLevelGrouped();
        for (Object[] row : riskCounts) {
            if (row[0] != null) {
                String riskName = row[0].toString();
                long count = ((Number) row[1]).longValue();
                countByRiskLevel.put(riskName, count);
            }
        }

        // Top 5 policy violations
        List<Object[]> topRows = auditFindingRepository.findTopPolicyViolations(PageRequest.of(0, 5));
        List<PolicyViolationCount> topViolations = new ArrayList<>();
        for (Object[] row : topRows) {
            String policyRef = (String) row[0];
            long count = ((Number) row[1]).longValue();
            topViolations.add(new PolicyViolationCount(policyRef, count));
        }

        return new DashboardSummaryResponse(
                totalAudited,
                avgScore,
                countByRiskLevel,
                topViolations
        );
    }
}
