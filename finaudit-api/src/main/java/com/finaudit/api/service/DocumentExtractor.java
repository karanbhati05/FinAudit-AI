package com.finaudit.api.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DocumentExtractor {

    public String extractText(Path filePath) throws IOException {
        String filename = filePath.getFileName().toString().toLowerCase();

        if (filename.endsWith(".pdf")) {
            return extractFromPdf(filePath.toFile());
        } else if (filename.endsWith(".txt") || filename.endsWith(".csv") || filename.endsWith(".tsv")) {
            return Files.readString(filePath);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + filename + ". Only PDF and text files are supported.");
        }
    }

    private String extractFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
