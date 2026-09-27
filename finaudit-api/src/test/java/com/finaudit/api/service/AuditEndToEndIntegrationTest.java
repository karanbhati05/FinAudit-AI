package com.finaudit.api.service;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.ReportStatus;
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
class AuditEndToEndIntegrationTest {

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
    private AuditRunRepository auditRunRepository;

    @Autowired
    private AuditFindingRepository auditFindingRepository;

    @BeforeEach
    void setup() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY is not set. Skipping live Gemini integration test.");
        policyIngestionService.ingestSeedPolicies();
    }

    @Test
    @DisplayName("Should detect and flag policy-violating flight line item during full audit")
    void shouldAuditPolicyViolatingReport() {
        // 1. Create a Report
        Report report = new Report(1L, "unapproved_travel.txt", "storage/reports/99/unapproved_travel.txt");
        report.setStatus(ReportStatus.AUDITING);
        report = reportRepository.save(report);

        // 2. Add deliberately policy-violating line item: ₹75,000 (~$900) flight claim with no VP approval note
        ReportLineItem lineItem = new ReportLineItem(
                report.getId(),
                "INV-FLIGHT-991",
                "Emirates Airlines",
                new BigDecimal("75000.00"),
                "INR",
                "TRAVEL",
                "Flight to London international flight business class, no VP approval obtained",
                1
        );
        lineItemRepository.save(lineItem);

        // 3. Run audit
        AuditRun auditRun = auditOrchestrationService.runAudit(report.getId());

        // 4. Assertions
        assertThat(auditRun).isNotNull();
        assertThat(auditRun.getComplianceScore()).isLessThan(100);

        List<AuditFinding> findings = auditFindingRepository.findByReportId(report.getId());
        assertThat(findings).isNotEmpty();
        assertThat(findings.get(0).getDescription()).isNotEmpty();

        Report completedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(completedReport.getStatus()).isEqualTo(ReportStatus.COMPLETE);
    }
}
