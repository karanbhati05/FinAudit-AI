package com.finaudit.api.repository;

import com.finaudit.api.entity.AuditFinding;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditFindingRepository extends JpaRepository<AuditFinding, Long> {
    List<AuditFinding> findByReportId(Long reportId);

    @Query("SELECT f.policyReference, COUNT(f) FROM AuditFinding f WHERE f.policyReference IS NOT NULL GROUP BY f.policyReference ORDER BY COUNT(f) DESC")
    List<Object[]> findTopPolicyViolations(Pageable pageable);
}
