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

    @Autowired
    private ReportRepository reportRepository;

    @Test
    @DisplayName("Should save and query audit runs by reportId")
    void shouldSaveAndQueryAuditRun() {
        com.finaudit.api.entity.Report report = reportRepository.save(
                new com.finaudit.api.entity.Report(1L, "test_run.pdf", "storage/test_run.pdf")
        );

        AuditRun run = new AuditRun(
                report.getId(),
                82,
                RiskLevel.MEDIUM,
                "{\"summary\": \"Audit completed with 1 finding\"}"
        );
        AuditRun saved = auditRunRepository.save(run);

        assertThat(saved.getId()).isNotNull();

        Optional<AuditRun> found = auditRunRepository.findByReportId(report.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getComplianceScore()).isEqualTo(82);
        assertThat(found.get().getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }
}
