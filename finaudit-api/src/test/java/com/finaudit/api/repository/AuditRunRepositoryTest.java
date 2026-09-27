package com.finaudit.api.repository;

import com.finaudit.api.entity.AuditRun;
import com.finaudit.core.model.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRunRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AuditRunRepository auditRunRepository;

    @Test
    @DisplayName("Should save and query audit runs by reportId")
    void shouldSaveAndQueryAuditRun() {
        AuditRun run = new AuditRun(
                10L,
                82,
                RiskLevel.MEDIUM,
                "{\"summary\": \"Audit completed with 1 finding\"}"
        );
        AuditRun saved = auditRunRepository.save(run);

        assertThat(saved.getId()).isNotNull();

        Optional<AuditRun> found = auditRunRepository.findByReportId(10L);
        assertThat(found).isPresent();
        assertThat(found.get().getComplianceScore()).isEqualTo(82);
        assertThat(found.get().getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }
}
