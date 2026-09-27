package com.finaudit.api.service;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.api.tool.DuplicateInvoiceDetectionTool;
import com.finaudit.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuplicateAuditOrchestrationTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportLineItemRepository lineItemRepository;

    @Mock
    private PolicySearchService policySearchService;

    @Mock
    private AuditPromptService auditPromptService;

    @Mock
    private AuditRunRepository auditRunRepository;

    @Mock
    private AuditFindingRepository auditFindingRepository;

    @Mock
    private DuplicateInvoiceDetectionTool duplicateInvoiceTool;

    @Mock
    private ChatClient chatClient;

    @Mock(answer = Answers.RETURNS_SELF)
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private AuditOrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        orchestrationService = new AuditOrchestrationService(
                reportRepository,
                lineItemRepository,
                policySearchService,
                auditPromptService,
                auditRunRepository,
                auditFindingRepository,
                duplicateInvoiceTool,
                chatClient
        );
    }

    @Test
    @DisplayName("Should persist duplicate invoice finding with ruleSource=DETERMINISTIC and CRITICAL severity")
    void shouldPersistDeterministicDuplicateFinding() {
        Report report = new Report(1L, "march_expenses.pdf", "storage/2/march.pdf");
        report.setId(2L);
        report.setStatus(ReportStatus.AUDITING);

        ReportLineItem duplicateItem = new ReportLineItem(
                2L, "INV-2024-DUP", "Acme Supplies", new BigDecimal("1200.00"), "USD",
                "OFFICE", "Duplicate Supplies", 1
        );
        duplicateItem.setId(50L);

        when(reportRepository.findById(2L)).thenReturn(Optional.of(report));
        when(lineItemRepository.findByReportId(2L)).thenReturn(List.of(duplicateItem));

        when(policySearchService.searchPolicies(anyString(), anyInt(), anyDouble()))
                .thenReturn(List.of());

        when(auditPromptService.buildSystemPrompt()).thenReturn("System Prompt");

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("{\"complianceScore\":50,\"riskLevel\":\"HIGH\"}");

        // Result returned with DETERMINISTIC finding referencing original Report #1
        AuditFindingDto findingDto = new AuditFindingDto(
                "DETERMINISTIC",
                "CRITICAL",
                "Invoice INV-2024-DUP was verified in database as already claimed in Report #1",
                "Section 7: Prohibition of Duplicate Claims and Resubmissions",
                50L
        );
        AuditReportResult result = new AuditReportResult(50, "HIGH", List.of(findingDto), "Duplicate invoice detected");
        when(auditPromptService.parseResponse(anyString())).thenReturn(result);

        when(auditRunRepository.save(any(AuditRun.class))).thenAnswer(i -> i.getArgument(0));

        AuditRun run = orchestrationService.runAudit(2L);

        assertThat(run).isNotNull();
        assertThat(run.getComplianceScore()).isEqualTo(50);
        assertThat(run.getRiskLevel()).isEqualTo(RiskLevel.HIGH);

        // Verify finding persisted with ruleSource=DETERMINISTIC and severity=CRITICAL
        verify(auditFindingRepository).save(argThat(finding ->
                finding.getRuleSource() == RuleSource.DETERMINISTIC &&
                finding.getSeverity() == FindingSeverity.CRITICAL &&
                finding.getDescription().contains("Report #1")
        ));

        // Verify tool was set with current report ID and cleared afterwards
        verify(duplicateInvoiceTool).setCurrentReportId(2L);
        verify(duplicateInvoiceTool).clearCurrentReportId();
    }
}
