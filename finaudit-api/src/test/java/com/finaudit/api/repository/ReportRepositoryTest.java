package com.finaudit.api.repository;

import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.User;
import com.finaudit.core.model.ReportStatus;
import com.finaudit.core.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save and query reports by ownerId and status")
    void shouldSaveAndQueryReports() {
        User user = new User("owner@example.com", "hash", UserRole.AUDITOR);
        user = userRepository.save(user);

        Report report = new Report(user.getId(), "expense_q1.pdf", "storage/reports/1/expense_q1.pdf");
        Report saved = reportRepository.save(report);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.UPLOADED);

        List<Report> ownerReports = reportRepository.findByOwnerId(user.getId());
        assertThat(ownerReports).hasSize(1);
        assertThat(ownerReports.get(0).getOriginalFilename()).isEqualTo("expense_q1.pdf");

        List<Report> uploadedReports = reportRepository.findByStatus(ReportStatus.UPLOADED);
        assertThat(uploadedReports).isNotEmpty();
    }
}
