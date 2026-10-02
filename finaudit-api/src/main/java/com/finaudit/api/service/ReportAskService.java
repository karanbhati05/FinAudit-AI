package com.finaudit.api.service;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.CompliancePolicy;
import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.CompliancePolicyRepository;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.AskResponse;
import com.finaudit.core.model.RiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Executes tightly grounded, single-purpose report Q&A with deterministic
 * numeric injection and hard refusal instructions.
 */
@Service
public class ReportAskService {

    private static final Logger log = LoggerFactory.getLogger(ReportAskService.class);

    public static final String RIGID_SYSTEM_PROMPT =
            "You will answer using ONLY the DATA block below. If the answer requires information not present in DATA, " +
            "respond exactly with: 'That's not something I can answer from this report's data.' " +
            "Do not estimate, do not use general knowledge about expense policies, do not perform multi-step inference " +
            "beyond directly reading the provided fields.";

    public static final String REFUSAL_TEXT = "That's not something I can answer from this report's data.";

    private final ReportRepository reportRepository;
    private final ReportLineItemRepository lineItemRepository;
    private final AuditFindingRepository auditFindingRepository;
    private final AuditRunRepository auditRunRepository;
    private final CompliancePolicyRepository compliancePolicyRepository;
    private final CostGuardrailService costGuardrailService;
    private final ChatClient chatClient;

    @org.springframework.beans.factory.annotation.Autowired
    public ReportAskService(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            AuditFindingRepository auditFindingRepository,
            AuditRunRepository auditRunRepository,
            CompliancePolicyRepository compliancePolicyRepository,
            CostGuardrailService costGuardrailService,
            ChatClient.Builder chatClientBuilder
    ) {
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.auditRunRepository = auditRunRepository;
        this.compliancePolicyRepository = compliancePolicyRepository;
        this.costGuardrailService = costGuardrailService;
        this.chatClient = chatClientBuilder.build();
    }

    // Constructor for testing with pre-built ChatClient
    public ReportAskService(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            AuditFindingRepository auditFindingRepository,
            AuditRunRepository auditRunRepository,
            CompliancePolicyRepository compliancePolicyRepository,
            CostGuardrailService costGuardrailService,
            ChatClient chatClient
    ) {
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.auditRunRepository = auditRunRepository;
        this.compliancePolicyRepository = compliancePolicyRepository;
        this.costGuardrailService = costGuardrailService;
        this.chatClient = chatClient;
    }

