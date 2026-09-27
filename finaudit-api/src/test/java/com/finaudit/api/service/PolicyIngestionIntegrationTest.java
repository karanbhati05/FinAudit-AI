package com.finaudit.api.service;

import com.finaudit.api.entity.CompliancePolicy;
import com.finaudit.api.repository.CompliancePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
@ActiveProfiles({"local", "seed-policies"})
@Testcontainers(disabledWithoutDocker = true)
class PolicyIngestionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("finaudit")
            .withUsername("postgres")
            .withPassword("postgres");

    @Autowired
    private PolicyIngestionService policyIngestionService;

    @Autowired
    private PolicySearchService policySearchService;

    @Autowired
    private CompliancePolicyRepository policyRepository;

    @BeforeEach
    void verifyEnvironment() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY is not set. Skipping live embedding test.");
    }

    @Test
    @DisplayName("Should ingest seed policies into pgvector and retrieve travel policy in top 3 results")
    void shouldIngestAndSearchPolicies() {
        int count = policyIngestionService.ingestSeedPolicies();
        assertThat(count).isGreaterThanOrEqualTo(5);

        List<CompliancePolicy> policies = policyRepository.findAll();
        assertThat(policies).isNotEmpty();

        // Perform similarity search for travel policy
        List<Document> results = policySearchService.searchPolicies("international travel over budget", 3, 0.5);
        assertThat(results).isNotEmpty();

        boolean foundTravelPolicy = results.stream()
                .anyMatch(doc -> doc.getText().toLowerCase().contains("travel") || doc.getText().toLowerCase().contains("flight"));
        assertThat(foundTravelPolicy).isTrue();
    }
}
