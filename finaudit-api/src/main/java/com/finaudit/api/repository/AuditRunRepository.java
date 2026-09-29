package com.finaudit.api.repository;

import com.finaudit.api.entity.AuditRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditRunRepository extends JpaRepository<AuditRun, Long> {
    Optional<AuditRun> findByReportId(Long reportId);

    @Query("SELECT COUNT(a) FROM AuditRun a")
    long countAllAudited();

    @Query("SELECT COALESCE(AVG(a.complianceScore), 0.0) FROM AuditRun a")
    Double getAverageComplianceScore();

    @Query("SELECT a.riskLevel, COUNT(a) FROM AuditRun a GROUP BY a.riskLevel")
    List<Object[]> countByRiskLevelGrouped();

    @Query("SELECT a FROM AuditRun a WHERE a.completedAt IS NOT NULL AND a.startedAt IS NOT NULL AND a.startedAt >= :cutoff")
    List<AuditRun> findCompletedRunsAfter(java.time.Instant cutoff);

    @Query("SELECT a FROM AuditRun a WHERE a.completedAt IS NOT NULL AND a.startedAt IS NOT NULL")
    List<AuditRun> findAllCompletedRuns();
}
