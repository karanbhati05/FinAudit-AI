package com.finaudit.api.repository;

import com.finaudit.api.entity.AuditRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditRunRepository extends JpaRepository<AuditRun, Long> {
    Optional<AuditRun> findByReportId(Long reportId);
}
