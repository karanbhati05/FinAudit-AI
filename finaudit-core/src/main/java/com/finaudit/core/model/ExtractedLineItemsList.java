package com.finaudit.core.model;

import java.util.List;

public record ExtractedLineItemsList(
        List<ExtractedLineItem> items
) {
    public ExtractedLineItemsList {
        if (items == null) {
            items = List.of();
        }
    }
}
