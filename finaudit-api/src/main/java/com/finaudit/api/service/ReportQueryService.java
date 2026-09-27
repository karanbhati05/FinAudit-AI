package com.finaudit.api.service;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.entity.CompliancePolicy;
import com.finaudit.api.entity.Report;
import com.finaudit.api.entity.ReportLineItem;
import com.finaudit.api.repository.*;
import com.finaudit.core.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReportQueryService {

    private final ReportRepository reportRepository;
    private final ReportLineItemRepository lineItemRepository;
    private final AuditRunRepository auditRunRepository;
    private final AuditFindingRepository auditFindingRepository;
    private final CompliancePolicyRepository compliancePolicyRepository;

    public ReportQueryService(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            AuditRunRepository auditRunRepository,
            AuditFindingRepository auditFindingRepository,
            CompliancePolicyRepository compliancePolicyRepository
    ) {
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.auditRunRepository = auditRunRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.compliancePolicyRepository = compliancePolicyRepository;
    }

    @Transactional(readOnly = true)
    public Page<ReportSummaryDto> getReports(Specification<Report> spec, Pageable pageable) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.finaudit.api.security.UserPrincipal principal) {
            if (principal.getRole() == UserRole.VIEWER) {
                spec = spec == null
                        ? ReportSpecifications.withOwnerId(principal.getId())
                        : spec.and(ReportSpecifications.withOwnerId(principal.getId()));
            }
        }

        Page<Report> reportsPage = reportRepository.findAll(spec, pageable);

        return reportsPage.map(report -> {
            Optional<AuditRun> runOpt = auditRunRepository.findByReportId(report.getId());
            Integer score = runOpt.map(AuditRun::getComplianceScore).orElse(null);
            RiskLevel risk = runOpt.map(AuditRun::getRiskLevel).orElse(null);

            return new ReportSummaryDto(
                    report.getId(),
                    report.getOwnerId(),
                    report.getOriginalFilename(),
                    report.getStatus(),
                    report.getUploadedAt(),
                    report.getAuditedAt(),
                    score,
                    risk
            );
        });
    }

    @Transactional(readOnly = true)
    public Optional<ReportDetailResponse> getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            return Optional.empty();
        }

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.finaudit.api.security.UserPrincipal principal) {
            if (principal.getRole() == UserRole.VIEWER) {
                if (report.getOwnerId() != null && !report.getOwnerId().equals(principal.getId())) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Access denied: Viewers can only view reports where they are the owner or participant."
                    );
                }
            }
        }

        List<ReportLineItem> lineItems = lineItemRepository.findByReportId(reportId);
        List<ReportLineItemDto> lineItemDtos = lineItems.stream()
                .map(item -> new ReportLineItemDto(
                        item.getId(),
                        item.getInvoiceId(),
                        item.getVendor(),
                        item.getAmount(),
                        item.getCurrency(),
                        item.getCategory(),
                        item.getRawText(),
                        item.getLineNumber()
                ))
                .toList();

        Optional<AuditRun> auditRunOpt = auditRunRepository.findByReportId(reportId);
        Integer complianceScore = auditRunOpt.map(AuditRun::getComplianceScore).orElse(null);
        RiskLevel riskLevel = auditRunOpt.map(AuditRun::getRiskLevel).orElse(null);
        String auditSummary = auditRunOpt.map(AuditRun::getRawModelOutputJson).orElse(null);

        List<AuditFinding> findings = auditFindingRepository.findByReportId(reportId);

        // Preload compliance policies to resolve title and bodyText for policy references
        List<CompliancePolicy> allPolicies = compliancePolicyRepository.findAll();
        Map<String, CompliancePolicy> policyMap = new HashMap<>();
        for (CompliancePolicy p : allPolicies) {
            policyMap.put(p.getTitle().toLowerCase().trim(), p);
        }

        List<ResolvedFindingResponse> resolvedFindings = findings.stream()
                .map(finding -> {
                    String ref = finding.getPolicyReference();
                    String resolvedTitle = null;
                    String resolvedBody = null;

                    if (ref != null && !ref.isBlank()) {
                        resolvedTitle = ref;
                        // Attempt lookup by exact or partial title match
                        CompliancePolicy matched = policyMap.get(ref.toLowerCase().trim());
                        if (matched == null) {
                            // Find closest matching title
                            matched = allPolicies.stream()
                                    .filter(p -> p.getTitle().toLowerCase().contains(ref.toLowerCase()) || ref.toLowerCase().contains(p.getTitle().toLowerCase()))
                                    .findFirst()
                                    .orElse(null);
                        }

                        if (matched != null) {
                            resolvedTitle = matched.getTitle();
                            resolvedBody = matched.getBodyText();
                        }
                    }

                    return new ResolvedFindingResponse(
                            finding.getId(),
                            finding.getLineItemId(),
                            finding.getRuleSource(),
                            finding.getSeverity(),
                            finding.getDescription(),
                            finding.getPolicyReference(),
                            resolvedTitle,
                            resolvedBody,
                            finding.getCreatedAt()
                    );
                })
                .toList();

        return Optional.of(new ReportDetailResponse(
                report.getId(),
                report.getOwnerId(),
                report.getOriginalFilename(),
                report.getStoragePath(),
                report.getStatus(),
                report.getUploadedAt(),
                report.getAuditedAt(),
                report.getErrorReason(),
                complianceScore,
                riskLevel,
                auditSummary,
                lineItemDtos,
                resolvedFindings
        ));
    }
}
