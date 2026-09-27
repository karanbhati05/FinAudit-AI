package com.finaudit.api.repository;

import com.finaudit.api.entity.ReportLineItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportLineItemRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ReportLineItemRepository lineItemRepository;

    @Test
    @DisplayName("Should perform CRUD and verify findByVendorAndInvoiceId duplicate-lookup query")
    void shouldPerformCrudAndDuplicateLookup() {
        ReportLineItem item1 = new ReportLineItem(
                1L,
                "INV-2024-001",
                "Acme Supplies",
                new BigDecimal("4500.00"),
                "USD",
                "OFFICE_SUPPLIES",
                "4500 USD Acme Supplies Desk Furniture",
                1
        );
        lineItemRepository.save(item1);

        ReportLineItem item2 = new ReportLineItem(
                2L,
                "INV-2024-001",
                "Acme Supplies",
                new BigDecimal("4500.00"),
                "USD",
                "OFFICE_SUPPLIES",
                "4500 USD Acme Supplies Duplicate Invoice",
                1
        );
        lineItemRepository.save(item2);

        // Verify duplicate lookup query
        List<ReportLineItem> duplicates = lineItemRepository.findByVendorAndInvoiceId("Acme Supplies", "INV-2024-001");
        assertThat(duplicates).hasSize(2);

        // Verify exclusion query (checking duplicates excluding current report 2)
        List<ReportLineItem> priorClaims = lineItemRepository.findByVendorIgnoreCaseAndInvoiceIdIgnoreCaseAndReportIdNot(
                "Acme Supplies", "INV-2024-001", 2L
        );
        assertThat(priorClaims).hasSize(1);
        assertThat(priorClaims.get(0).getReportId()).isEqualTo(1L);
    }
}
