package com.finaudit.core.model;

import java.math.BigDecimal;

public record ReportLineItemDto(
        Long id,
        String invoiceId,
        String vendor,
        BigDecimal amount,
        String currency,
        String category,
        String rawText,
        Integer lineNumber
) {}
