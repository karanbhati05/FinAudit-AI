package com.finaudit.api.service;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.AuditFindingDto;
import com.finaudit.core.model.AuditReportResult;
import com.finaudit.core.model.ReportStatus;
import com.finaudit.core.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditOrchestrationServiceTest {

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
                chatClient
        );
    }

    @Test
    @DisplayName("Should orchestrate full audit, match policy chunks, persist run & findings, and complete report")
    void shouldOrchestrateFullAudit() {
        Report report = new Report(1L, "travel_q1.pdf", "storage/reports/1/travel_q1.pdf");
        report.setId(1L);
        report.setStatus(ReportStatus.AUDITING);

        ReportLineItem item = new ReportLineItem(
                1L, "INV-771", "Luxury Jet Airways", new BigDecimal("4500.00"), "USD",
                "TRAVEL", "Business class flight to London", 1
        );
        item.setId(10L);

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(lineItemRepository.findByReportId(1L)).thenReturn(List.of(item));

        Document policyDoc = new Document("International business class requires VP pre-approval",
                Map.of("title", "Section 2: International Travel & Flight Class Pre-Approval"));
        when(policySearchService.searchPolicies(anyString(), anyInt(), anyDouble()))
                .thenReturn(List.of(policyDoc));

        when(auditPromptService.buildSystemPrompt()).thenReturn("System Prompt");

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("{\"complianceScore\":75,\"riskLevel\":\"MEDIUM\"}");

        AuditFindingDto findingDto = new AuditFindingDto(
                "SEMANTIC",
                "HIGH",
                "Business class claimed without VP pre-approval",
                "Section 2: International Travel & Flight Class Pre-Approval",
                10L
        );
        AuditReportResult result = new AuditReportResult(75, "MEDIUM", List.of(findingDto), "1 finding");
        when(auditPromptService.parseResponse(anyString())).thenReturn(result);

        when(auditRunRepository.save(any(AuditRun.class))).thenAnswer(i -> i.getArgument(0));

        AuditRun run = orchestrationService.runAudit(1L);

        assertThat(run).isNotNull();
        assertThat(run.getComplianceScore()).isEqualTo(75);
        assertThat(run.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);

        // Verify finding saved with policyReference linked back
        verify(auditFindingRepository, times(1)).save(argThat(finding ->
                finding.getPolicyReference().contains("Section 2") &&
                finding.getSeverity().name().equals("HIGH")
        ));

        // Verify status transition
        verify(reportRepository).save(report);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETE);
        assertThat(report.getAuditedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle no policies retrieved above threshold gracefully by setting policyReference to null")
    void shouldHandleNoPoliciesRetrievedEdgeCase() {
        Report report = new Report(2L, "misc_supplies.txt", "storage/reports/2/misc_supplies.txt");
        report.setId(2L);
        report.setStatus(ReportStatus.AUDITING);

        ReportLineItem item = new ReportLineItem(
                2L, "INV-882", "Unknown Corner Store", new BigDecimal("120.00"), "USD",
                "MISCELLANEOUS", "Unusual miscellaneous purchase", 1
        );
        item.setId(20L);

        when(reportRepository.findById(2L)).thenReturn(Optional.of(report));
        when(lineItemRepository.findByReportId(2L)).thenReturn(List.of(item));

        // No policies match similarity threshold
        when(policySearchService.searchPolicies(anyString(), anyInt(), anyDouble()))
                .thenReturn(List.of());

        when(auditPromptService.buildSystemPrompt()).thenReturn("System Prompt");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("{\"complianceScore\":90,\"riskLevel\":\"LOW\"}");

        // Model returns finding with hallucinated or generic text
        AuditFindingDto findingDto = new AuditFindingDto(
                "SEMANTIC",
                "LOW",
                "Vague purchase description",
                "Hallucinated Policy Name",
                20L
        );
        AuditReportResult result = new AuditReportResult(90, "LOW", List.of(findingDto), "Minor flag");
        when(auditPromptService.parseResponse(anyString())).thenReturn(result);

        when(auditRunRepository.save(any(AuditRun.class))).thenAnswer(i -> i.getArgument(0));

        AuditRun run = orchestrationService.runAudit(2L);

        assertThat(run).isNotNull();
        // Finding must have null policyReference since no policies were retrieved!
        verify(auditFindingRepository).save(argThat(finding ->
                finding.getPolicyReference() == null &&
                finding.getRuleSource().name().equals("SEMANTIC")
        ));
        assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETE);
    }
}
