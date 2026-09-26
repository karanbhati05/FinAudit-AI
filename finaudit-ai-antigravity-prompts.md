# FinAudit-AI — Antigravity Prompt Pack (Milestones 0–12)

Built for Google Antigravity, using **Gemini** as the model provider throughout (via Spring AI's Google GenAI starter, Gemini Developer API key — no GCP project required). Run these prompts against the repo in order. Each milestone assumes the previous one is merged and green before you start the next — tell Antigravity to run tests before moving on.

Set once, before Milestone 0:
```
export GEMINI_API_KEY=your_key_from_aistudio.google.com/apikey
```

---

## Milestone 0 — Repo Scaffolding & Toolchain

```
Scaffold a new Spring Boot 3.4+ multi-module Maven project called "finaudit-ai" targeting Java 21 with virtual threads enabled.

Modules:
- finaudit-api (Spring Web, Spring Security, Spring AI, Spring Data JPA)
- finaudit-core (domain models, DTOs, shared utilities, no framework deps)

Dependencies to wire into finaudit-api's pom.xml via the Spring AI BOM (latest GA):
- spring-ai-starter-model-google-genai
- spring-ai-starter-model-google-genai-embedding
- spring-ai-starter-vector-store-pgvector
- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-boot-starter-actuator
- org.postgresql:postgresql
- springdoc-openapi-starter-webmvc-ui
- test scope: spring-boot-starter-test, spring-security-test, testcontainers (postgres, junit-jupiter)

Add application.yml with profiles "local" and "docker". Configure:
  spring.ai.google.genai.api-key: ${GEMINI_API_KEY}
  spring.ai.google.genai.chat.model: gemini-2.5-flash
  spring.ai.google.genai.embedding.model: gemini-embedding-001 (or the current default embedding model — check the Spring AI docs for the correct property/model name)

Add a docker-compose.yml with a postgres:16 service using the pgvector/pgvector:pg16 image, exposing 5432, with a volume for persistence and a healthcheck.

Add .gitignore, README.md with setup instructions, and a GitHub Actions workflow stub at .github/workflows/ci.yml that just runs `mvn -B verify` for now (we'll flesh it out in Milestone 11).

Commit as "chore: scaffold finaudit-ai multi-module project". Confirm the project builds with `mvn clean install` before finishing.
```

---

## Milestone 1 — Domain Model & Postgres Schema

```
In finaudit-core and finaudit-api, model the core domain:

Entities (JPA, in finaudit-api):
- User (id, email, passwordHash, role [AUDITOR, ADMIN, VIEWER], createdAt)
- Report (id, ownerId, originalFilename, storagePath, status [UPLOADED, PARSING, AUDITING, COMPLETE, FAILED], uploadedAt, auditedAt)
- ReportLineItem (id, reportId, invoiceId, vendor, amount, currency, category, rawText, lineNumber)
- CompliancePolicy (id, title, bodyText, category, effectiveDate) — the source text that gets embedded into pgvector
- AuditFinding (id, reportId, lineItemId nullable, ruleSource [SEMANTIC, DETERMINISTIC], severity [LOW, MEDIUM, HIGH, CRITICAL], description, policyReference nullable, createdAt)
- AuditRun (id, reportId, complianceScore, riskLevel [LOW, MEDIUM, HIGH], startedAt, completedAt, rawModelOutputJson)

Write Flyway migrations (add flyway-core dependency) under src/main/resources/db/migration:
- V1__init_schema.sql for the above tables with proper FKs, indexes on reportId and invoiceId, and a unique constraint on (vendor, invoiceId) to support duplicate-invoice detection.
- V2__enable_pgvector.sql that runs `CREATE EXTENSION IF NOT EXISTS vector;`

Write Spring Data JPA repositories for each entity. Add a repository method `findByVendorAndInvoiceId` on ReportLineItemRepository — this will back the duplicate-invoice tool in Milestone 6.

Write a @DataJpaTest per repository using Testcontainers Postgres (not H2 — we need real pgvector-compatible behavior) verifying basic CRUD and the duplicate-lookup query.

Commit as "feat: domain model and Postgres schema (Flyway V1-V2)". All tests must pass against the Testcontainers Postgres instance.
```

---

## Milestone 2 — Document Ingestion & Parsing Pipeline

```
Implement the report upload and parsing pipeline in finaudit-api.

1. POST /api/reports/upload — multipart file upload (PDF or plain text), max 10MB, stores the raw file to a local "storage/reports/{reportId}/" directory (abstract this behind a StorageService interface so it can be swapped for S3-compatible storage later), creates a Report row with status UPLOADED, returns 202 Accepted with the reportId.

2. Add Apache PDFBox as a dependency for text extraction. Build a ReportParsingService that:
   - Extracts raw text from the uploaded PDF (or reads it directly if .txt)
   - Uses a Spring AI ChatClient call against Gemini with a structured-output prompt to extract line items into a List<ExtractedLineItem> record (invoiceId, vendor, amount, currency, category, rawLineText) using Spring AI's StructuredOutputConverter / BeanOutputConverter
   - Persists each extracted item as a ReportLineItem row
   - Transitions Report.status to PARSING then to AUDITING once parsing completes, or FAILED with a stored error reason on failure

3. Make parsing async: trigger it via Spring's @Async on a virtual-thread executor after upload returns 202, so the HTTP call doesn't block on the LLM round trip.

4. Add GET /api/reports/{id}/status returning the current status and, if present, the extracted line item count.

Write unit tests for ReportParsingService with the ChatClient mocked (don't hit Gemini in unit tests), and one integration test that uses a small real sample PDF and actually calls Gemini (tag it with @Tag("integration") and exclude it from the default `mvn test` run, only include it in `mvn verify -Pintegration`).

Commit as "feat: report upload and async PDF parsing via Gemini structured output".
```

---

## Milestone 3 — RAG Layer: Policy Ingestion & Vector Store

```
Build the compliance-policy RAG layer.

1. Create src/main/resources/policies/corporate-spending-policy.md containing ~10 realistic corporate spending policy clauses (travel expense caps, approved vendor lists, entertainment expense limits, per-diem rules, approval thresholds by role). This is your seed data.

2. Build a PolicyIngestionService that, on application startup (behind a "seed-policies" profile flag, not on every boot), splits the policy markdown into semantically coherent chunks (use Spring AI's TokenTextSplitter), generates embeddings via the Gemini embedding model, and writes them into the PgVectorStore, along with the CompliancePolicy rows in Postgres (title, bodyText, category).

3. Configure the PgVectorStore bean explicitly: table name "compliance_policy_embeddings", dimensions matching the Gemini embedding model's output size (verify the correct dimension from Spring AI docs — do not guess), IndexType HNSW, DistanceType COSINE_DISTANCE.

4. Add a PolicySearchService wrapping VectorStore.similaritySearch(query, topK, similarityThreshold) for use by the auditing agent in Milestone 5.

5. Add an admin-only endpoint POST /api/admin/policies/reindex that re-runs ingestion (guarded by ROLE_ADMIN, wired properly once Milestone 9 lands security — for now just annotate the endpoint and leave a TODO comment referencing Milestone 9).

Write an integration test using Testcontainers Postgres with pgvector that ingests the seed policies and asserts a similarity search for "international travel over budget" returns the travel expense policy chunk in the top 3 results.

Commit as "feat: RAG policy ingestion into pgvector".
```

---

## Milestone 4 — Structured Audit Output Contract

```
Before wiring the full agent, lock down the output contract so every later milestone targets the same shape.

1. Define these Java records in finaudit-core:
   - AuditFindingDto(String ruleSource, String severity, String description, String policyReference, Long lineItemId)
   - AuditReportResult(int complianceScore, String riskLevel, List<AuditFindingDto> flaggedItems, String summary)

2. Write a Gemini system prompt (store it as a resource file prompts/audit-system-prompt.st, using Spring AI's PromptTemplate / StTemplateRenderer) that instructs the model to:
   - Only use retrieved policy context and provided line items — never invent policy text
   - Return ONLY JSON matching the AuditReportResult schema, no prose, no markdown fences
   - Assign riskLevel deterministically from complianceScore (document the exact bands in the prompt: e.g. 90-100 LOW, 70-89 MEDIUM, below 70 HIGH)

3. Wire Spring AI's BeanOutputConverter (or the newer .entity(AuditReportResult.class) ChatClient API — check current Spring AI version docs for the non-deprecated approach) so the ChatClient call deserializes straight into AuditReportResult, with converter format instructions auto-injected into the prompt.

4. Write a unit test that mocks the ChatClient's raw JSON response and asserts correct deserialization into AuditReportResult, including a case with an empty flaggedItems list and a case with multiple findings.

Commit as "feat: structured audit output contract (AuditReportResult)".
```

---

## Milestone 5 — Semantic Auditing Agent (RAG-Grounded)

```
Implement the core semantic audit workflow, combining Milestones 2-4.

1. Build AuditOrchestrationService.runAudit(reportId):
   - Loads all ReportLineItems for the report
   - For each line item (or batched, your call — justify the choice in a code comment), runs PolicySearchService.similaritySearch using a query built from the line item's category + description
   - Assembles a single ChatClient call per report (not per line item, to control cost/latency) that includes: all line items, the retrieved policy chunks with their source titles, and the audit-system-prompt from Milestone 4
   - Parses the AuditReportResult, persists an AuditRun row (complianceScore, riskLevel, rawModelOutputJson) and one AuditFinding row per flagged item, linking policyReference back to the CompliancePolicy title
   - Updates Report.status to COMPLETE

2. Trigger runAudit automatically once parsing (Milestone 2) finishes successfully — wire it as the next step in the async pipeline, not a separate manual call.

3. Add GET /api/reports/{id}/audit returning the AuditRun plus its findings as JSON.

4. Handle the "no policies retrieved above threshold" case gracefully — don't let the model hallucinate a policy citation; if similarity search returns nothing usable, the finding's policyReference should be null and ruleSource should reflect that it's a general/heuristic flag, not a policy citation.

Write unit tests mocking VectorStore and ChatClient to verify the orchestration logic (correct persistence, correct status transitions, the no-policy-retrieved edge case). Add one @Tag("integration") end-to-end test: upload a sample report with a deliberately policy-violating line item (e.g. a ₹75,000 travel claim with no VP approval note) and assert the resulting AuditRun flags it.

Commit as "feat: RAG-grounded semantic audit orchestration".
```

---

## Milestone 6 — Deterministic Tool Calling (Duplicate Invoice Detection)

```
Add Spring AI function/tool calling so the model can invoke real Java code against Postgres mid-conversation, not just reason over provided context.

1. Write a @Tool-annotated (or @Bean-registered ToolCallback, depending on current Spring AI tool-calling API — use whichever is the non-deprecated approach in the version pinned in this project) method:
   checkDatabaseForDuplicateInvoice(String vendor, String invoiceId) -> DuplicateCheckResult(boolean isDuplicate, Long originalReportId, LocalDate originalReportDate)
   backed by ReportLineItemRepository.findByVendorAndInvoiceId, excluding the current report being audited.

2. Register this tool on the ChatClient used in AuditOrchestrationService (Milestone 5) so the model can call it autonomously when it notices a line item that looks like a resubmission.

3. Update the audit-system-prompt to instruct the model: when a line item's invoice ID looks suspicious or you want to verify it hasn't been claimed before, call checkDatabaseForDuplicateInvoice rather than assuming. Findings sourced this way must set ruleSource=DETERMINISTIC.

4. Write a unit test using Spring AI's test utilities (or a manually mocked ToolCallback) verifying the tool is invoked with correct arguments when given a line item text that mentions a previously-seen invoice ID, and that the resulting finding is persisted with ruleSource=DETERMINISTIC and the correct originalReportId reference.

5. Add a focused integration test: seed two reports with the same vendor+invoiceId, audit the second one, assert a DETERMINISTIC duplicate finding referencing the first report's ID.

Commit as "feat: deterministic duplicate-invoice tool calling".
```

---

## Milestone 7 — Audit Dashboard & Aggregation API

```
Build the read-side API that a frontend (or Postman/Swagger for now) will consume.

1. GET /api/dashboard/summary — aggregated stats across all reports the caller can see: total reports audited, average complianceScore, count by riskLevel, top 5 most common policy violations (group AuditFinding by policyReference, count).

2. GET /api/reports — paginated, filterable by status/riskLevel/date range, sortable by uploadedAt or complianceScore. Use Spring Data's Pageable + Specification API rather than hand-rolled query params.

3. GET /api/reports/{id} — full detail: report metadata, all line items, the AuditRun, all findings, each finding's policyReference resolved to the policy's title and bodyText (not just the ID).

4. Add OpenAPI annotations (springdoc is already on the classpath from Milestone 0) so all of this is browsable at /swagger-ui.html with example payloads for each endpoint.

Write @WebMvcTest slice tests for each controller with the service layer mocked, covering pagination edge cases (empty result, last page) and the aggregation math in the summary endpoint.

Commit as "feat: audit dashboard and reporting API".
```

---

## Milestone 8 — Frontend (Polished Demonstration Interface)

```
Build a React + Vite + TypeScript frontend in a /frontend directory, talking to the API from Milestones 7 and 9. This is a portfolio demonstration piece, so the visual quality matters as much as the wiring — target the restrained, confident aesthetic of modern AI-company sites (Anthropic, OpenAI): calm, high-contrast typography, generous whitespace, a single accent color used sparingly, subtle motion, no visual clutter. Not corporate-dashboard-generic, not a Bootstrap default.

Design system (set this up first, in its own commit, before any page):
- Tailwind CSS with a custom theme, not default colors. Pick ONE neutral base (near-black text on off-white in light mode, near-white text on a very dark near-black — not pure #000 — in dark mode) plus ONE accent color used only for primary actions, active states, and data highlights. Support a light/dark toggle, default to system preference.
- Typography: pair a clean grotesque sans (e.g. Inter or Geist) for UI text with slightly larger, tighter-tracked headings. Establish a real type scale (e.g. 13/15/18/24/32/48px) and use it consistently — no ad hoc font sizes scattered through components.
- Spacing: generous padding on cards and sections (think 24-48px, not 8px), consistent 8px-based spacing scale, lots of breathing room around the hero and dashboard sections rather than dense grids.
- Motion: subtle only — 150-250ms ease-out transitions on hover/focus, a gentle fade-and-slide on page/route transitions (framer-motion is fine), no bouncy or flashy animation. Loading states use a calm skeleton/shimmer, not a spinner icon.
- Components: rounded corners (moderate, not pill-shaped), soft 1px borders or very subtle shadows rather than heavy drop-shadows, cards with a slight hover lift. Buttons: one solid primary style (accent-filled), one ghost/secondary style. No default browser form styling anywhere.

Pages:
- Landing / marketing page (public, no auth) at "/" — this is the demonstration front door: a hero section stating what FinAudit-AI does in one confident sentence, a short 3-step "how it works" section (upload → RAG-grounded audit → dashboard), and a "Try it" CTA into login/register. This page is what a recruiter sees first, so give it real polish — treat it like a product landing page, not an afterthought above the login form.
- Login / Register — calls the auth endpoints from Milestone 9, stores the JWT in memory (not localStorage), attaches it via an axios interceptor. Clean centered card, no visual noise.
- Upload — drag-and-drop PDF zone with a clear hover/active state, polls GET /api/reports/{id}/status until COMPLETE or FAILED, shows a calm multi-step progress indicator (Uploaded → Parsing → Auditing → Complete) rather than a generic spinner.
- Dashboard — renders GET /api/dashboard/summary as a row of stat cards (total reports, avg compliance score, risk distribution) plus a bar chart (recharts, restyled to match the theme — no default recharts colors) of findings by risk level, and a recent-reports table.
- Report Detail — line items table, findings list color-coded by severity using the accent + neutral severity scale (not default red/yellow/green), each finding's policy citation shown as an expandable quote block with the source policy title.

Make it responsive down to mobile width — the landing page and dashboard in particular should hold up on a phone, since this is what gets shared as a portfolio link.

Add a README section explaining how to run the frontend against the local backend (proxy config for dev, env var for the API base URL), plus a couple of screenshots or a short screen recording reference once the pages are built.

Commit the design system setup first ("feat: frontend design system — theme, typography, base components"), then commit each page separately (e.g. "feat: landing page", "feat: auth pages", "feat: upload flow", "feat: dashboard", "feat: report detail") so the visual work is reviewable in pieces rather than one giant diff.
```

---

## Milestone 9 — Security & RBAC

```
Wire real authentication and authorization, replacing the TODOs left in earlier milestones.

1. Implement JWT-based auth: POST /api/auth/register, POST /api/auth/login issuing a signed JWT (use a proper library, e.g. jjwt) with role embedded as a claim, short expiry + refresh token flow.

2. Configure Spring Security:
   - /api/auth/** public
   - /api/reports/upload and /api/reports/** require ROLE_AUDITOR or ROLE_ADMIN
   - /api/admin/** requires ROLE_ADMIN only (wire the reindex endpoint from Milestone 3 here properly)
   - /api/dashboard/** requires any authenticated role, but a VIEWER can only see reports where they're a participant (add an ownerId/visibility check in the service layer, not just at the security-filter level)
   - Stateless session, BCrypt password hashing, proper CORS config scoped to the frontend's dev and prod origins only

3. Add a GlobalExceptionHandler (@ControllerAdvice) returning consistent JSON error bodies for 401/403/404/409/500, no stack traces leaking to the client.

4. Write spring-security-test based tests: unauthenticated requests get 401, wrong-role requests get 403, a VIEWER can't see another user's report detail, an ADMIN can hit the reindex endpoint.

Commit as "feat: JWT auth and role-based access control".
```

---

## Milestone 10 — Test Suite Hardening

```
Audit and complete test coverage across the whole project before CI/CD.

1. Ensure every service class has unit tests with external dependencies (ChatClient, VectorStore, repositories) mocked — target realistic coverage on business logic, not just happy paths: include the "Gemini returns malformed JSON" case, the "no line items extracted" case, the "vector store returns zero matches" case, and the "duplicate tool call finds nothing" case.

2. Ensure the @Tag("integration") suite covers the full happy path end-to-end: upload -> parse -> audit -> dashboard reflects it, running against Testcontainers Postgres+pgvector and (optionally, gated by an env var so CI can skip real API calls if GEMINI_API_KEY isn't available) a real Gemini call.

3. Add a JaCoCo plugin to the build with a minimum line-coverage threshold on finaudit-core and the service layer of finaudit-api (pick a defensible number, e.g. 75%, and fail the build below it).

4. Run `mvn verify -Pintegration` and fix anything that's flaky or broken. Report the final coverage numbers in the PR description.

Commit as "test: hardened unit and integration coverage, JaCoCo gate".
```

---

## Milestone 11 — CI/CD Pipeline

```
Flesh out .github/workflows/ci.yml into a real pipeline:

1. Job "build-and-test": on push and PR to main.
   - Set up JDK 21, cache Maven deps
   - Spin up Postgres+pgvector as a GitHub Actions service container (or let Testcontainers handle it if the runner supports Docker-in-Docker — pick whichever is more reliable and explain the choice in a comment)
   - Run `mvn -B verify` (unit tests + JaCoCo gate from Milestone 10)
   - Do NOT run the @Tag("integration") Gemini-calling tests in CI on every PR — gate them behind a manual workflow_dispatch job or a "nightly" schedule, since they cost real API credits
   - Build the frontend (npm ci && npm run build) and fail the job if it errors

2. Job "docker-build": only on merge to main.
   - Build a multi-stage Dockerfile for finaudit-api (Maven build stage -> slim JRE runtime stage, non-root user)
   - Build a Dockerfile for the frontend (Vite build -> nginx serve)
   - Push both images to GHCR tagged with the commit SHA and "latest"

3. Add a docker-compose.prod.yml wiring api + frontend + postgres+pgvector together for local "production-like" testing before real deployment.

Commit as "ci: build/test/coverage pipeline and multi-stage Docker images".
```

---

## Milestone 12 — Deployment

```
Deploy FinAudit-AI to a real, publicly reachable environment (Render, Railway, Fly.io, or a small cloud VM — pick one, free/cheap tier is fine for a portfolio piece, and explain the choice in the README).

1. Provision a managed Postgres instance with the pgvector extension available (Render and Neon both support this — verify before committing to a provider).
2. Deploy the finaudit-api container with GEMINI_API_KEY, JWT signing secret, and DB connection details as platform-level secrets — never committed to the repo.
3. Deploy the frontend as a static site (or in the same container behind nginx, your call), pointed at the deployed API's public URL, with CORS configured accordingly on the backend.
4. Add a /actuator/health endpoint check and wire basic uptime monitoring (the platform's built-in health checks are fine — no need for a separate monitoring stack for a portfolio project).
5. Update the README with: live demo URL, architecture diagram (can be ASCII or a simple draw.io export), setup instructions for running locally, and a "what I'd do differently at scale" section — recruiters read that section.

Smoke-test the live deployment: register a user, log in, upload a real sample report, confirm the audit completes and shows up on the dashboard. Paste the result in the PR description as proof.

Commit as "chore: production deployment and smoke test".
```

---

## Notes for running this pack

- Each prompt assumes Antigravity has repo write access and can run `mvn`, `npm`, and `docker` in its sandbox — confirm that before Milestone 0.
- Milestones 2, 4, 5, 6 are the ones that actually burn Gemini API credits during integration testing — keep those tests tagged and gated as instructed so day-to-day CI runs stay free.
- If Antigravity's model context gets truncated mid-milestone on a big one (5, 6, 9 are the heaviest), split it into two prompts: "part A — do X" then "part B — now do Y, here's what's already in the repo" rather than resending the whole milestone.
- Verify the exact Spring AI artifact/property names against the current docs before Milestone 0 — Spring AI has renamed starters/autoconfig packages across recent versions, and a few prompts above flag this explicitly rather than hardcoding something that might already be stale.
