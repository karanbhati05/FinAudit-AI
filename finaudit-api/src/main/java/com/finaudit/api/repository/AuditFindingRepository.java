package com.finaudit.api.repository;

import com.finaudit.api.entity.AuditFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditFindingRepository extends JpaRepository<AuditFinding, Long> {
    List<AuditFinding> findByReportId(Long reportId);
}
