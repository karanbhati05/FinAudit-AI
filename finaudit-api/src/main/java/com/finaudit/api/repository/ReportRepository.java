package com.finaudit.api.repository;

import com.finaudit.api.entity.Report;
import com.finaudit.core.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {
    List<Report> findByOwnerId(Long ownerId);
    List<Report> findByStatus(ReportStatus status);
    List<Report> findByStatusInAndUploadedAtBefore(Collection<ReportStatus> statuses, Instant cutoff);

    long countByUploadedAtAfter(Instant cutoff);
    long countByStatusAndUploadedAtAfter(ReportStatus status, Instant cutoff);

    List<Report> findByUploadedAtBeforeAndOwnerIdNot(Instant cutoff, Long ownerId);
    List<Report> findByUploadedAtBefore(Instant cutoff);

    @Query("""
        SELECT r.id,
               COALESCE(r.auditedAt, r.uploadedAt),
               ar.riskLevel,
               ar.complianceScore,
               SUM(li.amount)
        FROM Report r
        JOIN AuditRun ar ON ar.reportId = r.id
        LEFT JOIN ReportLineItem li ON li.reportId = r.id
        WHERE r.status = 'COMPLETE'
        GROUP BY r.id, r.auditedAt, r.uploadedAt, ar.riskLevel, ar.complianceScore
        ORDER BY COALESCE(r.auditedAt, r.uploadedAt) ASC
    """)
    List<Object[]> findReportSpendAndRiskMetricsAll();

    @Query("""
        SELECT r.id,
               COALESCE(r.auditedAt, r.uploadedAt),
               ar.riskLevel,
               ar.complianceScore,
               SUM(li.amount)
        FROM Report r
        JOIN AuditRun ar ON ar.reportId = r.id
        LEFT JOIN ReportLineItem li ON li.reportId = r.id
        WHERE r.status = 'COMPLETE'
          AND COALESCE(r.auditedAt, r.uploadedAt) >= :cutoff
        GROUP BY r.id, r.auditedAt, r.uploadedAt, ar.riskLevel, ar.complianceScore
        ORDER BY COALESCE(r.auditedAt, r.uploadedAt) ASC
    """)
    List<Object[]> findReportSpendAndRiskMetricsAfter(@Param("cutoff") Instant cutoff);
}
