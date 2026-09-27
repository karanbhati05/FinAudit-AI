package com.finaudit.api.service;

import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.api.storage.StorageService;
import com.finaudit.core.model.ReportStatus;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("local")
class ReportParsingIntegrationTest {

    @Autowired
    private ReportParsingService reportParsingService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportLineItemRepository lineItemRepository;

    @Autowired
    private StorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void checkApiKey() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY is not set. Skipping live Gemini integration test.");
    }

    @Test
    void shouldExtractRealPdfWithGemini() throws Exception {
        // 1. Create a real sample PDF file
        File pdfFile = tempDir.resolve("sample-invoice.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Invoice ID: INV-2024-8899");
                cs.newLineAtOffset(0, -20);
                cs.showText("Vendor: Acme Cloud Systems");
                cs.newLineAtOffset(0, -20);
                cs.showText("Category: SOFTWARE");
                cs.newLineAtOffset(0, -20);
                cs.showText("Amount: 4500.00 USD");
                cs.endText();
            }
            doc.save(pdfFile);
        }

        // 2. Persist initial Report
        Report report = new Report(1L, "sample-invoice.pdf", "pending");
        report = reportRepository.save(report);

        // 3. Store file
        try (FileInputStream in = new FileInputStream(pdfFile)) {
            Path stored = storageService.store(report.getId(), "sample-invoice.pdf", in);
            report.setStoragePath(stored.toString());
            report.setStatus(ReportStatus.UPLOADED);
            reportRepository.save(report);
        }

        // 4. Trigger parsing
        reportParsingService.parseReportAsync(report.getId()).join();

        // 5. Verify results
        Report updatedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(updatedReport.getStatus()).isEqualTo(ReportStatus.AUDITING);

        List<ReportLineItem> items = lineItemRepository.findByReportId(report.getId());
        assertThat(items).isNotEmpty();
        assertThat(items.get(0).getVendor()).containsIgnoringCase("Acme");
    }
}
