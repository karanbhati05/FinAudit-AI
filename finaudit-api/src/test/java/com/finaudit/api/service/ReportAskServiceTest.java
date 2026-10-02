package com.finaudit.api.service;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.exception.CircuitBreakerOpenException;
import com.finaudit.api.exception.RateLimitExceededException;
import com.finaudit.api.repository.*;
import com.finaudit.core.model.AskResponse;
import com.finaudit.core.model.FindingSeverity;
import com.finaudit.core.model.ReportStatus;
import com.finaudit.core.model.RiskLevel;
import com.finaudit.core.model.RuleSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportAskServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportLineItemRepository lineItemRepository;

    @Mock
    private AuditFindingRepository auditFindingRepository;

    @Mock
    private AuditRunRepository auditRunRepository;

    @Mock
    private CompliancePolicyRepository compliancePolicyRepository;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private CostGuardrailService costGuardrailService;
    private ReportAskService reportAskService;

    @BeforeEach
    void setUp() {
        // Use real CostGuardrailService with limits (userLimit=5, ipLimit=10, concurrency=2, dailyGeminiCalls=100)
        costGuardrailService = new CostGuardrailService(5, 10, 2, 100);

        reportAskService = new ReportAskService(
                reportRepository,
                lineItemRepository,
                auditFindingRepository,
                auditRunRepository,
                compliancePolicyRepository,
                costGuardrailService,
                chatClient
        );
    }

    private void mockChatClientPrompt(String responseText) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(responseText);
    }

    @Test
    @DisplayName("Question outside scope ('What's the weather today?') gets grounded=false and exact refusal string")
    void shouldRefuseOutOfScopeQuestionWithExactRefusalString() {
        Report report = new Report(1L, "expenses.pdf", "path");
        report.setId(10L);
        report.setStatus(ReportStatus.COMPLETE);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(lineItemRepository.findByReportId(10L)).thenReturn(List.of());
        when(auditFindingRepository.findByReportId(10L)).thenReturn(List.of());
        when(auditRunRepository.findByReportId(10L)).thenReturn(Optional.empty());

        mockChatClientPrompt("That's not something I can answer from this report's data.");

        AskResponse response = reportAskService.askQuestion(10L, "What's the weather today?", "session-abc");

        assertThat(response.grounded()).isFalse();
        assertThat(response.sourceType()).isEqualTo("NONE");
        assertThat(response.answer()).isEqualTo("That's not something I can answer from this report's data.");
    }

    @Test
    @DisplayName("Totals question returns Java-computed number verbatim in answer, not model-invented one")
    void shouldReturnJavaComputedTotalsVerbatimInAnswer() {
        Report report = new Report(1L, "expenses.pdf", "path");
        report.setId(10L);
        report.setStatus(ReportStatus.COMPLETE);

        ReportLineItem item1 = new ReportLineItem(10L, "INV-1011", "United Airlines", new BigDecimal("1850.00"), "USD", "Airfare", "Flight", 1);
        item1.setId(101L);
        ReportLineItem item2 = new ReportLineItem(10L, "INV-1012", "Hilton Hotels", new BigDecimal("660.00"), "USD", "Lodging", "Hotel", 2);
        item2.setId(102L);
        ReportLineItem item3 = new ReportLineItem(10L, "INV-1013", "Office Depot", new BigDecimal("120.00"), "USD", "Supplies", "Paper", 3);
        item3.setId(103L);

        // Flag item 1 and item 2: Total flagged = 1850.00 + 660.00 = 2510.00
        AuditFinding finding1 = new AuditFinding(10L, 101L, RuleSource.SEMANTIC, FindingSeverity.HIGH, "Business class ticket", "Clause 4.2");
        AuditFinding finding2 = new AuditFinding(10L, 102L, RuleSource.SEMANTIC, FindingSeverity.MEDIUM, "Per diem exceeded", "Clause 5.1");

        AuditRun auditRun = new AuditRun(10L, 72, RiskLevel.HIGH, "Summary");

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(lineItemRepository.findByReportId(10L)).thenReturn(List.of(item1, item2, item3));
        when(auditFindingRepository.findByReportId(10L)).thenReturn(List.of(finding1, finding2));
        when(auditRunRepository.findByReportId(10L)).thenReturn(Optional.of(auditRun));
        when(compliancePolicyRepository.findAll()).thenReturn(List.of());

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(userPromptCaptor.capture())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        // The model returns the answer utilizing the injected number
        when(callSpec.content()).thenReturn("Your total flagged amount is $2,510.00 across 2 flagged line items.");

        AskResponse response = reportAskService.askQuestion(10L, "What's my total flagged amount?", "session-abc");

        // 1. Assert the Java-computed number was injected into the prompt
        String capturedPrompt = userPromptCaptor.getValue();
        assertThat(capturedPrompt).contains("- Total Flagged Amount: $2,510.00");
        assertThat(capturedPrompt).contains("- Total Spend Amount: $2,630.00");
        assertThat(capturedPrompt).contains("- Flagged Line Items Count: 2");

        // 2. Assert response contains the Java-computed number verbatim
        assertThat(response.grounded()).isTrue();
        assertThat(response.answer()).contains("$2,510.00");
        assertThat(response.sourceType()).isEqualTo("LINE_ITEM");
    }

    @Test
    @DisplayName("Should enforce per-session rate limit cap (5 questions per visitor session)")
    void shouldEnforcePerSessionRateLimit() {
        Report report = new Report(1L, "expenses.pdf", "path");
        report.setId(10L);
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(lineItemRepository.findByReportId(10L)).thenReturn(List.of());
        when(auditFindingRepository.findByReportId(10L)).thenReturn(List.of());
        when(auditRunRepository.findByReportId(10L)).thenReturn(Optional.empty());

        mockChatClientPrompt("Answer");

        String sessionId = "visitor-session-xyz";

        // First 5 questions should succeed
        for (int i = 0; i < 5; i++) {
            AskResponse resp = reportAskService.askQuestion(10L, "Question " + i, sessionId);
            assertThat(resp.grounded()).isTrue();
        }

        // 6th question should be blocked by per-session cap
        assertThatThrownBy(() -> reportAskService.askQuestion(10L, "Question 6", sessionId))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Session question limit reached");
    }

    @Test
    @DisplayName("Should enforce per-report rate limit cap (10 questions per report daily)")
    void shouldEnforcePerReportRateLimit() {
        Report report = new Report(1L, "expenses.pdf", "path");
        report.setId(25L);
        when(reportRepository.findById(25L)).thenReturn(Optional.of(report));
        when(lineItemRepository.findByReportId(25L)).thenReturn(List.of());
        when(auditFindingRepository.findByReportId(25L)).thenReturn(List.of());
        when(auditRunRepository.findByReportId(25L)).thenReturn(Optional.empty());

        mockChatClientPrompt("Answer");

        // Use distinct sessions to test per-report cap
        for (int i = 0; i < 10; i++) {
            AskResponse resp = reportAskService.askQuestion(25L, "Question " + i, "session-" + i);
            assertThat(resp.grounded()).isTrue();
        }

        // 11th question for this report should be blocked
        assertThatThrownBy(() -> reportAskService.askQuestion(25L, "Question 11", "session-11"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Daily question limit reached for this report");
    }

    @Test
    @DisplayName("Should trip circuit breaker when daily quota is exceeded")
    void shouldTripCircuitBreakerWhenDailyQuotaExceeded() {
        // Create guardrail with daily limit of 2 calls
        CostGuardrailService limitedGuardrails = new CostGuardrailService(5, 10, 2, 2);
        ReportAskService limitedService = new ReportAskService(
                reportRepository, lineItemRepository, auditFindingRepository,
                auditRunRepository, compliancePolicyRepository, limitedGuardrails, chatClient
        );

        Report report = new Report(1L, "expenses.pdf", "path");
        report.setId(30L);
        when(reportRepository.findById(30L)).thenReturn(Optional.of(report));
        when(lineItemRepository.findByReportId(30L)).thenReturn(List.of());
        when(auditFindingRepository.findByReportId(30L)).thenReturn(List.of());
        when(auditRunRepository.findByReportId(30L)).thenReturn(Optional.empty());

        mockChatClientPrompt("Answer");

        // 2 calls consume the daily quota
        limitedService.askQuestion(30L, "Q1", "s1");
        limitedService.askQuestion(30L, "Q2", "s2");

        // 3rd call trips circuit breaker
        assertThatThrownBy(() -> limitedService.askQuestion(30L, "Q3", "s3"))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessageContaining("daily AI quota");
    }
}
