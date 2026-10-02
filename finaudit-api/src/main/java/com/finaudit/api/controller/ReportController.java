package com.finaudit.api.controller;

import com.finaudit.api.entity.Report;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.api.service.CostGuardrailService;
import com.finaudit.api.service.ReportParsingService;
import com.finaudit.api.storage.StorageService;
import com.finaudit.core.model.ReportStatus;
import com.finaudit.core.model.ReportStatusResponse;
import com.finaudit.core.model.ReportUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportSpecifications;
import com.finaudit.api.service.ReportQueryService;
import com.finaudit.core.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.finaudit.api.service.AuditReportPdfService;
import com.finaudit.api.service.ShareTokenService;
import com.finaudit.api.service.ReportAskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Financial Report ingestion, parsing status, and auditing")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportRepository reportRepository;
    private final ReportLineItemRepository lineItemRepository;
    private final StorageService storageService;
    private final ReportParsingService reportParsingService;
    private final AuditRunRepository auditRunRepository;
    private final AuditFindingRepository auditFindingRepository;
    private final ReportQueryService reportQueryService;
    private final CostGuardrailService costGuardrailService;
    private final AuditReportPdfService auditReportPdfService;
    private final ShareTokenService shareTokenService;
    private final ReportAskService reportAskService;
    private final String frontendBaseUrl;

    public ReportController(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            StorageService storageService,
            ReportParsingService reportParsingService,
            AuditRunRepository auditRunRepository,
            AuditFindingRepository auditFindingRepository,
            ReportQueryService reportQueryService,
            CostGuardrailService costGuardrailService,
            AuditReportPdfService auditReportPdfService,
            ShareTokenService shareTokenService,
            ReportAskService reportAskService,
            @Value("${finaudit.share.frontend-url:https://fin-audit-ai-xi.vercel.app}") String frontendBaseUrl
    ) {
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.storageService = storageService;
        this.reportParsingService = reportParsingService;
        this.auditRunRepository = auditRunRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.reportQueryService = reportQueryService;
        this.costGuardrailService = costGuardrailService;
        this.auditReportPdfService = auditReportPdfService;
        this.shareTokenService = shareTokenService;
        this.reportAskService = reportAskService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a financial report", description = "Uploads a PDF or text document for AI parsing and auditing")
    @ApiResponse(responseCode = "202", description = "Report uploaded and parsing initiated")
    public ResponseEntity<ReportUploadResponse> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "ownerId", required = false) Long ownerId,
            jakarta.servlet.http.HttpServletRequest request,
            java.security.Principal principal
    ) throws IOException {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            // 1. Guardrail validation: file size, extension, magic bytes (PDF %PDF- / clean text)
            if (costGuardrailService != null) {
                costGuardrailService.validateUploadFile(file);
            }

            // 2. Guardrail rate limiting: max 5/user/day, max 10/IP/day, max 100 Gemini calls circuit breaker
            String clientIp = extractClientIp(request);
            String userIdentifier = principal != null ? principal.getName() : (ownerId != null ? "user-" + ownerId : clientIp);
            if (costGuardrailService != null) {
                costGuardrailService.enforceUploadRateLimits(userIdentifier, clientIp);
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                originalFilename = "uploaded_report.pdf";
            }

            // 3. Initial entity save to generate report ID
            Report report = new Report(ownerId, originalFilename, "pending");
            report = reportRepository.save(report);
            MDC.put("reportId", String.valueOf(report.getId()));

            log.info("Report upload received: id={}, filename={}, size={} bytes, user={}, ip={}",
                    report.getId(), originalFilename, file.getSize(), userIdentifier, clientIp);

            // 4. Save physical file to storage/reports/{reportId}/
            Path storedPath = storageService.store(report.getId(), originalFilename, file.getInputStream());
            report.setStoragePath(storedPath.toString());
            report.setStatus(ReportStatus.UPLOADED);
            reportRepository.save(report);

            // 5. Track successful upload count towards daily limits
            if (costGuardrailService != null) {
                costGuardrailService.recordUpload(userIdentifier, clientIp);
            }

            // 6. Trigger asynchronous parsing on virtual threads
            reportParsingService.parseReportAsync(report.getId(), correlationId);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new ReportUploadResponse(report.getId(), "Report uploaded successfully. Parsing in progress."));
        } finally {
            MDC.remove("reportId");
            MDC.remove("correlationId");
        }
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed report audit", description = "Re-initiates the parsing and audit pipeline for a failed report")
    public ResponseEntity<ReportStatusResponse> retryReportAudit(@PathVariable("id") Long id) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("reportId", String.valueOf(id));
        MDC.put("correlationId", correlationId);

        try {
            Report report = reportRepository.findById(id).orElse(null);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }
            log.info("Retrying report audit for report ID: {}", id);
            report.setStatus(ReportStatus.UPLOADED);
            report.setErrorReason(null);
            reportRepository.save(report);
            reportParsingService.parseReportAsync(id, correlationId);
            return ResponseEntity.ok(new ReportStatusResponse(id, ReportStatus.UPLOADED, 0, null));
        } finally {
            MDC.remove("reportId");
            MDC.remove("correlationId");
        }
    }

    private String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get report ingestion status", description = "Returns the status and number of extracted line items for a report")
    public ResponseEntity<ReportStatusResponse> getReportStatus(@PathVariable("id") Long id) {
        Report report = reportRepository.findById(id)
                .orElse(null);

        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        int count = lineItemRepository.findByReportId(id).size();
        return ResponseEntity.ok(new ReportStatusResponse(
                report.getId(),
                report.getStatus(),
                count,
                report.getErrorReason()
        ));
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Get audit run and findings", description = "Returns the compliance score, risk level, and all policy findings for the report")
    public ResponseEntity<AuditReportResponse> getReportAudit(@PathVariable("id") Long id) {
        Report report = reportRepository.findById(id).orElse(null);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        AuditRun run = auditRunRepository.findByReportId(id).orElse(null);
        if (run == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<AuditFinding> findings = auditFindingRepository.findByReportId(id);
        List<AuditFindingResponse> findingResponses = findings.stream()
                .map(f -> new AuditFindingResponse(
                        f.getId(),
                        f.getLineItemId(),
                        f.getRuleSource(),
                        f.getSeverity(),
                        f.getDescription(),
                        f.getPolicyReference(),
                        f.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(new AuditReportResponse(
                id,
                run.getComplianceScore(),
                run.getRiskLevel(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getRawModelOutputJson(),
                findingResponses
        ));
    }

    @GetMapping
    @Operation(summary = "List paginated reports", description = "Query reports with pagination, sorting, and filters (status, riskLevel, date range)")
    public ResponseEntity<Page<ReportSummaryDto>> listReports(
            @RequestParam(value = "status", required = false) ReportStatus status,
            @RequestParam(value = "riskLevel", required = false) RiskLevel riskLevel,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(value = "ownerId", required = false) Long ownerId,
            @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        Specification<Report> spec = Specification.where(ReportSpecifications.withStatus(status))
                .and(ReportSpecifications.withRiskLevel(riskLevel))
                .and(ReportSpecifications.withUploadedBetween(startDate, endDate))
                .and(ReportSpecifications.withOwnerId(ownerId));

        return ResponseEntity.ok(reportQueryService.getReports(spec, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get complete report details", description = "Returns full detail of report, line items, audit run, and findings with resolved policy text")
    public ResponseEntity<ReportDetailResponse> getReportDetail(@PathVariable("id") Long id) {
        return reportQueryService.getReportDetail(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/export/pdf")
    @Operation(summary = "Export completed audit report as PDF", description = "Generates a clean executive one-page PDF summary of the audit score, risk level, and findings")
    public ResponseEntity<byte[]> exportReportPdf(@PathVariable("id") Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found for ID: " + id));

        AuditRun auditRun = auditRunRepository.findByReportId(id).orElse(null);
        List<AuditFinding> findings = auditFindingRepository.findByReportId(id);

        byte[] pdfBytes = auditReportPdfService.generateAuditReportPdf(report, auditRun, findings);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finaudit-report-" + id + ".pdf\"")
                .body(pdfBytes);
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Create an unauthenticated shareable link", description = "Generates a signed expiring token for read-only guest access to an audit report")
    public ResponseEntity<ReportShareResponse> createPublicShareLink(@PathVariable("id") Long id) {
        Report report = reportRepository.findById(id).orElse(null);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        String token = shareTokenService.generateShareToken(id);
        String shareUrl = frontendBaseUrl + "/share/" + token;
        long expiresInSeconds = 7 * 24 * 3600L; // 7 days

        return ResponseEntity.ok(new ReportShareResponse(id, token, shareUrl, expiresInSeconds));
    }

    @GetMapping("/public/share/{token}")
    @Operation(summary = "Public unauthenticated report view", description = "Returns read-only report detail using a signed expiring share token without requiring login")
    public ResponseEntity<ReportDetailResponse> getPublicSharedReport(@PathVariable("token") String token) {
        Long reportId = shareTokenService.validateAndExtractReportId(token);
        return reportQueryService.getReportDetail(reportId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/public/share/{token}/export/pdf")
    @Operation(summary = "Public unauthenticated PDF download", description = "Exports report PDF using a signed expiring share token without requiring login")
    public ResponseEntity<byte[]> exportPublicSharedReportPdf(@PathVariable("token") String token) {
        Long reportId = shareTokenService.validateAndExtractReportId(token);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found for ID: " + reportId));

        AuditRun auditRun = auditRunRepository.findByReportId(reportId).orElse(null);
        List<AuditFinding> findings = auditFindingRepository.findByReportId(reportId);

        byte[] pdfBytes = auditReportPdfService.generateAuditReportPdf(report, auditRun, findings);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finaudit-report-" + reportId + ".pdf\"")
                .body(pdfBytes);
    }

    @PostMapping("/{id}/ask")
    @Operation(summary = "Scoped report Q&A", description = "Asks a narrowly scoped question grounded exclusively in the report line items, findings, and cited policies")
    public ResponseEntity<AskResponse> askReportQuestion(
            @PathVariable("id") Long id,
            @RequestBody AskRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String sessionId = httpRequest.getHeader("X-Session-Id");
        if (sessionId == null || sessionId.isBlank()) {
            jakarta.servlet.http.HttpSession session = httpRequest.getSession(false);
            sessionId = session != null ? session.getId() : httpRequest.getRemoteAddr();
        }

        AskResponse response = reportAskService.askQuestion(id, request.question(), sessionId);
        return ResponseEntity.ok(response);
    }
}
