package com.finaudit.api.tool;

import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.core.model.DuplicateCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class DuplicateInvoiceDetectionTool {

    private static final Logger log = LoggerFactory.getLogger(DuplicateInvoiceDetectionTool.class);

    private final ReportLineItemRepository lineItemRepository;
    private final ReportRepository reportRepository;
    private final ThreadLocal<Long> currentReportIdHolder = new ThreadLocal<>();

    public DuplicateInvoiceDetectionTool(ReportLineItemRepository lineItemRepository, ReportRepository reportRepository) {
        this.lineItemRepository = lineItemRepository;
        this.reportRepository = reportRepository;
    }

    public void setCurrentReportId(Long reportId) {
        currentReportIdHolder.set(reportId);
    }

    public void clearCurrentReportId() {
        currentReportIdHolder.remove();
    }

    @Tool(description = "Check the database for duplicate or prior invoice submissions by vendor and invoiceId, excluding the current report being audited.")
    public DuplicateCheckResult checkDatabaseForDuplicateInvoice(
            @ToolParam(description = "The vendor or supplier name") String vendor,
            @ToolParam(description = "The invoice ID or transaction reference") String invoiceId
    ) {
        Long currentReportId = currentReportIdHolder.get();
        log.info("Executing tool checkDatabaseForDuplicateInvoice: vendor='{}', invoiceId='{}', excluding reportId={}",
                vendor, invoiceId, currentReportId);

        if (vendor == null || invoiceId == null) {
            return new DuplicateCheckResult(false, null, null);
        }

        List<ReportLineItem> matches;
        if (currentReportId != null) {
            matches = lineItemRepository.findByVendorIgnoreCaseAndInvoiceIdIgnoreCaseAndReportIdNot(
                    vendor.trim(), invoiceId.trim(), currentReportId
            );
        } else {
            matches = lineItemRepository.findByVendorAndInvoiceId(vendor.trim(), invoiceId.trim());
        }

        if (matches.isEmpty()) {
            log.info("No duplicates found for vendor '{}' with invoice ID '{}'.", vendor, invoiceId);
            return new DuplicateCheckResult(false, null, null);
        }

        ReportLineItem priorItem = matches.get(0);
        Long originalReportId = priorItem.getReportId();
        LocalDate originalReportDate = reportRepository.findById(originalReportId)
                .map(r -> r.getUploadedAt().atZone(ZoneId.systemDefault()).toLocalDate())
                .orElse(LocalDate.now());

        log.warn("Duplicate detected! Invoice '{}' from vendor '{}' already claimed in report ID: {} on date: {}",
                invoiceId, vendor, originalReportId, originalReportDate);

        return new DuplicateCheckResult(true, originalReportId, originalReportDate);
    }
}
