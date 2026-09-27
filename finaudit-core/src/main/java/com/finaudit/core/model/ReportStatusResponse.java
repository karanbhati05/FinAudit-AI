package com.finaudit.core.model;

public record ReportStatusResponse(
        Long reportId,
        ReportStatus status,
        int lineItemCount,
        String errorMessage
) {
    public ReportStatusResponse(Long reportId, ReportStatus status, int lineItemCount) {
        this(reportId, status, lineItemCount, null);
    }
}
