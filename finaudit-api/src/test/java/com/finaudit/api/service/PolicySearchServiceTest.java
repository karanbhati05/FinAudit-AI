package com.finaudit.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicySearchServiceTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private PolicySearchService policySearchService;

    @Test
    @DisplayName("Should query vector store and return matched policy documents")
    void shouldSearchPolicies() {
        Document doc = new Document("Section 2: International Travel", Map.of("category", "TRAVEL"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        List<Document> results = policySearchService.searchPolicies("flight over budget", 3, 0.7);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getText()).contains("International Travel");
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }
}
