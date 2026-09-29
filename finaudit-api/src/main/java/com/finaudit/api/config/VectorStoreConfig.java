package com.finaudit.api.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

    public static final int EMBEDDING_DIMENSIONS = 768; // Gemini gemini-embedding-001 output dimension (configured to 768)
    public static final String VECTOR_TABLE_NAME = "compliance_policy_embeddings";

    @Bean
    @org.springframework.context.annotation.Primary
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(EMBEDDING_DIMENSIONS)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .vectorTableName(VECTOR_TABLE_NAME)
                .schemaName("public")
                .initializeSchema(true)
                .build();
    }
}
