package com.finaudit.api.repository;

import com.finaudit.api.entity.ReportLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportLineItemRepository extends JpaRepository<ReportLineItem, Long> {
    List<ReportLineItem> findByReportId(Long reportId);
    List<ReportLineItem> findByVendorAndInvoiceId(String vendor, String invoiceId);
    List<ReportLineItem> findByVendorIgnoreCaseAndInvoiceIdIgnoreCaseAndReportIdNot(String vendor, String invoiceId, Long reportId);
}