    @Transactional(readOnly = true)
    public AskResponse askQuestion(Long reportId, String question, String sessionId) {
        if (question == null || question.isBlank()) {
            return AskResponse.ungrounded(REFUSAL_TEXT);
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found for ID: " + reportId));

        // 1. Rate Limiting: Circuit Breaker + per-report (10) + per-session (5)
        costGuardrailService.enforceQuestionRateLimits(reportId, sessionId);

        // 2. Fetch line items and findings
        List<ReportLineItem> lineItems = lineItemRepository.findByReportId(reportId);
        List<AuditFinding> findings = auditFindingRepository.findByReportId(reportId);
        Optional<AuditRun> auditRunOpt = auditRunRepository.findByReportId(reportId);

        // 3. Preload ONLY the policy chunks already cited for this report (no new vector searches)
        Map<String, CompliancePolicy> policyLookup = new HashMap<>();
        List<CompliancePolicy> allPolicies = compliancePolicyRepository.findAll();
        for (CompliancePolicy p : allPolicies) {
            policyLookup.put(p.getTitle().toLowerCase().trim(), p);
        }

        Map<String, String> citedPolicies = new LinkedHashMap<>();
        for (AuditFinding f : findings) {
            String ref = f.getPolicyReference();
            if (ref != null && !ref.isBlank() && !ref.equalsIgnoreCase("null")) {
                CompliancePolicy matched = policyLookup.get(ref.toLowerCase().trim());
                if (matched == null) {
                    matched = allPolicies.stream()
                            .filter(p -> p.getTitle().toLowerCase().contains(ref.toLowerCase()) || ref.toLowerCase().contains(p.getTitle().toLowerCase()))
                            .findFirst()
                            .orElse(null);
                }
                if (matched != null) {
                    citedPolicies.put(matched.getTitle(), matched.getBodyText());
                } else {
                    citedPolicies.put(ref, "Policy referenced by finding: " + ref);
                }
            }
        }

        // 4. Compute deterministic arithmetic in Java BEFORE LLM call
        Map<Long, ReportLineItem> itemById = lineItems.stream()
                .filter(i -> i.getId() != null)
                .collect(Collectors.toMap(ReportLineItem::getId, i -> i, (a, b) -> a));

        Set<Long> flaggedItemIds = findings.stream()
                .map(AuditFinding::getLineItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        BigDecimal totalSpend = lineItems.stream()
                .map(ReportLineItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalFlaggedAmount = lineItems.stream()
                .filter(i -> i.getId() != null && flaggedItemIds.contains(i.getId()))
                .map(ReportLineItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int totalCount = lineItems.size();
        int flaggedCount = (int) lineItems.stream()
                .filter(i -> i.getId() != null && flaggedItemIds.contains(i.getId()))
                .count();

        // Spend by category
        Map<String, BigDecimal> categorySpend = new LinkedHashMap<>();
        for (ReportLineItem item : lineItems) {
            String cat = item.getCategory() != null && !item.getCategory().isBlank() ? item.getCategory().trim() : "Uncategorized";
            BigDecimal amt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
            categorySpend.merge(cat, amt, BigDecimal::add);
        }
        StringBuilder catSpendStr = new StringBuilder();
        categorySpend.forEach((k, v) -> catSpendStr.append(k).append(": $").append(String.format(Locale.US, "%,.2f", v)).append("; "));

        Integer complianceScore = auditRunOpt.map(AuditRun::getComplianceScore).orElse(null);
        RiskLevel riskLevel = auditRunOpt.map(AuditRun::getRiskLevel).orElse(null);

        // 5. Build compact, rigid DATA block
        StringBuilder dataBlock = new StringBuilder();
        dataBlock.append("=== DATA BLOCK ===\n");
        dataBlock.append("[REPORT INFO]\n");
        dataBlock.append("Report ID: ").append(report.getId()).append("\n");
        dataBlock.append("Filename: ").append(report.getOriginalFilename()).append("\n");
        dataBlock.append("Compliance Score: ").append(complianceScore != null ? complianceScore + "/100" : "N/A").append("\n");
        dataBlock.append("Risk Level: ").append(riskLevel != null ? riskLevel.name() : "N/A").append("\n");

        dataBlock.append("\n[COMPUTED SUMMARY FACTS (USE THESE EXACT PRE-COMPUTED NUMBERS FOR ARITHMETIC / TOTALS)]\n");
        dataBlock.append("- Total Line Items Count: ").append(totalCount).append("\n");
        dataBlock.append("- Flagged Line Items Count: ").append(flaggedCount).append("\n");
        dataBlock.append("- Total Spend Amount: $").append(String.format(Locale.US, "%,.2f", totalSpend)).append("\n");
        dataBlock.append("- Total Flagged Amount: $").append(String.format(Locale.US, "%,.2f", totalFlaggedAmount)).append("\n");
        dataBlock.append("- Spend By Category: ").append(catSpendStr).append("\n");
        dataBlock.append("- Deterministic Summary: ").append(flaggedCount).append(" of ").append(totalCount)
                .append(" line items flagged, $").append(String.format(Locale.US, "%,.2f", totalFlaggedAmount)).append(" total flagged amount\n");

        dataBlock.append("\n[LINE ITEMS TABLE]\n");
        dataBlock.append("| Line # | Invoice ID | Vendor | Amount | Currency | Category | Raw Text |\n");
        int rowNum = 1;
        for (ReportLineItem item : lineItems) {
            dataBlock.append("| ")
                    .append(item.getLineNumber() != null ? item.getLineNumber() : rowNum++).append(" | ")
                    .append(item.getInvoiceId() != null ? item.getInvoiceId() : "N/A").append(" | ")
                    .append(item.getVendor() != null ? item.getVendor() : "N/A").append(" | ")
                    .append(item.getAmount() != null ? String.format(Locale.US, "%.2f", item.getAmount()) : "0.00").append(" | ")
                    .append(item.getCurrency() != null ? item.getCurrency() : "USD").append(" | ")
                    .append(item.getCategory() != null ? item.getCategory() : "N/A").append(" | ")
                    .append(item.getRawText() != null ? item.getRawText().replace("\n", " ").trim() : "").append(" |\n");
        }

        dataBlock.append("\n[AUDIT FINDINGS]\n");
        if (findings.isEmpty()) {
            dataBlock.append("No audit findings or policy violations recorded for this report.\n");
        } else {
            for (AuditFinding f : findings) {
                ReportLineItem li = f.getLineItemId() != null ? itemById.get(f.getLineItemId()) : null;
                String itemDesc = li != null ? "Line Item #" + (li.getLineNumber() != null ? li.getLineNumber() : "?") + " (" + li.getVendor() + ")" : "General";
                dataBlock.append("- ").append(itemDesc).append(": [").append(f.getSeverity()).append("] ")
                        .append(f.getDescription())
                        .append(f.getPolicyReference() != null ? " (Cited Policy: " + f.getPolicyReference() + ")" : "")
                        .append("\n");
            }
        }

        dataBlock.append("\n[CITED POLICIES]\n");
        if (citedPolicies.isEmpty()) {
            dataBlock.append("No specific corporate policy clauses were retrieved or cited during audit.\n");
        } else {
            for (Map.Entry<String, String> entry : citedPolicies.entrySet()) {
                dataBlock.append("- Policy: ").append(entry.getKey()).append("\n");
                dataBlock.append("  Clause Text: \"").append(entry.getValue().replace("\n", " ").trim()).append("\"\n");
            }
        }
        dataBlock.append("=== END DATA BLOCK ===\n");

        String userPrompt = "DATA:\n" + dataBlock + "\n\nQUESTION:\n" + question.trim();

        // 6. Record question against limits
        costGuardrailService.recordQuestion(reportId, sessionId);

        // 7. Call LLM
        log.info("Sending scoped question to ChatClient for report ID: {}", reportId);
        String rawAnswer;
        try {
            rawAnswer = chatClient.prompt()
                    .system(RIGID_SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Failed executing ChatClient call for question on report ID: {}", reportId, e);
            throw e;
        }

        String trimmed = rawAnswer != null ? rawAnswer.trim() : "";

        // 8. Groundedness and Refusal Evaluation
        if (trimmed.isEmpty() ||
                trimmed.equalsIgnoreCase(REFUSAL_TEXT) ||
                trimmed.toLowerCase().contains("not something i can answer from this report") ||
                trimmed.toLowerCase().contains("not something i can answer")) {
            return new AskResponse(REFUSAL_TEXT, false, "NONE");
        }

        // Determine sourceType: "LINE_ITEM" | "POLICY" | "FINDING" | "NONE"
        String qLower = question.toLowerCase();
        String aLower = trimmed.toLowerCase();
        String sourceType = "LINE_ITEM";

        if (qLower.contains("policy") || qLower.contains("clause") || qLower.contains("rule") ||
                qLower.contains("standard") || aLower.contains("policy") || aLower.contains("clause")) {
            sourceType = "POLICY";
        } else if (qLower.contains("total") || qLower.contains("amount") || qLower.contains("spend") ||
                qLower.contains("sum") || qLower.contains("average") || qLower.contains("how much") ||
                qLower.contains("vendor") || qLower.contains("invoice") || qLower.contains("how many")) {
            sourceType = "LINE_ITEM";
        } else if (qLower.contains("flag") || qLower.contains("finding") || qLower.contains("violation") ||
                qLower.contains("severity") || qLower.contains("why") || aLower.contains("violation") || aLower.contains("finding")) {
            sourceType = "FINDING";
        }

        return new AskResponse(trimmed, true, sourceType);
    }
}
