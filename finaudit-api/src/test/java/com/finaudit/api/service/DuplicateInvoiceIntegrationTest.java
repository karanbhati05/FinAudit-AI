package com.finaudit.api.service;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.FindingSeverity;
import com.finaudit.core.model.ReportStatus;
import com.finaudit.core.model.RuleSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
@ActiveProfiles({"local", "seed-policies"})
@Testcontainers(disabledWithoutDocker = true)
class DuplicateInvoiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("finaudit")
            .withUsername("postgres")
            .withPassword("postgres");

    @Autowired
    private PolicyIngestionService policyIngestionService;

    @Autowired
    private AuditOrchestrationService auditOrchestrationService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportLineItemRepository lineItemRepository;

    @Autowired
    private AuditFindingRepository auditFindingRepository;

    @BeforeEach
    void setup() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY is not set. Skipping live Gemini duplicate tool test.");
        policyIngestionService.ingestSeedPolicies();
    }

    @Test
    @DisplayName("Should detect duplicate invoice via tool calling and assert DETERMINISTIC finding referencing Report 1")
    void shouldDetectDuplicateInvoiceAcrossReports() {
        // Report 1: Earlier report processed in the past
        Report report1 = new Report(1L, "january_claim.txt", "storage/1/jan.txt");
        report1.setStatus(ReportStatus.COMPLETE);
        report1 = reportRepository.save(report1);

        ReportLineItem item1 = new ReportLineItem(
                report1.getId(),
                "INV-CLAIM-8833",
                "Acme Office Supplies",
                new BigDecimal("3450.00"),
                "USD",
                "OFFICE_SUPPLIES",
                "Desk and ergonomic chairs",
                1
        );
        lineItemRepository.save(item1);

        // Report 2: Current report being audited with the exact same vendor and invoice ID
        Report report2 = new Report(2L, "march_claim.txt", "storage/2/mar.txt");
        report2.setStatus(ReportStatus.AUDITING);
        report2 = reportRepository.save(report2);

        ReportLineItem item2 = new ReportLineItem(
                report2.getId(),
                "INV-CLAIM-8833",
                "Acme Office Supplies",
                new BigDecimal("3450.00"),
                "USD",
                "OFFICE_SUPPLIES",
                "Duplicate claim of previous invoice INV-CLAIM-8833 from Acme Office Supplies",
                1
        );
        lineItemRepository.save(item2);

        // Run audit on Report 2
        AuditRun auditRun = auditOrchestrationService.runAudit(report2.getId());

        assertThat(auditRun).isNotNull();

        List<AuditFinding> findings = auditFindingRepository.findByReportId(report2.getId());
        assertThat(findings).isNotEmpty();

        Long originalReportId = report1.getId();
        boolean foundDuplicateFinding = findings.stream().anyMatch(f ->
                f.getRuleSource() == RuleSource.DETERMINISTIC &&
                f.getSeverity() == FindingSeverity.CRITICAL &&
                f.getDescription().contains(String.valueOf(originalReportId))
        );

        assertThat(foundDuplicateFinding)
                .as("Must contain a DETERMINISTIC CRITICAL finding referencing report1 ID")
                .isTrue();
    }
}
