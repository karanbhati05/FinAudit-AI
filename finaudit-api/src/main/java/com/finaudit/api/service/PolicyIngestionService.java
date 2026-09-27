package com.finaudit.api.service;

import com.finaudit.api.entity.CompliancePolicy;
import com.finaudit.api.repository.CompliancePolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class PolicyIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PolicyIngestionService.class);

    private final CompliancePolicyRepository policyRepository;
    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;
    private final Environment environment;

    public PolicyIngestionService(
            CompliancePolicyRepository policyRepository,
            VectorStore vectorStore,
            ResourceLoader resourceLoader,
            Environment environment
    ) {
        this.policyRepository = policyRepository;
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("seed-policies")) {
            log.info("Active profile contains 'seed-policies' — initiating compliance policy ingestion.");
            ingestSeedPolicies();
        }
    }

    @Transactional
    public int ingestSeedPolicies() {
        try {
            Resource resource = resourceLoader.getResource("classpath:policies/corporate-spending-policy.md");
            if (!resource.exists()) {
                throw new IllegalStateException("Policy seed file classpath:policies/corporate-spending-policy.md not found.");
            }

            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            return ingestPolicyMarkdown(content, "Corporate Spending Policy 2024", LocalDate.of(2024, 1, 1));
        } catch (IOException e) {
            log.error("Failed to read policy seed markdown", e);
            throw new RuntimeException("Policy ingestion failed", e);
        }
    }

    @Transactional
    public int ingestPolicyMarkdown(String markdownContent, String defaultTitle, LocalDate effectiveDate) {
        log.info("Starting policy parsing and vector store ingestion...");
        String[] sections = markdownContent.split("(?m)^##\\s+");

        List<Document> allChunks = new ArrayList<>();
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        int savedPolicyCount = 0;

        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("# ")) {
                continue; // Skip the main top-level title header
            }

            // Extract section title from the first line
            int firstNewline = trimmed.indexOf('\n');
            String title = firstNewline > 0 ? trimmed.substring(0, firstNewline).trim() : defaultTitle;
            String body = firstNewline > 0 ? trimmed.substring(firstNewline).trim() : trimmed;

            String category = determineCategory(title, body);

            // 1. Save or update CompliancePolicy in Postgres
            Optional<CompliancePolicy> existing = policyRepository.findByTitle(title);
            CompliancePolicy policy;
            if (existing.isPresent()) {
                policy = existing.get();
                policy.setBodyText(body);
                policy.setCategory(category);
                policy.setEffectiveDate(effectiveDate);
            } else {
                policy = new CompliancePolicy(title, body, category, effectiveDate);
            }
            policy = policyRepository.save(policy);
            savedPolicyCount++;

            // 2. Chunk text using Spring AI TokenTextSplitter
            Document sectionDoc = new Document(
                    title + "\n\n" + body,
                    Map.of(
                            "policyId", String.valueOf(policy.getId()),
                            "title", title,
                            "category", category
                    )
            );

            List<Document> chunks = textSplitter.apply(List.of(sectionDoc));
            allChunks.addAll(chunks);
        }

        // 3. Write embeddings to PgVectorStore
        if (!allChunks.isEmpty()) {
            vectorStore.add(allChunks);
            log.info("Successfully ingested {} policy sections ({} chunks) into vector store.", savedPolicyCount, allChunks.size());
        }

        return savedPolicyCount;
    }

    private String determineCategory(String title, String body) {
        String combined = (title + " " + body).toUpperCase();
        if (combined.contains("TRAVEL") || combined.contains("LODGING") || combined.contains("FLIGHT")) {
            return "TRAVEL";
        } else if (combined.contains("VENDOR") || combined.contains("SOFTWARE") || combined.contains("IT")) {
            return "IT_SOFTWARE";
        } else if (combined.contains("DINING") || combined.contains("ENTERTAINMENT") || combined.contains("ALCOHOL")) {
            return "ENTERTAINMENT";
        } else if (combined.contains("PER-DIEM") || combined.contains("MEAL")) {
            return "PER_DIEM";
        } else if (combined.contains("APPROVAL") || combined.contains("THRESHOLD")) {
            return "APPROVAL_LIMITS";
        } else if (combined.contains("DUPLICATE") || combined.contains("RESUBMISSION")) {
            return "DUPLICATE_CLAIM";
        }
        return "GENERAL_COMPLIANCE";
    }
}
