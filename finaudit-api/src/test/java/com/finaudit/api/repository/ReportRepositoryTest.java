package com.finaudit.api.repository;

import com.finaudit.api.entity.Report;
import com.finaudit.core.model.ReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Test
    @DisplayName("Should save and query reports by ownerId and status")
    void shouldSaveAndQueryReports() {
        Report report = new Report(100L, "expense_q1.pdf", "storage/reports/1/expense_q1.pdf");
        Report saved = reportRepository.save(report);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.UPLOADED);

        List<Report> ownerReports = reportRepository.findByOwnerId(100L);
        assertThat(ownerReports).hasSize(1);
        assertThat(ownerReports.get(0).getOriginalFilename()).isEqualTo("expense_q1.pdf");

        List<Report> uploadedReports = reportRepository.findByStatus(ReportStatus.UPLOADED);
        assertThat(uploadedReports).isNotEmpty();
    }
}
