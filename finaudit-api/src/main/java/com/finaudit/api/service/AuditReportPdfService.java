package com.finaudit.api.service;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.Report;
import com.finaudit.core.model.FindingSeverity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditReportPdfService {

    private static final Logger log = LoggerFactory.getLogger(AuditReportPdfService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm 'UTC'")
            .withZone(ZoneId.of("UTC"));

    public byte[] generateAuditReportPdf(Report report, AuditRun auditRun, List<AuditFinding> findings) {
        log.info("Generating PDF audit summary for report ID: {}", report.getId());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float margin = 40;
                float usableWidth = pageWidth - (2 * margin);

                // --- 1. Top Header Banner ---
                cs.setNonStrokingColor(15 / 255f, 23 / 255f, 42 / 255f); // Slate-900
                cs.addRect(margin, pageHeight - 90, usableWidth, 50);
                cs.fill();

                // Brand Title
                cs.setNonStrokingColor(Color.WHITE);
                cs.beginText();
                cs.setFont(fontBold, 16);
                cs.newLineAtOffset(margin + 15, pageHeight - 65);
                cs.showText("FINAUDIT AI");
                cs.endText();

                // Subtitle
                cs.setNonStrokingColor(203 / 255f, 213 / 255f, 225 / 255f); // Slate-300
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(margin + 150, pageHeight - 65);
                cs.showText("Automated Enterprise Compliance Audit Report");
                cs.endText();

                // Report Metadata Line
                float currentY = pageHeight - 110;
                cs.setNonStrokingColor(71 / 255f, 85 / 255f, 105 / 255f); // Slate-600
                cs.beginText();
                cs.setFont(fontBold, 9);
                cs.newLineAtOffset(margin, currentY);
                cs.showText("REPORT ID: ");
                cs.setFont(fontRegular, 9);
                cs.showText(String.valueOf(report.getId()));
                cs.setFont(fontBold, 9);
                cs.showText("   |   FILE: ");
                cs.setFont(fontRegular, 9);
                cs.showText(sanitizeText(report.getOriginalFilename()));
                cs.setFont(fontBold, 9);
                cs.showText("   |   AUDITED AT: ");
                cs.setFont(fontRegular, 9);
                cs.showText(report.getAuditedAt() != null ? DATE_FORMATTER.format(report.getAuditedAt()) : "Recent");
                cs.endText();

                // Horizontal separator
                currentY -= 10;
                cs.setStrokingColor(226 / 255f, 232 / 255f, 240 / 255f); // Slate-200
                cs.setLineWidth(1);
                cs.moveTo(margin, currentY);
                cs.lineTo(margin + usableWidth, currentY);
                cs.stroke();

                // --- 2. Key Metrics Cards ---
                currentY -= 65;
                float cardWidth = (usableWidth - 20) / 2;

                // Card A: Compliance Score
                int score = auditRun != null ? auditRun.getComplianceScore() : 100;
                Color scoreBg = score >= 80 ? new Color(240, 253, 244) : (score >= 60 ? new Color(254, 252, 232) : new Color(254, 242, 242));
                Color scoreText = score >= 80 ? new Color(22, 101, 52) : (score >= 60 ? new Color(133, 77, 14) : new Color(153, 27, 27));

                cs.setNonStrokingColor(scoreBg);
                cs.addRect(margin, currentY, cardWidth, 55);
                cs.fill();

                cs.setStrokingColor(226 / 255f, 232 / 255f, 240 / 255f);
                cs.addRect(margin, currentY, cardWidth, 55);
                cs.stroke();

                cs.setNonStrokingColor(71 / 255f, 85 / 255f, 105 / 255f);
                cs.beginText();
                cs.setFont(fontRegular, 9);
                cs.newLineAtOffset(margin + 12, currentY + 38);
                cs.showText("COMPLIANCE SCORE");
                cs.endText();

                cs.setNonStrokingColor(scoreText);
                cs.beginText();
                cs.setFont(fontBold, 22);
                cs.newLineAtOffset(margin + 12, currentY + 12);
                cs.showText(score + " / 100");
                cs.endText();

                // Card B: Risk Level
                String risk = auditRun != null && auditRun.getRiskLevel() != null ? auditRun.getRiskLevel().name() : "LOW";
                Color riskBg = "LOW".equals(risk) ? new Color(240, 253, 244) : ("MEDIUM".equals(risk) ? new Color(254, 252, 232) : new Color(254, 242, 242));
                Color riskText = "LOW".equals(risk) ? new Color(22, 101, 52) : ("MEDIUM".equals(risk) ? new Color(133, 77, 14) : new Color(153, 27, 27));

                float cardBX = margin + cardWidth + 20;
                cs.setNonStrokingColor(riskBg);
                cs.addRect(cardBX, currentY, cardWidth, 55);
                cs.fill();

                cs.setStrokingColor(226 / 255f, 232 / 255f, 240 / 255f);
                cs.addRect(cardBX, currentY, cardWidth, 55);
                cs.stroke();

                cs.setNonStrokingColor(71 / 255f, 85 / 255f, 105 / 255f);
                cs.beginText();
                cs.setFont(fontRegular, 9);
                cs.newLineAtOffset(cardBX + 12, currentY + 38);
                cs.showText("DETERMINED RISK LEVEL");
                cs.endText();

                cs.setNonStrokingColor(riskText);
                cs.beginText();
                cs.setFont(fontBold, 20);
                cs.newLineAtOffset(cardBX + 12, currentY + 12);
                cs.showText(risk + " RISK");
                cs.endText();

                // --- 3. Findings Section ---
                currentY -= 25;
                cs.setNonStrokingColor(15 / 255f, 23 / 255f, 42 / 255f);
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.newLineAtOffset(margin, currentY);
                cs.showText("Audit Findings & Policy Citations (" + (findings != null ? findings.size() : 0) + ")");
                cs.endText();

                currentY -= 15;

                if (findings == null || findings.isEmpty()) {
                    cs.setNonStrokingColor(22 / 255f, 101 / 255f, 52 / 255f);
                    cs.beginText();
                    cs.setFont(fontOblique, 10);
                    cs.newLineAtOffset(margin + 5, currentY);
                    cs.showText("Clean audit: No compliance violations or policy discrepancies detected.");
                    cs.endText();
                    currentY -= 20;
                } else {
                    int maxFindingsToShow = Math.min(findings.size(), 5);
                    for (int i = 0; i < maxFindingsToShow; i++) {
                        AuditFinding f = findings.get(i);
                        FindingSeverity sev = f.getSeverity() != null ? f.getSeverity() : FindingSeverity.LOW;
                        Color badgeColor = sev == FindingSeverity.CRITICAL || sev == FindingSeverity.HIGH
                                ? new Color(225, 29, 72)
                                : (sev == FindingSeverity.MEDIUM ? new Color(217, 119, 6) : new Color(37, 99, 235));

                        // Severity Tag + Rule Source
                        cs.setNonStrokingColor(badgeColor);
                        cs.beginText();
                        cs.setFont(fontBold, 9);
                        cs.newLineAtOffset(margin, currentY);
                        cs.showText("[" + sev.name() + "]");
                        cs.endText();

                        cs.setNonStrokingColor(100 / 255f, 116 / 255f, 139 / 255f);
                        cs.beginText();
                        cs.setFont(fontBold, 9);
                        cs.newLineAtOffset(margin + 55, currentY);
                        cs.showText("Source: " + (f.getRuleSource() != null ? f.getRuleSource().name() : "SEMANTIC"));
                        if (f.getPolicyReference() != null && !f.getPolicyReference().isBlank()) {
                            cs.setFont(fontRegular, 8);
                            cs.showText("  |  Policy: " + truncate(sanitizeText(f.getPolicyReference()), 65));
                        }
                        cs.endText();

                        currentY -= 12;

                        // Finding Description (wrapped)
                        List<String> wrappedDesc = wrapText(sanitizeText(f.getDescription()), fontRegular, 8, usableWidth - 10);
                        cs.setNonStrokingColor(30 / 255f, 41 / 255f, 59 / 255f);
                        for (String line : wrappedDesc) {
                            if (currentY < 70) break; // page boundary guard
                            cs.beginText();
                            cs.setFont(fontRegular, 8);
                            cs.newLineAtOffset(margin + 5, currentY);
                            cs.showText(line);
                            cs.endText();
                            currentY -= 10;
                        }

                        currentY -= 6; // spacing between findings
                        if (currentY < 70) break;
                    }

                    if (findings.size() > 5) {
                        cs.setNonStrokingColor(100 / 255f, 116 / 255f, 139 / 255f);
                        cs.beginText();
                        cs.setFont(fontOblique, 8);
                        cs.newLineAtOffset(margin, currentY);
                        cs.showText("... and " + (findings.size() - 5) + " additional itemized findings recorded in full report log.");
                        cs.endText();
                    }
                }

                // --- 4. Page Footer ---
                cs.setStrokingColor(226 / 255f, 232 / 255f, 240 / 255f);
                cs.moveTo(margin, 45);
                cs.lineTo(margin + usableWidth, 45);
                cs.stroke();

                cs.setNonStrokingColor(148 / 255f, 163 / 255f, 184 / 255f);
                cs.beginText();
                cs.setFont(fontRegular, 8);
                cs.newLineAtOffset(margin, 32);
                cs.showText("FinAudit AI Autonomous Compliance Engine • Grounded RAG + Deterministic Tool Enforcement");
                cs.endText();

                cs.beginText();
                cs.setFont(fontRegular, 8);
                cs.newLineAtOffset(pageWidth - margin - 80, 32);
                cs.showText("Confidential • Page 1 of 1");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate PDF audit report for report ID: {}", report.getId(), e);
            throw new UncheckedIOException("Failed to generate PDF audit report", e);
        }
    }

    private static String sanitizeText(String text) {
        if (text == null) return "";
        // PDF standard Type1 fonts only support Latin-1 / ASCII. Strip non-ASCII or replace smart quotes
        return text.replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .replace("\u2014", "-")
                .replaceAll("[^\\x20-\\x7E]", " ")
                .trim();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private static List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            try {
                float width = font.getStringWidth(testLine) / 1000 * fontSize;
                if (width <= maxWidth) {
                    currentLine = new StringBuilder(testLine);
                } else {
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                    }
                    currentLine = new StringBuilder(word);
                }
            } catch (IOException e) {
                currentLine.append(" ").append(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
