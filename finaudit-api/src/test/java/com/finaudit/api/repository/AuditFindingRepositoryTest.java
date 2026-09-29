package com.finaudit.api.repository;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.core.model.FindingSeverity;
import com.finaudit.core.model.RuleSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditFindingRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AuditFindingRepository findingRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Test
    @DisplayName("Should save and query audit findings by reportId")
    void shouldSaveAndQueryFindings() {
        com.finaudit.api.entity.Report report = reportRepository.save(
                new com.finaudit.api.entity.Report(1L, "test_finding.pdf", "storage/test_finding.pdf")
        );

        AuditFinding finding = new AuditFinding(
                report.getId(),
                null,
                RuleSource.SEMANTIC,
                FindingSeverity.HIGH,
                "Travel claim exceeds the allowed per diem of $75.",
                "Travel Expense Policy 2024"
        );
        AuditFinding saved = findingRepository.save(finding);

        assertThat(saved.getId()).isNotNull();

        List<AuditFinding> findings = findingRepository.findByReportId(report.getId());
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(FindingSeverity.HIGH);
    }
}
