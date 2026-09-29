package com.finaudit.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class EmbeddingModelDimensionVerificationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("finaudit")
            .withUsername("postgres")
            .withPassword("postgres");

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    @DisplayName("Verify real Gemini embedding call returns exactly 768 dimensions")
    void testEmbeddingDimensions() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY must be set");

        System.out.println("=== EXECUTING LIVE GEMINI EMBEDDING CALL ===");
        float[] vector = embeddingModel.embed("All international flight bookings must be economy class unless approved by a Vice President.");

        System.out.println(">>> RAW VECTOR LENGTH: " + vector.length);
        System.out.println(">>> FIRST 5 DIMENSIONS: " + Arrays.toString(Arrays.copyOf(vector, 5)));

        assertThat(vector).hasSize(768);
        System.out.println("=== VERIFICATION PASSED: OUTPUT DIMENSIONALITY IS EXACTLY 768 ===");
    }
}
