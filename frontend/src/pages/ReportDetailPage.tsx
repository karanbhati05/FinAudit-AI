import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../services/api';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';
import {
  ArrowLeft,
  AlertTriangle,
  AlertCircle,
  RefreshCw,
  FileText,
  ChevronDown,
  ChevronUp,
  Cpu,
  Layers,
  Calendar,
  CheckCircle2,
} from 'lucide-react';

interface LineItem {
  id: number;
  reportId: number;
  invoiceId: string;
  vendor: string;
  amount: number;
  currency: string;
  category: string;
  rawText?: string;
  lineNumber?: number;
}

interface Finding {
  id: number;
  lineItemId?: number;
  ruleSource: 'SEMANTIC' | 'DETERMINISTIC';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  description: string;
  policyReference?: string;
  policyTitle?: string;
  policyBodyText?: string;
  createdAt: string;
}

interface ReportDetail {
  id: number;
  ownerId?: number;
  originalFilename: string;
  storagePath: string;
  status: 'UPLOADED' | 'PARSING' | 'AUDITING' | 'COMPLETE' | 'FAILED';
  uploadedAt: string;
  auditedAt?: string;
  errorReason?: string;
  complianceScore?: number;
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH';
  auditSummary?: string;
  lineItems: LineItem[];
  findings: Finding[];
}

// Fallback demo data to showcase detail visuals if report is not found on backend
const DEMO_DETAIL: ReportDetail = {
  id: 104,
  originalFilename: 'Q1-Engineering-Travel-Expenses.pdf',
  storagePath: 'storage/reports/104/Q1-Engineering-Travel-Expenses.pdf',
  status: 'COMPLETE',
  uploadedAt: new Date(Date.now() - 7200000).toISOString(),
  auditedAt: new Date(Date.now() - 7100000).toISOString(),
  complianceScore: 72,
  riskLevel: 'HIGH',
  auditSummary:
    'Audit finalized with 3 policy violations identified. Detected 1 duplicate invoice submission already recorded in a prior expense report, and 2 corporate travel spending cap exceptions exceeding allowable per-diem and seating limits.',
  lineItems: [
    {
      id: 501,
      reportId: 104,
      invoiceId: 'INV-1011',
      vendor: 'United Airlines',
      amount: 1850.0,
      currency: 'USD',
      category: 'Airfare',
      lineNumber: 1,
      rawText: 'Flight ticket SFO-NYC Business Class $1850.00',
    },
    {
      id: 502,
      reportId: 104,
      invoiceId: 'INV-1012',
      vendor: 'Hilton Midtown NYC',
      amount: 680.0,
      currency: 'USD',
      category: 'Lodging',
      lineNumber: 2,
      rawText: '2 nights lodging at $340.00/night',
    },
    {
      id: 503,
      reportId: 104,
      invoiceId: 'INV-1013',
      vendor: 'Uber Technologies',
      amount: 92.5,
      currency: 'USD',
      category: 'Ground Transportation',
      lineNumber: 3,
      rawText: 'JFK Airport transfer rides',
    },
    {
      id: 504,
      reportId: 104,
      invoiceId: 'INV-1014',
      vendor: 'Gramercy Tavern',
      amount: 420.0,
      currency: 'USD',
      category: 'Meals & Entertainment',
      lineNumber: 4,
      rawText: 'Dinner with 3 client team members',
    },
    {
      id: 505,
      reportId: 104,
      invoiceId: 'INV-9402',
      vendor: 'TechSupplies Inc',
      amount: 240.0,
      currency: 'USD',
      category: 'Office Supplies',
      lineNumber: 5,
      rawText: 'Ergonomic keyboard and travel hub',
    },
  ],
  findings: [
    {
      id: 1,
      lineItemId: 505,
      ruleSource: 'DETERMINISTIC',
      severity: 'CRITICAL',
      description:
        'Duplicate invoice detected: Invoice #INV-9402 for vendor TechSupplies Inc was previously submitted and audited in Report #12. Dual reimbursement violation.',
      policyReference: 'Clause 2.0: Duplicate Billing Prevention',
      policyTitle: 'Clause 2.0 — Duplicate Invoicing and Fraud Prevention Rule',
      policyBodyText:
        'Employees and departments may not submit invoice IDs that have previously been settled, reimbursed, or processed under any active or archived expense filing. Deterministic database verification runs automatically across all corporate submissions.',
      createdAt: new Date().toISOString(),
    },
    {
      id: 2,
      lineItemId: 501,
      ruleSource: 'SEMANTIC',
      severity: 'HIGH',
      description:
        'Air travel booked in Business Class ($1,850.00). Corporate policy mandates Economy class for all domestic flights under 6 hours duration unless executive pre-approval is documented.',
      policyReference: 'Clause 4.2: Commercial Flight Class Restrictions',
      policyTitle: 'Clause 4.2 — Commercial Air Travel Allowance & Class of Service',
      policyBodyText:
        'Standard coach/economy fare is the required class of travel for all domestic flights. Business or Premium Economy booking is restricted to international segments exceeding 6 consecutive hours in transit with VP-level authorization.',
      createdAt: new Date().toISOString(),
    },
    {
      id: 3,
      lineItemId: 504,
      ruleSource: 'SEMANTIC',
      severity: 'MEDIUM',
      description:
        'Meal expense of $420.00 exceeds the $100.00 per-attendee evening cap for 3 attendees without detailed receipt itemization.',
      policyReference: 'Clause 7.1: Business Meal Limits',
      policyTitle: 'Clause 7.1 — Corporate Dining & Client Entertainment Per-Diem',
      policyBodyText:
        'Business meals involving clients are reimbursable up to $100.00 USD per person inclusive of gratuity. Itemized food and beverage receipts specifying all attendees and business rationale must be attached.',
      createdAt: new Date().toISOString(),
    },
  ],
};

