package com.finaudit.api.service;

import com.finaudit.api.repository.CompliancePolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyIngestionServiceTest {

    @Mock
    private CompliancePolicyRepository policyRepository;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private Environment environment;

    @InjectMocks
    private PolicyIngestionService policyIngestionService;

    @Test
    @DisplayName("ingestSeedPolicies() should parse markdown, persist policies, and ingest into vectorStore")
    void shouldIngestSeedPolicies() {
        String markdown = """
                # Corporate Spending Policy
                
                ## Clause 1: Per-Diem Meal Allowance
                Category: Meals
                Effective: 2024-01-01
                
                Daily meal expenses are capped at $75 per day.
                
                ## Clause 2: Flight Booking Standards
                Category: Travel
                Effective: 2024-01-01
                
                Economy flights must be booked at least 14 days in advance.
                """;

        Resource resource = new ByteArrayResource(markdown.getBytes(StandardCharsets.UTF_8));
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(policyRepository.findByTitle(anyString())).thenReturn(java.util.Optional.empty());
        when(policyRepository.save(any(com.finaudit.api.entity.CompliancePolicy.class))).thenAnswer(inv -> {
            com.finaudit.api.entity.CompliancePolicy p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        int count = policyIngestionService.ingestSeedPolicies();

        assertThat(count).isGreaterThanOrEqualTo(2);
        verify(policyRepository, atLeast(2)).save(any(com.finaudit.api.entity.CompliancePolicy.class));
        verify(vectorStore).add(anyList());
    }
}
