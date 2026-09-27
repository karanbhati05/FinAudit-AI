package com.finaudit.api.entity;

import com.finaudit.core.model.FindingSeverity;
import com.finaudit.core.model.RuleSource;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "audit_findings")
public class AuditFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "line_item_id")
    private Long lineItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_source", nullable = false)
    private RuleSource ruleSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FindingSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "policy_reference")
    private String policyReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AuditFinding() {}

    public AuditFinding(Long reportId, Long lineItemId, RuleSource ruleSource, FindingSeverity severity, String description, String policyReference) {
        this.reportId = reportId;
        this.lineItemId = lineItemId;
        this.ruleSource = ruleSource;
        this.severity = severity;
        this.description = description;
        this.policyReference = policyReference;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getLineItemId() {
        return lineItemId;
    }

    public void setLineItemId(Long lineItemId) {
        this.lineItemId = lineItemId;
    }

    public RuleSource getRuleSource() {
        return ruleSource;
    }

    public void setRuleSource(RuleSource ruleSource) {
        this.ruleSource = ruleSource;
    }

    public FindingSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(FindingSeverity severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPolicyReference() {
        return policyReference;
    }

    public void setPolicyReference(String policyReference) {
        this.policyReference = policyReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditFinding that = (AuditFinding) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
