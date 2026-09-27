package com.finaudit.api.service;

import com.finaudit.core.model.AuditReportResult;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditPromptService {

    private final Resource promptResource;
    private final BeanOutputConverter<AuditReportResult> outputConverter;

    public AuditPromptService(@Value("classpath:prompts/audit-system-prompt.st") Resource promptResource) {
        this.promptResource = promptResource;
        this.outputConverter = new BeanOutputConverter<>(AuditReportResult.class);
    }

    public String buildSystemPrompt() {
        PromptTemplate template = new PromptTemplate(promptResource);
        return template.render(Map.of("format", outputConverter.getFormat()));
    }

    public BeanOutputConverter<AuditReportResult> getOutputConverter() {
        return outputConverter;
    }

    public AuditReportResult parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("Model response is empty");
        }

        // Clean out possible markdown code block fences if any LLM returns ```json ... ```
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        return outputConverter.convert(cleaned);
    }
}
