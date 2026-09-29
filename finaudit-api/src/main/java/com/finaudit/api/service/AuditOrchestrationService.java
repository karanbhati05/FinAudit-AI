package com.finaudit.api.service;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.finaudit.api.tool.DuplicateInvoiceDetectionTool;
import org.slf4j.MDC;
import java.util.UUID;

@Service
public class AuditOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AuditOrchestrationService.class);

    private final ReportRepository reportRepository;
    private final ReportLineItemRepository lineItemRepository;
    private final PolicySearchService policySearchService;
    private final AuditPromptService auditPromptService;
    private final AuditRunRepository auditRunRepository;
    private final AuditFindingRepository auditFindingRepository;
    private final DuplicateInvoiceDetectionTool duplicateInvoiceTool;
    private final ChatClient chatClient;
    private final CostGuardrailService costGuardrailService;

    @org.springframework.beans.factory.annotation.Autowired
    public AuditOrchestrationService(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            PolicySearchService policySearchService,
            AuditPromptService auditPromptService,
            AuditRunRepository auditRunRepository,
            AuditFindingRepository auditFindingRepository,
            DuplicateInvoiceDetectionTool duplicateInvoiceTool,
            ChatClient.Builder chatClientBuilder,
            CostGuardrailService costGuardrailService
    ) {
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.policySearchService = policySearchService;
        this.auditPromptService = auditPromptService;
        this.auditRunRepository = auditRunRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.duplicateInvoiceTool = duplicateInvoiceTool;
        this.chatClient = chatClientBuilder.build();
        this.costGuardrailService = costGuardrailService;
    }

    // Constructor for unit tests with pre-built ChatClient
    public AuditOrchestrationService(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            PolicySearchService policySearchService,
            AuditPromptService auditPromptService,
            AuditRunRepository auditRunRepository,
            AuditFindingRepository auditFindingRepository,
            DuplicateInvoiceDetectionTool duplicateInvoiceTool,
            ChatClient chatClient
    ) {
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.policySearchService = policySearchService;
        this.auditPromptService = auditPromptService;
        this.auditRunRepository = auditRunRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.duplicateInvoiceTool = duplicateInvoiceTool;
        this.chatClient = chatClient;
        this.costGuardrailService = null;
    }

    public AuditOrchestrationService(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            PolicySearchService policySearchService,
            AuditPromptService auditPromptService,
            AuditRunRepository auditRunRepository,
            AuditFindingRepository auditFindingRepository,
            ChatClient chatClient
    ) {
        this(reportRepository, lineItemRepository, policySearchService, auditPromptService, auditRunRepository, auditFindingRepository, null, chatClient);
    }

    @Transactional
    public AuditRun runAudit(Long reportId) {
        return runAudit(reportId, null);
    }

    @Transactional
    public AuditRun runAudit(Long reportId, String correlationId) {
        String effectiveCorrelationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("reportId", String.valueOf(reportId));
        MDC.put("correlationId", effectiveCorrelationId);

        try {
            log.info("Initiating RAG-grounded audit orchestration for report ID: {}", reportId);
            Instant startedAt = Instant.now();

            Report report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new IllegalArgumentException("Report not found for ID: " + reportId));

            List<ReportLineItem> lineItems = lineItemRepository.findByReportId(reportId);
            if (lineItems.isEmpty()) {
                log.warn("Report ID: {} has no line items. Recording empty audit run.", reportId);
                AuditRun cleanRun = new AuditRun(reportId, 100, RiskLevel.LOW, "{\"summary\":\"No line items present in report.\"}");
                cleanRun.setCompletedAt(Instant.now());
                cleanRun = auditRunRepository.save(cleanRun);
                report.setStatus(ReportStatus.COMPLETE);
                report.setAuditedAt(Instant.now());
                reportRepository.save(report);
                return cleanRun;
            }

            // --- Step 1: Policy Retrieval (RAG) ---
        // Cost & Latency Decision:
        // Instead of triggering N separate vector searches and LLM calls per line item, we aggregate
        // line item descriptions and categories to retrieve relevant policy chunks in batch.
        // A single consolidated LLM audit call drastically cuts latency and cost while providing
        // full report context to the model for holistic multi-item cross-checks.
        Set<String> searchQueries = lineItems.stream()
                .map(item -> (item.getCategory() != null ? item.getCategory() : "") + " " + item.getVendor() + " " + (item.getRawText() != null ? item.getRawText() : ""))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Map<String, Document> matchedPolicies = new LinkedHashMap<>();
        for (String query : searchQueries) {
            List<Document> docs = policySearchService.searchPolicies(query, 3, 0.60);
            for (Document doc : docs) {
                String title = (String) doc.getMetadata().getOrDefault("title", doc.getText());
                matchedPolicies.putIfAbsent(title, doc);
            }
        }

        StringBuilder policyContextBuilder = new StringBuilder();
        if (matchedPolicies.isEmpty()) {
            // Handle edge case where no policies match threshold:
            // Explicitly instruct model not to hallucinate policy references.
            policyContextBuilder.append("NOTICE: No specific corporate compliance policies matched the line items above the similarity threshold.\n");
            policyContextBuilder.append("Do NOT hallucinate or invent policy citations. If you flag an item based on general financial prudence, set 'policyReference' to null and 'ruleSource' to 'SEMANTIC'.\n");
        } else {
            policyContextBuilder.append("### Retrieved Applicable Compliance Policies:\n\n");
            for (Document doc : matchedPolicies.values()) {
                String title = (String) doc.getMetadata().getOrDefault("title", "Policy Clause");
                policyContextBuilder.append("#### ").append(title).append("\n");
                policyContextBuilder.append(doc.getText()).append("\n\n");
            }
        }

        // --- Step 2: Assemble Line Items Payload ---
        StringBuilder lineItemsBuilder = new StringBuilder("### Report Line Items to Audit:\n\n");
        for (ReportLineItem item : lineItems) {
            lineItemsBuilder.append(String.format(
                    "- Line %d [ID: %d]: Invoice: %s | Vendor: %s | Amount: %s %s | Category: %s | Text: %s\n",
                    item.getLineNumber() != null ? item.getLineNumber() : 0,
                    item.getId(),
                    item.getInvoiceId(),
                    item.getVendor(),
                    item.getAmount(),
                    item.getCurrency(),
                    item.getCategory() != null ? item.getCategory() : "N/A",
                    item.getRawText() != null ? item.getRawText() : ""
            ));
        }

        // --- Step 3: Execute Gemini Audit Call ---
        String systemPrompt = auditPromptService.buildSystemPrompt();
        String userPrompt = policyContextBuilder + "\n\n" + lineItemsBuilder;

        log.debug("Sending audit prompt to ChatClient for report ID: {}", reportId);
        if (duplicateInvoiceTool != null) {
            duplicateInvoiceTool.setCurrentReportId(reportId);
        }

        if (costGuardrailService != null) {
            costGuardrailService.tryAcquireAuditSlot();
        }

        String rawResponse;
        try {
            if (costGuardrailService != null) {
                costGuardrailService.recordGeminiCall();
            }

            var promptSpec = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt);

            if (duplicateInvoiceTool != null) {
                promptSpec = promptSpec.tools(duplicateInvoiceTool);
            }

            rawResponse = promptSpec.call().content();
        } finally {
            if (duplicateInvoiceTool != null) {
                duplicateInvoiceTool.clearCurrentReportId();
            }
            if (costGuardrailService != null) {
                costGuardrailService.releaseAuditSlot();
            }
        }

        // --- Step 4: Parse Structured Output ---
        AuditReportResult auditResult = auditPromptService.parseResponse(rawResponse);

        // Map risk level
        RiskLevel riskLevel;
        try {
            riskLevel = RiskLevel.valueOf(auditResult.riskLevel().toUpperCase());
        } catch (Exception e) {
            int score = auditResult.complianceScore();
            riskLevel = score >= 90 ? RiskLevel.LOW : (score >= 70 ? RiskLevel.MEDIUM : RiskLevel.HIGH);
        }

        // --- Step 5: Persist AuditRun & Findings ---
        AuditRun auditRun = new AuditRun(reportId, auditResult.complianceScore(), riskLevel, rawResponse);
        auditRun.setStartedAt(startedAt);
        auditRun.setCompletedAt(Instant.now());
        auditRun = auditRunRepository.save(auditRun);

        for (AuditFindingDto findingDto : auditResult.flaggedItems()) {
            RuleSource ruleSource = "DETERMINISTIC".equalsIgnoreCase(findingDto.ruleSource())
                    ? RuleSource.DETERMINISTIC
                    : RuleSource.SEMANTIC;

            FindingSeverity severity;
            try {
                severity = FindingSeverity.valueOf(findingDto.severity().toUpperCase());
            } catch (Exception e) {
                severity = FindingSeverity.MEDIUM;
            }

            // Verify policy citation: if similarity search returned nothing, force null policyReference
            String policyRef = matchedPolicies.isEmpty() ? null : findingDto.policyReference();

            AuditFinding finding = new AuditFinding(
                    reportId,
                    findingDto.lineItemId(),
                    ruleSource,
                    severity,
                    findingDto.description(),
                    policyRef
            );
            auditFindingRepository.save(finding);
        }

        // --- Step 6: Transition Report to COMPLETE ---
        report.setStatus(ReportStatus.COMPLETE);
        report.setAuditedAt(Instant.now());
        reportRepository.save(report);

            log.info("Successfully audited report ID: {}. Score: {}, Risk: {}, Findings: {}",
                    reportId, auditRun.getComplianceScore(), auditRun.getRiskLevel(), auditResult.flaggedItems().size());

            return auditRun;
        } catch (Exception e) {
            log.error("Failed to execute RAG audit for report ID: {}", reportId, e);
            try {
                Report failedReport = reportRepository.findById(reportId).orElse(null);
                if (failedReport != null) {
                    failedReport.setStatus(ReportStatus.FAILED);
                    failedReport.setErrorReason("Audit failed: " + (e.getMessage() != null ? e.getMessage() : "Unexpected error during AI audit."));
                    reportRepository.save(failedReport);
                }
            } catch (Exception inner) {
                log.error("Failed to mark report ID: {} as FAILED", reportId, inner);
            }
            throw e;
        } finally {
            MDC.remove("reportId");
            MDC.remove("correlationId");
        }
    }
}
