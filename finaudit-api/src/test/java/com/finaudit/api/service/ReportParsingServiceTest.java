package com.finaudit.api.service;

import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.api.storage.StorageService;
import com.finaudit.core.model.ExtractedLineItem;
import com.finaudit.core.model.ExtractedLineItemsList;
import com.finaudit.core.model.ReportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportParsingServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportLineItemRepository lineItemRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private DocumentExtractor documentExtractor;

    @Mock
    private ChatClient chatClient;

    @Mock(answer = Answers.RETURNS_SELF)
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private ReportParsingService reportParsingService;

    @BeforeEach
    void setUp() {
        reportParsingService = new ReportParsingService(
                reportRepository,
                lineItemRepository,
                storageService,
                documentExtractor,
                chatClient
        );
    }

    @Test
    @DisplayName("Should parse report successfully, persist line items, and transition to AUDITING")
    void shouldParseReportSuccessfully() throws Exception {
        Report report = new Report(1L, "sample.txt", "storage/reports/1/sample.txt");
        report.setId(1L);
        report.setStatus(ReportStatus.UPLOADED);

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(documentExtractor.extractText(any(Path.class))).thenReturn("Sample invoice text line 1");

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        doReturn(requestSpec).when(requestSpec).user(any(Consumer.class));
        when(requestSpec.call()).thenReturn(callSpec);

        ExtractedLineItem item = new ExtractedLineItem(
                "INV-101",
                "Vendor Corp",
                new BigDecimal("1250.00"),
                "USD",
                "TRAVEL",
                "Flight to NYC $1250"
        );
        ExtractedLineItemsList itemsList = new ExtractedLineItemsList(List.of(item));
        when(callSpec.entity(ExtractedLineItemsList.class)).thenReturn(itemsList);

        reportParsingService.parseReportAsync(1L).join();

        // Verify status transitions: saved initially as PARSING, then as AUDITING
        verify(reportRepository, atLeast(2)).save(report);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.AUDITING);
        assertThat(report.getErrorReason()).isNull();

        // Verify line item persisted
        verify(lineItemRepository, times(1)).save(any(ReportLineItem.class));
    }

    @Test
    @DisplayName("Should transition report to FAILED when text extraction throws an exception")
    void shouldHandleExtractionFailure() throws Exception {
        Report report = new Report(2L, "corrupted.pdf", "storage/reports/2/corrupted.pdf");
        report.setId(2L);
        report.setStatus(ReportStatus.UPLOADED);

        when(reportRepository.findById(2L)).thenReturn(Optional.of(report));
        when(documentExtractor.extractText(any(Path.class))).thenThrow(new RuntimeException("Corrupted PDF file"));

        reportParsingService.parseReportAsync(2L).join();

        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(report.getErrorReason()).contains("Corrupted PDF file");
        verify(lineItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should transition report to FAILED when Gemini structured output throws malformed JSON exception")
    void shouldHandleMalformedJsonFromGemini() throws Exception {
        Report report = new Report(3L, "malformed.pdf", "storage/reports/3/malformed.pdf");
        report.setId(3L);
        report.setStatus(ReportStatus.UPLOADED);

        when(reportRepository.findById(3L)).thenReturn(Optional.of(report));
        when(documentExtractor.extractText(any(Path.class))).thenReturn("Some raw document text");

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        doReturn(requestSpec).when(requestSpec).user(any(Consumer.class));
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.entity(ExtractedLineItemsList.class)).thenThrow(new RuntimeException("Malformed JSON from LLM"));

        reportParsingService.parseReportAsync(3L).join();

        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(report.getErrorReason()).contains("Malformed JSON from LLM");
        verify(lineItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should transition report to FAILED when no line items are extracted by model")
    void shouldHandleNoLineItemsExtracted() throws Exception {
        Report report = new Report(4L, "empty.pdf", "storage/reports/4/empty.pdf");
        report.setId(4L);
        report.setStatus(ReportStatus.UPLOADED);

        when(reportRepository.findById(4L)).thenReturn(Optional.of(report));
        when(documentExtractor.extractText(any(Path.class))).thenReturn("Non-financial plain text without any receipts");

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        doReturn(requestSpec).when(requestSpec).user(any(Consumer.class));
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.entity(ExtractedLineItemsList.class)).thenReturn(new ExtractedLineItemsList(List.of()));

        reportParsingService.parseReportAsync(4L).join();

        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(report.getErrorReason()).contains("No line items could be extracted");
        verify(lineItemRepository, never()).save(any());
    }
}
