package com.finaudit.api.entity;

import com.finaudit.core.model.RiskLevel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "audit_runs")
public class AuditRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "compliance_score", nullable = false)
    private Integer complianceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "raw_model_output_json", columnDefinition = "TEXT")
    private String rawModelOutputJson;

    public AuditRun() {}

    public AuditRun(Long reportId, Integer complianceScore, RiskLevel riskLevel, String rawModelOutputJson) {
        this.reportId = reportId;
        this.complianceScore = complianceScore;
        this.riskLevel = riskLevel;
        this.rawModelOutputJson = rawModelOutputJson;
        this.startedAt = Instant.now();
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

    public Integer getComplianceScore() {
        return complianceScore;
    }

    public void setComplianceScore(Integer complianceScore) {
        this.complianceScore = complianceScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getRawModelOutputJson() {
        return rawModelOutputJson;
    }

    public void setRawModelOutputJson(String rawModelOutputJson) {
        this.rawModelOutputJson = rawModelOutputJson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditRun auditRun = (AuditRun) o;
        return Objects.equals(id, auditRun.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
