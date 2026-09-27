package com.finaudit.api.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentExtractorTest {

    private final DocumentExtractor extractor = new DocumentExtractor();

    @Test
    @DisplayName("extractText should read plain text file directly")
    void shouldExtractFromTextFile(@TempDir Path tempDir) throws IOException {
        Path txtFile = tempDir.resolve("sample.txt");
        Files.writeString(txtFile, "Invoice #101\nVendor: Acme Corp\nTotal: $150.00");

        String text = extractor.extractText(txtFile);
        assertThat(text).contains("Invoice #101").contains("Acme Corp");
    }

    @Test
    @DisplayName("extractText should extract text from real PDF file using PDFBox")
    void shouldExtractFromPdfFile(@TempDir Path tempDir) throws IOException {
        Path pdfFile = tempDir.resolve("sample.pdf");

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Corporate Travel Expense Statement PDF");
                cs.endText();
            }
            doc.save(pdfFile.toFile());
        }

        String text = extractor.extractText(pdfFile);
        assertThat(text).contains("Corporate Travel Expense Statement PDF");
    }

    @Test
    @DisplayName("extractText should throw IllegalArgumentException for unsupported extensions")
    void shouldRejectUnsupportedFile(@TempDir Path tempDir) throws IOException {
        Path jpgFile = tempDir.resolve("receipt.jpg");
        Files.writeString(jpgFile, "dummy");

        assertThatThrownBy(() -> extractor.extractText(jpgFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }
}
