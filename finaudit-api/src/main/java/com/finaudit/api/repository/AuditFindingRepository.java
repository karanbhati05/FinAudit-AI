package com.finaudit.api.repository;

import com.finaudit.api.entity.AuditFinding;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditFindingRepository extends JpaRepository<AuditFinding, Long> {
    List<AuditFinding> findByReportId(Long reportId);

    @Query("SELECT f.policyReference, COUNT(f) FROM AuditFinding f WHERE f.policyReference IS NOT NULL GROUP BY f.policyReference ORDER BY COUNT(f) DESC")
    List<Object[]> findTopPolicyViolations(Pageable pageable);

    @Query("""
        SELECT f.policyReference, COUNT(f)
        FROM AuditFinding f
        JOIN Report r ON r.id = f.reportId
        WHERE f.policyReference IS NOT NULL
          AND r.status = 'COMPLETE'
        GROUP BY f.policyReference
        ORDER BY COUNT(f) DESC
    """)
    List<Object[]> findTopPolicyViolationsAll(Pageable pageable);

    @Query("""
        SELECT f.policyReference, COUNT(f)
        FROM AuditFinding f
        JOIN Report r ON r.id = f.reportId
        WHERE f.policyReference IS NOT NULL
          AND r.status = 'COMPLETE'
          AND COALESCE(r.auditedAt, r.uploadedAt) >= :cutoff
        GROUP BY f.policyReference
        ORDER BY COUNT(f) DESC
    """)
    List<Object[]> findTopPolicyViolationsAfter(@Param("cutoff") Instant cutoff, Pageable pageable);
}
