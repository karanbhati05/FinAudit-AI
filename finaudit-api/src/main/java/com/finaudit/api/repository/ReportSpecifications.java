package com.finaudit.api.repository;

import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.Report;
import com.finaudit.core.model.ReportStatus;
import com.finaudit.core.model.RiskLevel;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class ReportSpecifications {

    private ReportSpecifications() {}

    public static Specification<Report> withStatus(ReportStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Report> withOwnerId(Long ownerId) {
        return (root, query, cb) -> ownerId == null ? null : cb.equal(root.get("ownerId"), ownerId);
    }

    public static Specification<Report> withUploadedBetween(Instant startDate, Instant endDate) {
        return (root, query, cb) -> {
            if (startDate != null && endDate != null) {
                return cb.between(root.get("uploadedAt"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("uploadedAt"), startDate);
            } else if (endDate != null) {
                return cb.lessThanOrEqualTo(root.get("uploadedAt"), endDate);
            }
            return null;
        };
    }

    public static Specification<Report> withRiskLevel(RiskLevel riskLevel) {
        return (root, query, cb) -> {
            if (riskLevel == null) {
                return null;
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<AuditRun> auditRoot = subquery.from(AuditRun.class);
            subquery.select(auditRoot.get("reportId"))
                    .where(
                            cb.equal(auditRoot.get("reportId"), root.get("id")),
                            cb.equal(auditRoot.get("riskLevel"), riskLevel)
                    );
            return cb.exists(subquery);
        };
    }
}
