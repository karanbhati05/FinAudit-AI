package com.finaudit.api.service;

import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.api.storage.StorageService;
import com.finaudit.core.model.ExtractedLineItem;
import com.finaudit.core.model.ExtractedLineItemsList;
import com.finaudit.core.model.ReportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ReportParsingService {

    private static final Logger log = LoggerFactory.getLogger(ReportParsingService.class);

    private final ReportRepository reportRepository;
    private final ReportLineItemRepository lineItemRepository;
    private final StorageService storageService;
    private final DocumentExtractor documentExtractor;
    private final ChatClient chatClient;
    private final AuditOrchestrationService auditOrchestrationService;

    public ReportParsingService(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            StorageService storageService,
            DocumentExtractor documentExtractor,
            ChatClient.Builder chatClientBuilder,
            @org.springframework.context.annotation.Lazy AuditOrchestrationService auditOrchestrationService
    ) {
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.storageService = storageService;
        this.documentExtractor = documentExtractor;
        this.chatClient = chatClientBuilder.build();
        this.auditOrchestrationService = auditOrchestrationService;
    }

    // Constructor for testing with pre-built ChatClient
    public ReportParsingService(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            StorageService storageService,
            DocumentExtractor documentExtractor,
            ChatClient chatClient
    ) {
        this(reportRepository, lineItemRepository, storageService, documentExtractor, chatClient, null);
    }

    public ReportParsingService(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            StorageService storageService,
            DocumentExtractor documentExtractor,
            ChatClient chatClient,
            AuditOrchestrationService auditOrchestrationService
    ) {
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.storageService = storageService;
        this.documentExtractor = documentExtractor;
        this.chatClient = chatClient;
        this.auditOrchestrationService = auditOrchestrationService;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> parseReportAsync(Long reportId) {
        log.info("Starting asynchronous parsing for report ID: {}", reportId);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        try {
            report.setStatus(ReportStatus.PARSING);
            report.setErrorReason(null);
            reportRepository.save(report);

            // 1. Extract raw document text
            Path filePath = Paths.get(report.getStoragePath());
            String rawText = documentExtractor.extractText(filePath);

            if (rawText == null || rawText.isBlank()) {
                throw new IllegalStateException("Uploaded document contained no extractable text.");
            }

            // 2. Call Gemini via ChatClient with structured output
            String systemPrompt = """
                You are an expert financial audit parser.
                Extract every distinct invoice or expense line item from the document.
                For each item provide:
                - invoiceId: invoice or transaction ID (or 'INV-' + vendor + line number if not explicitly specified)
                - vendor: merchant, provider, or counterparty name
                - amount: total numerical amount
                - currency: ISO 3-letter currency code (e.g. USD, EUR, INR, GBP)
                - category: expense category (e.g. TRAVEL, MEALS, LODGING, SOFTWARE, OFFICE_SUPPLIES)
                - rawLineText: the raw text representation of this item
                Return only structured data matching the schema.
                """;

            ExtractedLineItemsList extractedData = chatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u.text("Extract all financial line items from this document text:\n\n{text}").param("text", rawText))
                    .call()
                    .entity(ExtractedLineItemsList.class);

            List<ExtractedLineItem> items = extractedData != null ? extractedData.items() : List.of();

            // 3. Persist line items
            int lineNumber = 1;
            for (ExtractedLineItem item : items) {
                ReportLineItem entity = new ReportLineItem(
                        report.getId(),
                        item.invoiceId() != null ? item.invoiceId() : "INV-" + lineNumber,
                        item.vendor() != null ? item.vendor() : "UNKNOWN",
                        item.amount(),
                        item.currency() != null ? item.currency() : "USD",
                        item.category(),
                        item.rawLineText(),
                        lineNumber++
                );
                lineItemRepository.save(entity);
            }

            // 4. Update status to AUDITING
            report.setStatus(ReportStatus.AUDITING);
            reportRepository.save(report);
            log.info("Completed parsing report ID: {}. Extracted {} line items.", reportId, items.size());

            // 5. Automatically trigger runAudit as the next step in the async pipeline
            if (auditOrchestrationService != null) {
                log.info("Automatically launching RAG audit for report ID: {}", reportId);
                auditOrchestrationService.runAudit(reportId);
            }

        } catch (Exception e) {
            log.error("Failed to parse report ID: {}", reportId, e);
            report.setStatus(ReportStatus.FAILED);
            report.setErrorReason(e.getMessage());
            reportRepository.save(report);
        }

        return CompletableFuture.completedFuture(null);
    }
}
