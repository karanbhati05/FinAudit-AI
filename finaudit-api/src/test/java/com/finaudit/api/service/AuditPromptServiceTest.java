package com.finaudit.api.service;

import com.finaudit.core.model.AuditReportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AuditPromptServiceTest {

    private AuditPromptService auditPromptService;

    @BeforeEach
    void setUp() {
        String templateContent = "System Prompt Header\n{format}\nSystem Prompt Footer";
        Resource resource = new ByteArrayResource(templateContent.getBytes(StandardCharsets.UTF_8));
        auditPromptService = new AuditPromptService(resource);
    }

    @Test
    @DisplayName("Should correctly deserialize JSON response with empty flagged items")
    void shouldDeserializeEmptyFlaggedItems() {
        String rawJson = """
            {
              "complianceScore": 100,
              "riskLevel": "LOW",
              "flaggedItems": [],
              "summary": "All line items are fully compliant with company spending policies."
            }
            """;

        AuditReportResult result = auditPromptService.parseResponse(rawJson);

        assertThat(result).isNotNull();
        assertThat(result.complianceScore()).isEqualTo(100);
        assertThat(result.riskLevel()).isEqualTo("LOW");
        assertThat(result.flaggedItems()).isEmpty();
        assertThat(result.summary()).contains("fully compliant");
    }

    @Test
    @DisplayName("Should correctly deserialize JSON response with multiple findings")
    void shouldDeserializeMultipleFindings() {
        String rawJson = """
            {
              "complianceScore": 60,
              "riskLevel": "HIGH",
              "flaggedItems": [
                {
                  "ruleSource": "SEMANTIC",
                  "severity": "HIGH",
                  "description": "Flight expense of $3,200 without VP pre-approval",
                  "policyReference": "Section 2: International Travel & Flight Class Pre-Approval",
                  "lineItemId": 101
                },
                {
                  "ruleSource": "DETERMINISTIC",
                  "severity": "CRITICAL",
                  "description": "Invoice INV-2024-001 from Acme was already claimed in Report #4",
                  "policyReference": "Section 7: Prohibition of Duplicate Claims and Resubmissions",
                  "lineItemId": 102
                }
              ],
              "summary": "Multiple serious violations found including unauthorized travel and duplicate invoice."
            }
            """;

        AuditReportResult result = auditPromptService.parseResponse(rawJson);

        assertThat(result).isNotNull();
        assertThat(result.complianceScore()).isEqualTo(60);
        assertThat(result.riskLevel()).isEqualTo("HIGH");
        assertThat(result.flaggedItems()).hasSize(2);

        var firstFinding = result.flaggedItems().get(0);
        assertThat(firstFinding.ruleSource()).isEqualTo("SEMANTIC");
        assertThat(firstFinding.severity()).isEqualTo("HIGH");
        assertThat(firstFinding.lineItemId()).isEqualTo(101L);
        assertThat(firstFinding.policyReference()).contains("Section 2");

        var secondFinding = result.flaggedItems().get(1);
        assertThat(secondFinding.ruleSource()).isEqualTo("DETERMINISTIC");
        assertThat(secondFinding.severity()).isEqualTo("CRITICAL");
        assertThat(secondFinding.lineItemId()).isEqualTo(102L);
    }

    @Test
    @DisplayName("Should successfully strip markdown code fences from JSON")
    void shouldStripMarkdownFences() {
        String wrappedJson = """
            ```json
            {
              "complianceScore": 85,
              "riskLevel": "MEDIUM",
              "flaggedItems": [],
              "summary": "Minor issues noted."
            }
            ```
            """;

        AuditReportResult result = auditPromptService.parseResponse(wrappedJson);

        assertThat(result).isNotNull();
        assertThat(result.complianceScore()).isEqualTo(85);
        assertThat(result.riskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("Should render system prompt with injected JSON schema format instructions")
    void shouldRenderSystemPromptWithFormat() {
        String systemPrompt = auditPromptService.buildSystemPrompt();

        assertThat(systemPrompt).contains("System Prompt Header");
        assertThat(systemPrompt).contains("System Prompt Footer");
        assertThat(systemPrompt).contains("complianceScore");
        assertThat(systemPrompt).contains("riskLevel");
    }
}
