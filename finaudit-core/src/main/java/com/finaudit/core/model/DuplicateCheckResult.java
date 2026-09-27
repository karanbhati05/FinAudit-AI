package com.finaudit.core.model;

import java.time.LocalDate;

public record DuplicateCheckResult(
        boolean isDuplicate,
        Long originalReportId,
        LocalDate originalReportDate
) {}
