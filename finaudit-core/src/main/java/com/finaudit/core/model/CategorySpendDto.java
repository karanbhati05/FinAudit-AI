package com.finaudit.core.model;

import java.math.BigDecimal;

public record CategorySpendDto(
        String category,
        BigDecimal amount,
        int count,
        double percentage
) {}