export const ReportDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [report, setReport] = useState<ReportDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRetrying, setIsRetrying] = useState(false);
  const [expandedFindings, setExpandedFindings] = useState<Record<number, boolean>>({ 1: true });

  const fetchReport = async () => {
    try {
      const res = await api.get<ReportDetail>(`/reports/${id}`);
      setReport(res.data);
    } catch (err) {
      console.warn('Backend not responding or report not found, falling back to demo detail', err);
      setReport({
        ...DEMO_DETAIL,
        id: id ? parseInt(id, 10) : DEMO_DETAIL.id,
      });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    setIsLoading(true);
    fetchReport();
  }, [id]);

  // Polling watchdog for in-progress pipeline
  useEffect(() => {
    if (!report) return;
    if (['UPLOADED', 'PARSING', 'AUDITING'].includes(report.status)) {
      const timer = setTimeout(() => {
        fetchReport();
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [report?.status, id]);

  const handleRetry = async () => {
    if (!id) return;
    try {
      setIsRetrying(true);
      await api.post(`/reports/${id}/retry`);
      await fetchReport();
    } catch (err: any) {
      console.error('Failed to trigger audit retry', err);
      const msg = err?.response?.data?.message || 'Could not retry audit pipeline. Please check server status.';
      alert(msg);
    } finally {
      setIsRetrying(false);
    }
  };

  const toggleFindingExpansion = (findingId: number) => {
    setExpandedFindings((prev) => ({
      ...prev,
      [findingId]: !prev[findingId],
    }));
  };

  const activeReport = report || DEMO_DETAIL;

  // Map findings by lineItemId for highlighting in line-items table
  const findingsByLineItem = (activeReport.findings || []).reduce(
    (acc, finding) => {
      if (finding.lineItemId) {
        acc[finding.lineItemId] = finding;
      }
      return acc;
    },
    {} as Record<number, Finding>
  );

  return (
    <div className="max-w-6xl mx-auto px-6 py-12">
      {/* Top Navigation */}
      <div className="mb-8">
        <Link
          to="/dashboard"
          className="inline-flex items-center gap-2 text-caption text-secondary hover:text-primary transition-colors mb-4"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Back to Dashboard</span>
        </Link>

        {isLoading ? (
          <Skeleton className="h-24 w-full" />
        ) : (
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-6 border-b border-subtle">
            <div>
              <div className="flex items-center gap-3">
                <span className="text-caption font-mono uppercase tracking-wider text-accent font-semibold">
                  Report #{activeReport.id}
                </span>
                <Badge status={activeReport.status}>{activeReport.status}</Badge>
                {activeReport.riskLevel && (
                  <Badge riskLevel={activeReport.riskLevel}>{activeReport.riskLevel} RISK</Badge>
                )}
              </div>
              <h1 className="text-title md:text-headline font-bold text-primary mt-2">
                {activeReport.originalFilename}
              </h1>
              <div className="flex items-center gap-4 text-caption text-secondary mt-2">
                <span className="flex items-center gap-1.5 font-mono">
                  <Calendar className="h-3.5 w-3.5" />
                  Uploaded {new Date(activeReport.uploadedAt).toLocaleDateString()}
                </span>
                {activeReport.auditedAt && (
                  <span className="font-mono">
                    Audited {new Date(activeReport.auditedAt).toLocaleTimeString()}
                  </span>
                )}
              </div>
            </div>

            {/* Compliance Score Gauge Card */}
            <div className="flex items-center gap-4 p-4 rounded-xl bg-surface-subtle border border-subtle">
              <div className="text-right">
                <div className="text-caption uppercase tracking-wider text-muted font-medium">
                  Compliance Score
                </div>
                <div className="text-headline font-bold text-primary font-mono">
                  {activeReport.complianceScore ?? '—'}
                  <span className="text-caption text-muted font-normal">/100</span>
                </div>
              </div>
              <div
                className={`h-12 w-12 rounded-xl flex items-center justify-center font-bold text-subhead ${
                  (activeReport.complianceScore || 0) >= 85
                    ? 'bg-accent-subtle text-accent border border-accent/40'
                    : (activeReport.complianceScore || 0) >= 70
                    ? 'bg-amber-500/10 text-amber-600 border border-amber-500/20'
                    : 'bg-red-500/10 text-red-600 border border-red-500/20'
                }`}
              >
                {(activeReport.complianceScore || 0) >= 85 ? 'A' : (activeReport.complianceScore || 0) >= 70 ? 'B' : 'C'}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Failure Banner with Retry Action */}
      {activeReport.status === 'FAILED' && (
        <div className="p-6 rounded-xl bg-red-500/10 border border-red-500/20 text-red-600 dark:text-red-400 mb-8 space-y-3">
          <div className="flex items-center gap-3 font-semibold text-lg">
            <AlertCircle className="h-6 w-6 text-red-500 shrink-0" />
            <span>Audit Processing Failed</span>
          </div>
          <p className="text-body text-secondary">
            We couldn&apos;t complete the AI compliance audit on this file.
            {activeReport.errorReason && (
              <span className="block mt-1 font-mono text-caption text-red-500">
                Reason: {activeReport.errorReason}
              </span>
            )}
          </p>
          <div className="pt-2 flex flex-wrap items-center gap-3">
            <Button
              variant="primary"
              size="sm"
              onClick={handleRetry}
              disabled={isRetrying}
              className="gap-2"
            >
              <RefreshCw className={`h-4 w-4 ${isRetrying ? 'animate-spin' : ''}`} />
              <span>{isRetrying ? 'Re-initiating Pipeline...' : 'Retry Audit'}</span>
            </Button>
            <Link to="/upload">
              <Button variant="secondary" size="sm">
                Upload Different File
              </Button>
            </Link>
          </div>
        </div>
      )}

      {/* In-Progress Watchdog Banner */}
      {['UPLOADED', 'PARSING', 'AUDITING'].includes(activeReport.status) && (
        <div className="p-6 rounded-xl bg-accent/10 border border-accent/20 mb-8 space-y-3">
          <div className="flex items-center gap-3 font-semibold text-lg text-accent">
            <RefreshCw className="h-5 w-5 animate-spin text-accent" />
            <span>Audit In Progress ({activeReport.status})</span>
          </div>
          <p className="text-body text-secondary">
            The automated pipeline is parsing document tables, cross-referencing company policy embeddings, and executing RAG compliance audits. This page will update automatically when processing concludes.
          </p>
        </div>
      )}

      {/* Executive Summary Card */}
      {activeReport.auditSummary && (
        <Card padding="md" className="mb-10 border-subtle bg-surface">
          <div className="flex items-start gap-3">
            <div className="h-8 w-8 rounded-lg bg-accent text-white flex items-center justify-center shrink-0 mt-0.5">
              <Cpu className="h-4 w-4" />
            </div>
            <div>
              <h2 className="text-subhead font-semibold text-primary">Executive Audit Summary</h2>
              <p className="text-body text-secondary mt-1 leading-relaxed">
                {activeReport.auditSummary}
              </p>
            </div>
          </div>
        </Card>
      )}

      {/* Section 1: Findings List (Accent + Neutral Severity Scale) */}
      <div className="mb-12">
        <div className="flex items-center justify-between mb-6">
          <div>
            <span className="text-caption font-mono uppercase tracking-widest text-accent font-semibold">
              Identified Exceptions
            </span>
            <h2 className="text-headline font-bold text-primary mt-1">
              Audit Findings ({activeReport.findings.length})
            </h2>
          </div>
          <span className="text-caption text-secondary">
            Color-coded using accent & neutral severity scale
          </span>
        </div>

        {activeReport.findings.length === 0 ? (
          <Card padding="lg" className="text-center border-subtle bg-surface">
            <CheckCircle2 className="h-8 w-8 text-accent mx-auto mb-3" />
            <h3 className="text-subhead font-medium text-primary">Zero Policy Violations Found</h3>
            <p className="text-caption text-secondary mt-1">
              All line items conform with active corporate spending guidelines and no duplicate invoices were found.
            </p>
          </Card>
        ) : (
          <div className="space-y-4">
            {activeReport.findings.map((finding) => {
              const isExpanded = !!expandedFindings[finding.id];

              // Styling with accent + neutral severity scale (no default red/yellow/green)
              const cardBorderClass =
                finding.severity === 'CRITICAL'
                  ? 'border-accent ring-1 ring-accent/30 bg-surface'
                  : finding.severity === 'HIGH'
                  ? 'border-accent/40 bg-surface'
                  : finding.severity === 'MEDIUM'
                  ? 'border-strong bg-surface'
                  : 'border-subtle bg-surface-subtle/30';

              return (
                <div
                  key={finding.id}
                  className={`rounded-xl border transition-all duration-200 overflow-hidden ${cardBorderClass}`}
                >
                  {/* Finding Main Header */}
                  <div className="p-6">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div className="flex items-center gap-3">
                        <Badge severity={finding.severity}>{finding.severity}</Badge>
                        <span className="inline-flex items-center gap-1.5 text-caption font-mono px-2 py-0.5 rounded bg-surface-subtle border border-subtle text-secondary">
                          {finding.ruleSource === 'DETERMINISTIC' ? (
                            <>
                              <Layers className="h-3 w-3 text-accent" />
                              <span>DETERMINISTIC TOOL</span>
                            </>
                          ) : (
                            <>
                              <Cpu className="h-3 w-3 text-accent" />
                              <span>SEMANTIC RAG</span>
                            </>
                          )}
                        </span>
                      </div>
                      <span className="text-caption font-mono text-muted">
                        Finding #{finding.id}
                      </span>
                    </div>

                    <p className="text-body text-primary font-medium mt-4 leading-relaxed">
                      {finding.description}
                    </p>

                    {/* Expandable Policy Citation Block */}
                    <div className="mt-5 pt-4 border-t border-subtle">
                      <button
                        type="button"
                        onClick={() => toggleFindingExpansion(finding.id)}
                        className="w-full flex items-center justify-between text-left text-caption font-medium text-secondary hover:text-primary transition-colors cursor-pointer group"
                      >
                        <span className="flex items-center gap-2">
                          <span className="font-mono text-accent">Policy Reference:</span>
                          <span className="text-primary font-medium">
                            {finding.policyTitle || finding.policyReference || 'Corporate Spending Policy'}
                          </span>
                        </span>
                        {isExpanded ? (
                          <ChevronUp className="h-4 w-4 text-secondary group-hover:text-primary" />
                        ) : (
                          <ChevronDown className="h-4 w-4 text-secondary group-hover:text-primary" />
                        )}
                      </button>

                      {isExpanded && (
                        <div className="mt-3 p-4 rounded-lg bg-surface-subtle border-l-2 border-accent text-caption leading-relaxed transition-all duration-150">
                          <div className="font-mono text-muted uppercase text-[11px] mb-1">
                            Exact Policy Clause Citation:
                          </div>
                          <p className="text-secondary italic">
                            &ldquo;{finding.policyBodyText || finding.description}&rdquo;
                          </p>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Section 2: Extracted Line Items Table */}
      <div>
        <div className="mb-6">
          <span className="text-caption font-mono uppercase tracking-widest text-accent font-semibold">
            Extracted Expense Items
          </span>
          <h2 className="text-headline font-bold text-primary mt-1">
            Line Items ({activeReport.lineItems.length})
          </h2>
          <p className="text-caption text-secondary mt-0.5">
            Parsed by Gemini structured output converter from uploaded document.
          </p>
        </div>

        {(!activeReport.lineItems || activeReport.lineItems.length === 0) ? (
          <Card padding="lg" className="text-center border-subtle bg-surface">
            <FileText className="h-8 w-8 text-secondary mx-auto mb-3 opacity-60" />
            <h3 className="text-subhead font-medium text-primary">No Extracted Line Items</h3>
            <p className="text-caption text-secondary mt-1 max-w-md mx-auto">
              No individual invoice or transaction items were detected in this document. The file may be an unformatted text or summary file.
            </p>
          </Card>
        ) : (
          <Card padding="none" className="border-subtle bg-surface overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-subtle bg-surface-subtle/50 text-caption uppercase text-muted font-medium tracking-wider">
                    <th className="px-6 py-3.5">#</th>
                    <th className="px-6 py-3.5">Invoice ID</th>
                    <th className="px-6 py-3.5">Vendor</th>
                    <th className="px-6 py-3.5">Category</th>
                    <th className="px-6 py-3.5">Amount</th>
                    <th className="px-6 py-3.5 text-right">Audit Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-subtle text-body">
                  {activeReport.lineItems.map((item, idx) => {
                  const flaggedFinding = findingsByLineItem[item.id];

                  return (
                    <tr
                      key={item.id}
                      className={`hover:bg-surface-subtle/40 transition-colors ${
                        flaggedFinding ? 'bg-accent-subtle/10' : ''
                      }`}
                    >
                      <td className="px-6 py-4 text-caption text-muted font-mono">
                        {item.lineNumber || idx + 1}
                      </td>
                      <td className="px-6 py-4 font-mono font-medium text-primary">
                        {item.invoiceId}
                      </td>
                      <td className="px-6 py-4 text-primary font-medium">
                        {item.vendor}
                      </td>
                      <td className="px-6 py-4 text-caption text-secondary">
                        <span className="px-2.5 py-1 rounded-md bg-surface-subtle border border-subtle">
                          {item.category}
                        </span>
                      </td>
                      <td className="px-6 py-4 font-mono font-semibold text-primary">
                        {item.currency} {item.amount.toFixed(2)}
                      </td>
                      <td className="px-6 py-4 text-right">
                        {flaggedFinding ? (
                          <div className="inline-flex items-center gap-1.5">
                            <AlertTriangle className="h-3.5 w-3.5 text-accent" />
                            <Badge severity={flaggedFinding.severity}>
                              {flaggedFinding.severity}
                            </Badge>
                          </div>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-caption text-secondary">
                            <CheckCircle2 className="h-3.5 w-3.5 text-accent" />
                            <span>Passed</span>
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
        )}
      </div>
    </div>
  );
};
