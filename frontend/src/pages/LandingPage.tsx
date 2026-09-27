import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import {
  FileSpreadsheet,
  Cpu,
  BarChart3,
  ArrowRight,
  CheckCircle2,
  Lock,
  Layers,
  Sparkles,
} from 'lucide-react';

export const LandingPage: React.FC = () => {
  return (
    <div className="flex flex-col min-h-screen">
      {/* Hero Section */}
      <section className="relative pt-20 pb-24 md:pt-32 md:pb-36 overflow-hidden">
        <div className="max-w-6xl mx-auto px-6">
          <div className="max-w-3xl">
            {/* Pill Tag */}
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-subtle bg-surface-subtle/80 mb-8 text-caption text-secondary">
              <span className="h-2 w-2 rounded-full bg-accent animate-pulse" />
              <span>Production-Grade Autonomous Financial Auditor</span>
            </div>

            {/* Headline */}
            <h1 className="text-display text-primary tracking-tight font-bold mb-6">
              Autonomous, hallucination-resistant financial compliance audit.
            </h1>

            {/* Subheading */}
            <p className="text-subhead text-secondary leading-relaxed mb-10 max-w-2xl">
              FinAudit-AI pairs Spring AI and Gemini with pgvector semantic retrieval and deterministic database tool-calling to audit corporate expenses against policy clauses with zero tolerance for drift.
            </p>

            {/* CTAs */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
              <Link to="/upload">
                <Button variant="primary" size="lg" className="w-full sm:w-auto group">
                  <span>Audit an Expense Report</span>
                  <ArrowRight className="h-4 w-4 ml-2 group-hover:translate-x-0.5 transition-transform" />
                </Button>
              </Link>
              <Link to="/dashboard">
                <Button variant="secondary" size="lg" className="w-full sm:w-auto">
                  View Live Dashboard
                </Button>
              </Link>
            </div>
          </div>

          {/* Interactive Preview Mockup Card */}
          <div className="mt-16 md:mt-24">
            <Card padding="none" className="overflow-hidden border border-subtle shadow-lg bg-surface">
              <div className="border-b border-subtle bg-surface-subtle px-6 py-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex gap-1.5">
                    <span className="h-3 w-3 rounded-full bg-border-strong inline-block" />
                    <span className="h-3 w-3 rounded-full bg-border-strong inline-block" />
                    <span className="h-3 w-3 rounded-full bg-border-strong inline-block" />
                  </div>
                  <span className="text-caption font-mono text-secondary">
                    REPORT-2026-Q1-TRAVEL.PDF — AUDIT SUMMARY
                  </span>
                </div>
                <div className="flex items-center gap-3">
                  <Badge severity="CRITICAL">HIGH RISK</Badge>
                  <span className="text-caption text-secondary font-mono">Score: 68/100</span>
                </div>
              </div>

              <div className="p-6 md:p-8 grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Metric 1 */}
                <div className="p-4 rounded-lg bg-surface-subtle/50 border border-subtle">
                  <div className="text-caption text-muted uppercase tracking-wider mb-1">
                    Deterministic Checks
                  </div>
                  <div className="text-title text-primary font-semibold">1 Duplicate Detected</div>
                  <p className="text-caption text-secondary mt-1">
                    Invoice #INV-9402 previously submitted in Report #12
                  </p>
                </div>

                {/* Metric 2 */}
                <div className="p-4 rounded-lg bg-surface-subtle/50 border border-subtle">
                  <div className="text-caption text-muted uppercase tracking-wider mb-1">
                    Semantic Policy Matches
                  </div>
                  <div className="text-title text-primary font-semibold">3 Clause Violations</div>
                  <p className="text-caption text-secondary mt-1">
                    Grounded in Corporate Travel & Expense Policy v2.1
                  </p>
                </div>

                {/* Metric 3 */}
                <div className="p-4 rounded-lg bg-surface-subtle/50 border border-subtle">
                  <div className="text-caption text-muted uppercase tracking-wider mb-1">
                    Total Amount Flagged
                  </div>
                  <div className="text-title text-accent font-semibold">$3,450.00 USD</div>
                  <p className="text-caption text-secondary mt-1">
                    Exceeds per-diem and executive flight thresholds
                  </p>
                </div>
              </div>

              {/* Sample finding row */}
              <div className="border-t border-subtle px-6 py-4 bg-surface flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <Badge severity="HIGH">HIGH SEVERITY</Badge>
                    <span className="text-body font-medium text-primary">
                      Airfare class exceeds standard economy allowance
                    </span>
                  </div>
                  <p className="text-caption text-secondary">
                    Flight ticket from SFO to LHR booked in Business Class ($2,400.00) without pre-approval.
                  </p>
                </div>
                <div className="text-caption font-mono text-muted shrink-0">
                  Ref: Policy Clause 4.2 (Air Travel Standards)
                </div>
              </div>
            </Card>
          </div>
        </div>
      </section>

      {/* 3-Step "How It Works" Section */}
      <section className="py-24 border-t border-subtle bg-surface-subtle/30">
        <div className="max-w-6xl mx-auto px-6">
          <div className="max-w-xl mb-16">
            <span className="text-caption font-mono uppercase tracking-widest text-accent font-semibold">
              Workflow Architecture
            </span>
            <h2 className="text-headline text-primary mt-2">
              Three stages from unstructured receipt to audited finding.
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {/* Step 1 */}
            <Card padding="lg" className="relative group">
              <div className="h-12 w-12 rounded-xl bg-accent-subtle border border-accent/20 flex items-center justify-center text-accent mb-6">
                <FileSpreadsheet className="h-6 w-6" />
              </div>
              <div className="text-caption font-mono text-accent mb-2">STAGE 01</div>
              <h3 className="text-title text-primary mb-3">Ingest & Async Parsing</h3>
              <p className="text-body text-secondary leading-relaxed">
                Raw expense PDFs or text receipts are uploaded and handled asynchronously on Java 21 virtual threads. Gemini extracts structured line items via validated schema conversion.
              </p>
            </Card>

            {/* Step 2 */}
            <Card padding="lg" className="relative group">
              <div className="h-12 w-12 rounded-xl bg-accent-subtle border border-accent/20 flex items-center justify-center text-accent mb-6">
                <Cpu className="h-6 w-6" />
              </div>
              <div className="text-caption font-mono text-accent mb-2">STAGE 02</div>
              <h3 className="text-title text-primary mb-3">RAG + Deterministic Audit</h3>
              <p className="text-body text-secondary leading-relaxed">
                The engine vector-searches relevant policy clauses in pgvector (768-dim embeddings) while invoking database tools to guarantee zero duplicate invoices across prior reports.
              </p>
            </Card>

            {/* Step 3 */}
            <Card padding="lg" className="relative group">
              <div className="h-12 w-12 rounded-xl bg-accent-subtle border border-accent/20 flex items-center justify-center text-accent mb-6">
                <BarChart3 className="h-6 w-6" />
              </div>
              <div className="text-caption font-mono text-accent mb-2">STAGE 03</div>
              <h3 className="text-title text-primary mb-3">Actionable Dashboard</h3>
              <p className="text-body text-secondary leading-relaxed">
                Aggregated metrics, compliance scores, and risk distributions are updated instantly. Auditors can inspect individual findings with full policy citations and line-item context.
              </p>
            </Card>
          </div>
        </div>
      </section>

      {/* Technical Specifications Section */}
      <section className="py-24 border-t border-subtle">
        <div className="max-w-6xl mx-auto px-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
            <div>
              <span className="text-caption font-mono uppercase tracking-widest text-accent font-semibold">
                Under the Hood
              </span>
              <h2 className="text-headline text-primary mt-2 mb-6">
                Engineered for enterprise trust and verifiable outputs.
              </h2>
              <p className="text-body text-secondary leading-relaxed mb-8">
                Unlike chat-based auditing that hallucinates rules, FinAudit-AI strictly binds model completions to retrieved policy texts in pgvector and performs exact SQL lookups for invoice verification.
              </p>

              <div className="space-y-4">
                <div className="flex items-start gap-3">
                  <CheckCircle2 className="h-5 w-5 text-accent shrink-0 mt-0.5" />
                  <div>
                    <span className="text-body font-medium text-primary">Strict Output Schemas:</span>
                    <span className="text-body text-secondary ml-1">
                      Enforced by Spring AI BeanOutputConverter directly to typed records.
                    </span>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <CheckCircle2 className="h-5 w-5 text-accent shrink-0 mt-0.5" />
                  <div>
                    <span className="text-body font-medium text-primary">pgvector Cosine Search:</span>
                    <span className="text-body text-secondary ml-1">
                      768-dimensional embeddings generated with Google's text-embedding-004.
                    </span>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <CheckCircle2 className="h-5 w-5 text-accent shrink-0 mt-0.5" />
                  <div>
                    <span className="text-body font-medium text-primary">Deterministic Tool Calling:</span>
                    <span className="text-body text-secondary ml-1">
                      Spring AI @Tool checks the database for duplicate invoices before the LLM concludes.
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <Card padding="md" className="border-subtle bg-surface">
                <div className="flex items-center gap-3 mb-2">
                  <Layers className="h-5 w-5 text-accent" />
                  <span className="text-body font-semibold text-primary">Backend Stack</span>
                </div>
                <p className="text-caption text-secondary font-mono">
                  Java 21 • Spring Boot 3.4.3 • Spring AI 1.1.8 • Flyway • PostgreSQL 16 + pgvector
                </p>
              </Card>

              <Card padding="md" className="border-subtle bg-surface">
                <div className="flex items-center gap-3 mb-2">
                  <Sparkles className="h-5 w-5 text-accent" />
                  <span className="text-body font-semibold text-primary">Model & Ingestion</span>
                </div>
                <p className="text-caption text-secondary font-mono">
                  Gemini 2.5 Flash • text-embedding-004 • Apache PDFBox 3.0.4
                </p>
              </Card>

              <Card padding="md" className="border-subtle bg-surface">
                <div className="flex items-center gap-3 mb-2">
                  <Lock className="h-5 w-5 text-accent" />
                  <span className="text-body font-semibold text-primary">Security & Isolation</span>
                </div>
                <p className="text-caption text-secondary font-mono">
                  Stateless JWT • Spring Security RBAC • In-Memory Credential Handling
                </p>
              </Card>
            </div>
          </div>
        </div>
      </section>

      {/* Call To Action Banner */}
      <section className="py-20 border-t border-subtle bg-surface-subtle/50">
        <div className="max-w-4xl mx-auto px-6 text-center">
          <h2 className="text-headline text-primary mb-4">
            Ready to experience deterministic financial auditing?
          </h2>
          <p className="text-body text-secondary mb-8 max-w-xl mx-auto">
            Upload a sample corporate receipt or browse the live dashboard to see real-time compliance scoring in action.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link to="/upload">
              <Button variant="primary" size="lg">
                Upload New Report
              </Button>
            </Link>
            <Link to="/dashboard">
              <Button variant="secondary" size="lg">
                Explore Dashboard
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
};
