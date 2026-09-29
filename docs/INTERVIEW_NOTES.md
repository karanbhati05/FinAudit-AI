# FinAudit-AI — Technical Architecture & Interview Notes

> **Author**: Karan Bhati  
> **Repository**: [karanbhati05/FinAudit-AI](https://github.com/karanbhati05/FinAudit-AI)  
> **System**: Autonomous Enterprise Financial Compliance & Expense Ingestion Audit  

---

## 1. Problem Statement & Enterprise Scenario

Large enterprises process tens of thousands of corporate expense claims, travel receipts, and vendor invoices monthly, losing an estimated 5% of annual revenue to policy non-compliance, unauthorized booking upgrades, and fraudulent duplicate billings. Traditional accounting controls rely on manual, sample-based audits that inspect less than 5% of total filings, leaving systemic leakage undetected until formal quarterly or annual reporting cycles. FinAudit-AI solves this by providing automated, 100% submission-level compliance auditing, combining deterministic relational database queries for fraud detection with vector-grounded semantic retrieval against corporate policy clauses to produce verifiable, audit-grade findings with zero hallucinations.

---

## 2. End-to-End Architecture Walkthrough

The request lifecycle moves across a decoupled ingestion and inference pipeline:

```
[Client / React SPA]
       │
       ▼ (1) Upload PDF/TXT
[ReportController] ──► [CostGuardrailService] (Rate Limiting: 5/user, 10/IP; Magic Bytes Check)
       │
       ▼ (2) Persist raw document
[LocalStorageService]
       │
       ▼ (3) Async Trigger
[ReportParsingService] ──► [DocumentExtractor] (Apache PDFBox / UTF-8 Text Extraction)
       │                       │
       │                       ▼
       │               [AuditPromptService] (Spring AI ChatClient Line-Item Extraction)
       │                       │
       │                       ▼
       │               [ReportLineItemRepository] (Batch Line Items Saved)
       │
       ▼ (4) Autonomous RAG Orchestration
[AuditOrchestrationService]
       ├──► [CostGuardrailService] (Semaphore concurrency cap: max 2 in-flight audits)
       ├──► [CostGuardrailService] (Daily spend circuit breaker: 100 Gemini calls/day)
       ├──► [PolicySearchService] (Cosine similarity search on pgvector `compliance_policies`)
       ├──► [DuplicateInvoiceDetectionTool] (Registered Spring AI Function Tool)
       │         └──► [ReportLineItemRepository] (Deterministic cross-report duplicate query)
       └──► [ChatClient] (Google Gemini 2.5 Flash with System Prompt & Tool Execution)
                 │
                 ▼ (5) Structured Output Parsing
       [AuditReportResult] ──► [AuditRunRepository] (Compliance Score & Risk Level Saved)
                           └──► [AuditFindingRepository] (Findings with RuleSource & Policy Citation)
                                 │
                                 ▼ (6) Completion
                       [ReportStatus: COMPLETE]
                                 ▲
                                 │ (Watchdog auto-reclaims timeouts > 2m)
                       [ReportWatchdogService]
```

### Detailed Component Roles:
1. **Intake & Abuse Mitigation**: An HTTP multipart upload reaches `ReportController.uploadReport()`. `CostGuardrailService` inspects the byte stream for magic headers (`%PDF-` for PDFs, ASCII printable ratios for plain text) and verifies that the submitting user and origin IP have not exceeded their daily rate limits (5 per user, 10 per IP).
2. **Persistence**: `LocalStorageService` writes the original file to a structured directory path (`storage/reports/{id}/{filename}`).
3. **Document Extraction & Normalization**: `ReportParsingService.parseReportAsync()` is dispatched on Spring Boot 3.4's Java 21 Virtual Threads. `DocumentExtractor` parses the unstructured text via Apache PDFBox. Next, `AuditPromptService` constructs an extraction prompt instructing Gemini to parse items into an array of typed line items (invoice IDs, vendors, amounts, currencies, categories), which are normalized and saved into `ReportLineItemRepository`.
4. **Autonomous Audit Orchestration**: `AuditOrchestrationService.runAudit()` aggregates unique line-item categories and vendor metadata, querying `PolicySearchService` to fetch relevant policy clauses via pgvector cosine distance (`0.60` similarity threshold).
5. **Deterministic Tool Execution**: The service configures `DuplicateInvoiceDetectionTool` as an active tool callback on the Gemini ChatClient prompt. During reasoning, if Gemini identifies an invoice ID, it calls the Java tool, which queries the database for identical invoice numbers previously filed under separate reports by that vendor.
6. **Structured Output & Persistence**: Gemini returns a JSON response adhering to `AuditReportResult`. The orchestration service maps findings into `AuditFinding` entities tagged with either `DETERMINISTIC` or `SEMANTIC` rule sources, stores an `AuditRun` record with the overall compliance score (0–100) and risk level (`LOW`, `MEDIUM`, `HIGH`), and transitions the report status to `COMPLETE`.
7. **Watchdog Resilience**: If any step in the asynchronous pipeline hangs or network loss disrupts the LLM stream, `ReportWatchdogService` (running every 30 seconds via `@Scheduled`) flags any report lingering in `PARSING` or `AUDITING` for more than 2 minutes as `FAILED`, exposing a clear retry trigger to the frontend.

---

## 3. Key Technical Decisions & Trade-Offs

### 1. RAG (Retrieval-Augmented Generation) over Fine-Tuning
- **Trade-Off**: Fine-tuning bakes knowledge directly into model weights at the expense of agility and traceability.
- **Reasoning**: Corporate travel policies, dining allowances, and spending thresholds change dynamically (e.g. quarterly per-diem rate updates or tenant-specific addenda). Fine-tuning requires expensive re-training runs, causes catastrophic forgetting, and produces probabilistic citations that cannot be verified against legal text. By contrast, RAG using pgvector with a calibrated cosine similarity threshold (`0.60`) allows policy updates to take effect in milliseconds with zero model retraining. Furthermore, every finding stores the exact clause title and verbatim policy excerpt, providing audit traceability that compliance officers can defend in an external review.

### 2. Deterministic Java Tool Call over LLM "Memory" for Duplicate Invoices
- **Trade-Off**: Allowing the LLM to inspect historical filings in-context simplifies prompt design, but balloons token costs and fails at scale.
- **Reasoning**: LLMs are probabilistic token generators, not relational indexing engines. Feeding thousands of historic invoice records into an LLM context window is cost-prohibitive, introduces attention degradation, and still risks false negatives on invoice numbers with minor formatting variations. Duplicate invoice detection is an exact, zero-tolerance business rule. We implemented `DuplicateInvoiceDetectionTool` as a deterministic Java function tool invoking indexed SQL queries (`WHERE vendor = ? AND invoice_id = ? AND report_id != ?`). The LLM decides *when* to trigger the tool, while PostgreSQL guarantees 100% precision.

### 3. Structured Output (`BeanOutputConverter` / Record Mapping) over Free-Text Parsing
- **Trade-Off**: Unstructured natural language output provides creative conversational summaries, but cannot feed automated accounting workflows.
- **Reasoning**: Financial compliance systems must integrate with downstream ERP systems (SAP, NetSuite, Oracle). Relying on regex parsing of free-form LLM text is notoriously brittle across model updates. By enforcing structured JSON schemas via Spring AI's structured output converters into strongly typed Java 21 records (`AuditReportResult`, `AuditFindingDto`), we achieve compile-time schema safety, reliable database persistence, and clean frontend visualization without ad-hoc string scrubbing.

### 4. JWT with Role-Scoped Visibility over All-or-Nothing Auth
- **Trade-Off**: An all-or-nothing auth model (any authenticated user can do anything) is faster to build, but fails enterprise security posture.
- **Reasoning**: Corporate expense data contains sensitive employee personal information and executive travel routes. We implemented stateless JWT tokens with role claims (`ROLE_AUDITOR`, `ROLE_VIEWER`). Auditors have full authority to upload, retry, and manage policies, while Viewers can only view aggregate dashboard metrics and report findings. This provides defense-in-depth and tenant data isolation without requiring stateful server session lookups.

---

## 4. What Went Wrong & How It Was Found: The `gemini-embedding-001` Dimension Mismatch

During Milestone 11 verification, while running end-to-end policy seeding against a live PostgreSQL 16 instance with pgvector, the application crashed during document vectorization with a database error:

```
org.postgresql.util.PSQLException: ERROR: column "embedding" is of type vector(768) but expression is of type vector(3072)
```

### Root Cause Analysis:
In the initial Flyway migration script (`V1__init_schema.sql`), the `compliance_policies` table defined the embedding column as `embedding vector(768)`, which matched Google's previous generation embedding model (`text-embedding-004`). When upgrading to Google's flagship `gemini-embedding-001` model in Spring AI, the Gemini REST endpoint defaulted to generating **3072-dimensional** vector arrays.

### Investigation & Solution:
1. **Consideration 1 — Schema Migration**: We considered altering the column to `vector(3072)`. However, doubling vector dimensionality significantly increases pgvector memory consumption, slows down HNSW index construction, and degrades cache locality on constrained cloud database tiers (such as Render's 512MB RAM tier).
2. **Consideration 2 — Matryoshka Representation Learning**: Google's `gemini-embedding-001` supports Matryoshka dimensionality truncation, allowing clients to request lower-dimensional sub-embeddings (such as 768) without significant loss of semantic retrieval fidelity.
3. **The Fix (Commit `20efba8`)**: We updated `application.yml` to explicitly declare `dimensions: 768` for both the primary model configuration and the text options:
   ```yaml
   spring:
     ai:
       vertex:
         ai:
           gemini:
             embedding:
               model: gemini-embedding-001
               options:
                 model: gemini-embedding-001
                 dimensions: 768
   ```
4. **Automated Guardrail**: To prevent future regressions across dependency updates, we implemented `EmbeddingModelDimensionVerificationTest`, an integration test that performs a live call to the embedding endpoint and explicitly asserts:
   ```java
   float[] vector = embeddingModel.embed("All international flight bookings must be economy class...");
   assertThat(vector).hasSize(768);
   ```

---

## 5. What I'd Change at Enterprise Scale

1. **True Multi-Tenancy & Partitioned Vector Storage**:
   Currently, policies and reports exist within a single database schema. At enterprise scale with hundreds of corporate tenants, I would implement schema-per-tenant isolation or PostgreSQL Row-Level Security (RLS). Furthermore, the `compliance_policies` table would be partitioned by `organization_id` so vector cosine distance scans remain bounded strictly within a tenant's own policy handbook.

2. **Asynchronous Decoupling via Apache Kafka**:
   Document parsing and audit execution currently leverage Spring's virtual threads within the application JVM. For high-volume enterprise ingestion (e.g. 100,000 invoices per hour during month-end close), I would decouple intake from processing using Apache Kafka topics (`document-uploaded`, `document-parsed`, `audit-completed`). Stateless worker nodes could scale horizontally based on consumer group lag.

3. **Streaming Audit Responses via Server-Sent Events (SSE)**:
   The current architecture uses frontend short-polling (every 3 seconds) to track status changes. Replacing this with Server-Sent Events (SSE) or WebSockets would allow findings to stream directly to the auditor's screen line-by-line as Gemini identifies violations, cutting perceived latency from 15 seconds to sub-second feedback.

4. **Semantic Caching with Redis**:
   Standard corporate policies (e.g. "Domestic flight coach restrictions" or "Per-diem meals capped at $100") are retrieved hundreds of times a day. Implementing a semantic cache in Redis for vector search results and tool responses would eliminate redundant LLM calls, cutting API spend by 60–80%.

5. **Human-in-the-Loop (HITL) Workflow & Adjudication**:
   AI findings should not autonomously withhold employee reimbursements without human oversight. I would introduce an auditor review stage where flags marked `CRITICAL` or `HIGH` require one-click human adjudication ("Confirm Violation", "Dismiss Exception", or "Request Itemized Receipt"), with the auditor's reasoning logged back to improve retrieval context.

6. **Immutable, Tamper-Evident Audit Trails**:
   Because FinAudit-AI is a compliance instrument, its own operations must satisfy external regulatory scrutiny (SOX Section 404, SOC 2 Type II). I would store raw model prompts, tool execution arguments, and final outputs in immutable Write-Once-Read-Many (WORM) storage (such as AWS S3 Object Lock or Amazon QLDB) with cryptographic hashing to guarantee that audit histories cannot be modified retroactively.
