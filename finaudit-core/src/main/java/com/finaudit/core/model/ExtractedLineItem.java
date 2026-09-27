package com.finaudit.core.model;

import java.math.BigDecimal;

public record ExtractedLineItem(
        String invoiceId,
        String vendor,
        BigDecimal amount,
        String currency,
        String category,
        String rawLineText
) {}
