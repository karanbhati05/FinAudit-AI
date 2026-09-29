package com.finaudit.api.config;

import com.finaudit.api.entity.*;
import com.finaudit.api.repository.*;
import com.finaudit.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Automatically seeds a public demo account (demo@finaudit.ai) populated with 4 pre-audited
 * real-world enterprise reports, realistic line items, and compliance findings so that visitors
 * can immediately explore a populated dashboard without friction or required signup.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String DEMO_EMAIL = "demo@finaudit.ai";
    public static final String DEMO_RAW_PASSWORD = "DemoAuditor2026!";

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ReportLineItemRepository lineItemRepository;
    private final AuditRunRepository auditRunRepository;
    private final AuditFindingRepository auditFindingRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            UserRepository userRepository,
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            AuditRunRepository auditRunRepository,
            AuditFindingRepository auditFindingRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.auditRunRepository = auditRunRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedDemoData();
        } catch (Exception e) {
            log.warn("Demo data seeding encountered a non-fatal exception: {}", e.getMessage());
        }
    }

    public void seedDemoData() {
        // 1. Ensure Demo User exists
        User demoUser = userRepository.findByEmail(DEMO_EMAIL).orElseGet(() -> {
            log.info("Creating default public demo account: {}", DEMO_EMAIL);
            User u = new User(DEMO_EMAIL, passwordEncoder.encode(DEMO_RAW_PASSWORD), UserRole.AUDITOR);
            return userRepository.save(u);
        });

        // 2. Check if reports already exist for this demo user
        List<Report> existingReports = reportRepository.findByOwnerId(demoUser.getId());
        if (!existingReports.isEmpty()) {
            log.info("Demo account already has {} pre-audited reports. Skipping seeding.", existingReports.size());
            return;
        }

        log.info("Seeding 4 pre-audited demo reports for demo account...");

        // Report 1: Q3_Executive_Travel_Claim.pdf (Score: 72, Risk: MEDIUM)
        createReport(
                demoUser.getId(),
                "Q3_Executive_Travel_Claim.pdf",
                "storage/demo/Q3_Executive_Travel_Claim.pdf",
                72,
                RiskLevel.MEDIUM,
                Instant.now().minus(2, ChronoUnit.HOURS),
                "Audit completed with 2 findings. Detected non-compliant airfare class booking for a regional flight and dining per-diem cap violation.",
                List.of(
                        new LineItemData("INV-2026-101", "Singapore Airlines", new BigDecimal("3450.00"), "USD", "Airfare", "Business Class flight SIN-BKK return", 1),
                        new LineItemData("INV-2026-102", "Marina Bay Sands Hotel", new BigDecimal("1800.00"), "USD", "Lodging", "3 nights executive deluxe room", 2),
                        new LineItemData("INV-2026-103", "Morton's The Steakhouse", new BigDecimal("680.00"), "USD", "Meals & Entertainment", "Client dinner with 3 executive attendees", 3),
                        new LineItemData("INV-2026-104", "Grab Transport", new BigDecimal("45.00"), "USD", "Ground Transportation", "Airport to client office transfer", 4)
                ),
                List.of(
                        new FindingData(0, RuleSource.SEMANTIC, FindingSeverity.HIGH,
                                "Business class airfare exceeds corporate travel policy allowance for regional flight durations under 6 hours without prior VP authorization.",
                                "Corporate Travel & Expense Policy - Section 4.1: Air Travel Class"),
                        new FindingData(2, RuleSource.SEMANTIC, FindingSeverity.MEDIUM,
                                "Per-attendee meal expenditure of $170/head exceeds the allowable $100 cap for client entertainment without itemized attendee list.",
                                "Corporate Dining & Entertainment Policy - Section 2.3: Per Diem Caps")
                )
        );

        // Report 2: Vendor_Payment_Batch_Sept.txt (Score: 35, Risk: HIGH)
        createReport(
                demoUser.getId(),
                "Vendor_Payment_Batch_Sept.txt",
                "storage/demo/Vendor_Payment_Batch_Sept.txt",
                35,
                RiskLevel.HIGH,
                Instant.now().minus(5, ChronoUnit.HOURS),
                "Audit finalized with CRITICAL findings. Deterministic fraud rule detected duplicate invoice submission across accounts, plus procurement dual-authorization threshold breach.",
                List.of(
                        new LineItemData("INV-2026-8801", "Northwind Cloud Services", new BigDecimal("12400.00"), "USD", "IT Infrastructure", "Managed Kubernetes Cluster Monthly", 1),
                        new LineItemData("INV-2026-8802", "Apex Logistics Corp", new BigDecimal("4120.00"), "USD", "Logistics", "Air freight expedited delivery", 2),
                        new LineItemData("INV-2026-8801", "Northwind Cloud Services", new BigDecimal("12400.00"), "USD", "IT Infrastructure", "Managed Kubernetes Cluster Monthly", 3),
                        new LineItemData("INV-2026-8803", "SecureNet Cyber Consulting", new BigDecimal("18500.00"), "USD", "Consulting", "Quarterly penetration testing & audit", 4)
                ),
                List.of(
                        new FindingData(2, RuleSource.DETERMINISTIC, FindingSeverity.CRITICAL,
                                "Duplicate invoice ID INV-2026-8801 detected with identical vendor and amount ($12,400.00). Deterministic duplicate invoice fraud trigger activated.",
                                "Anti-Fraud & Invoice Verification Policy - Section 1.1: Duplicate Detection"),
                        new FindingData(3, RuleSource.SEMANTIC, FindingSeverity.HIGH,
                                "Single consulting disbursement of $18,500.00 exceeds $15,000 threshold without mandatory secondary executive procurement sign-off.",
                                "Procurement & Approval Matrix - Section 6.2: Dual Sign-off")
                )
        );

        // Report 3: Global_Procurement_Audit_H1.pdf (Score: 96, Risk: LOW)
        createReport(
                demoUser.getId(),
                "Global_Procurement_Audit_H1.pdf",
                "storage/demo/Global_Procurement_Audit_H1.pdf",
                96,
                RiskLevel.LOW,
                Instant.now().minus(1, ChronoUnit.DAYS),
                "Exemplary compliance. All hardware and software expenditures adhere to negotiated vendor rate cards with appropriate pre-approvals.",
                List.of(
                        new LineItemData("INV-2026-4401", "Dell Enterprise Technologies", new BigDecimal("8200.00"), "USD", "Hardware", "4x Precision Mobile Workstations", 1),
                        new LineItemData("INV-2026-4402", "JetBrains s.r.o.", new BigDecimal("1490.00"), "USD", "Software", "Annual All Products Developer Pack", 2),
                        new LineItemData("INV-2026-4403", "Amazon Web Services", new BigDecimal("3210.00"), "USD", "Cloud Services", "Production S3 & Aurora Postgres hosting", 3)
                ),
                List.of(
                        new FindingData(1, RuleSource.SEMANTIC, FindingSeverity.LOW,
                                "Annual software renewal verified against active enterprise contract; compliant with IT purchasing guidelines.",
                                "Software Licensing & Asset Management - Section 3.1")
                )
        );

        // Report 4: Marketing_Field_Expenses_Aug.csv (Score: 58, Risk: HIGH)
        createReport(
                demoUser.getId(),
                "Marketing_Field_Expenses_Aug.csv",
                "storage/demo/Marketing_Field_Expenses_Aug.csv",
                58,
                RiskLevel.HIGH,
                Instant.now().minus(2, ChronoUnit.DAYS),
                "Audit identified 2 policy violations including an unapproved luxury hospitality expenditure and non-business weekend dining charges.",
                List.of(
                        new LineItemData("INV-2026-5510", "Marriott Downtown Hotel", new BigDecimal("920.00"), "USD", "Lodging", "2 nights conference speaker lodging", 1),
                        new LineItemData("INV-2026-5511", "Harbor Luxury Yacht Charters", new BigDecimal("4500.00"), "USD", "Entertainment", "VIP Client Reception on Harbor", 2),
                        new LineItemData("INV-2026-5512", "Convention Expo Center", new BigDecimal("2200.00"), "USD", "Marketing", "Event booth exhibit rental fee", 3),
                        new LineItemData("INV-2026-5513", "Downtown Nightclub Lounge", new BigDecimal("850.00"), "USD", "Entertainment", "Weekend team bonding beverage tab", 4)
                ),
                List.of(
                        new FindingData(1, RuleSource.SEMANTIC, FindingSeverity.HIGH,
                                "Luxury yacht charter expenditure ($4,500.00) exceeds standard client event limits and requires prior CFO authorization.",
                                "Corporate Entertainment Standards - Section 5.4: Special Events"),
                        new FindingData(3, RuleSource.SEMANTIC, FindingSeverity.HIGH,
                                "Expenditure incurred during non-operating weekend hours with no documented client attendance or business justification.",
                                "Business Expense Substantiation - Section 2.1: Eligible Operating Hours")
                )
        );

        log.info("Successfully seeded 4 pre-audited demo reports!");
    }

    private void createReport(
            Long ownerId,
            String filename,
            String storagePath,
            int score,
            RiskLevel risk,
            Instant uploadTime,
            String summary,
            List<LineItemData> lineItems,
            List<FindingData> findings
    ) {
        Report report = new Report(ownerId, filename, storagePath);
        report.setStatus(ReportStatus.COMPLETE);
        report.setAuditedAt(uploadTime.plus(45, ChronoUnit.SECONDS));
        report = reportRepository.save(report);

        // Line Items
        List<ReportLineItem> savedLineItems = new java.util.ArrayList<>();
        for (LineItemData item : lineItems) {
            ReportLineItem li = new ReportLineItem(
                    report.getId(),
                    item.invoiceId,
                    item.vendor,
                    item.amount,
                    item.currency,
                    item.category,
                    item.rawText,
                    item.lineNumber
            );
            savedLineItems.add(lineItemRepository.save(li));
        }

        // Audit Run
        String rawJson = "{\"complianceScore\":" + score + ",\"riskLevel\":\"" + risk.name() + "\",\"summary\":\"" + summary + "\"}";
        AuditRun auditRun = new AuditRun(report.getId(), score, risk, rawJson);
        auditRun.setStartedAt(uploadTime);
        auditRun.setCompletedAt(uploadTime.plus(45, ChronoUnit.SECONDS));
        auditRunRepository.save(auditRun);

        // Findings
        for (FindingData fd : findings) {
            Long lineItemId = (fd.lineItemIndex >= 0 && fd.lineItemIndex < savedLineItems.size())
                    ? savedLineItems.get(fd.lineItemIndex).getId()
                    : null;

            AuditFinding af = new AuditFinding(
                    report.getId(),
                    lineItemId,
                    fd.ruleSource,
                    fd.severity,
                    fd.description,
                    fd.policyReference
            );
            auditFindingRepository.save(af);
        }
    }

    private record LineItemData(String invoiceId, String vendor, BigDecimal amount, String currency, String category, String rawText, int lineNumber) {}
    private record FindingData(int lineItemIndex, RuleSource ruleSource, FindingSeverity severity, String description, String policyReference) {}
}
