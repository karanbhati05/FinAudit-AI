import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../services/api';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';
import {
  FileText,
  Download,
  AlertCircle,
  CheckCircle2,
  Calendar,
  Layers,
  Cpu,
  ExternalLink,
  ShieldCheck,
  Clock,
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

export const PublicReportPage: React.FC = () => {
  const { token } = useParams<{ token: string }>();
  const [report, setReport] = useState<ReportDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isExpired, setIsExpired] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);

  useEffect(() => {
    if (!token) return;
    fetchPublicReport();
  }, [token]);

  const fetchPublicReport = async () => {
    try {
      setIsLoading(true);
      setIsExpired(false);
      setErrorMessage(null);
      const res = await api.get(`/reports/public/share/${token}`);
      setReport(res.data);
    } catch (err: any) {
      console.error('Failed to load public shared report', err);
      if (err?.response?.status === 410) {
        setIsExpired(true);
      } else {
        setErrorMessage(
          err?.response?.data?.message || 'Unable to load public audit report. The link may be malformed or invalid.'
        );
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleDownloadPdf = async () => {
    if (!token || !report) return;
    try {
      setIsDownloadingPdf(true);
      const res = await api.get(`/reports/public/share/${token}/export/pdf`, {
        responseType: 'blob',
      });
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `finaudit-report-${report.id}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Failed to download public PDF', err);
      alert('Could not export PDF at this moment. Please try again.');
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  if (isLoading) {
    return (
      <div className="max-w-5xl mx-auto px-6 py-16">
        <Skeleton className="h-10 w-64 mb-6" />
        <Skeleton className="h-32 w-full mb-8" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <Skeleton className="h-40" />
          <Skeleton className="h-40" />
          <Skeleton className="h-40" />
        </div>
      </div>
    );
  }

  if (isExpired) {
    return (
      <div className="max-w-2xl mx-auto px-6 py-20 text-center">
        <div className="w-16 h-16 bg-amber-500/10 text-amber-500 rounded-2xl flex items-center justify-center mx-auto mb-6 border border-amber-500/20">
          <Clock className="w-8 h-8" />
        </div>
        <h1 className="text-headline font-bold text-primary mb-3">Public Share Link Expired</h1>
        <p className="text-body text-secondary mb-8">
          This read-only audit link was time-bounded and has expired. FinAudit share tokens expire automatically after 7 days to preserve corporate document confidentiality.
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link to="/">
            <Button variant="primary">Return to Homepage</Button>
          </Link>
          <Link to="/login">
            <Button variant="secondary">Sign In to Dashboard</Button>
          </Link>
        </div>
      </div>
    );
  }

  if (errorMessage || !report) {
    return (
      <div className="max-w-2xl mx-auto px-6 py-20 text-center">
        <div className="w-16 h-16 bg-red-500/10 text-red-500 rounded-2xl flex items-center justify-center mx-auto mb-6 border border-red-500/20">
          <AlertCircle className="w-8 h-8" />
        </div>
        <h1 className="text-headline font-bold text-primary mb-3">Invalid Share Link</h1>
        <p className="text-body text-secondary mb-8">
          {errorMessage || 'The requested audit report could not be found or the link signature is invalid.'}
        </p>
        <Link to="/">
          <Button variant="primary">Return to Homepage</Button>
        </Link>
      </div>
    );
  }

  const findings = report.findings || [];
  const lineItems = report.lineItems || [];
  const score = report.complianceScore ?? 100;
  const risk = report.riskLevel ?? 'LOW';

  return (
    <div className="max-w-6xl mx-auto px-6 py-12">
      {/* Public Read-Only Banner */}
      <div className="mb-6 bg-indigo-500/10 border border-indigo-500/20 rounded-xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-indigo-500/20 text-indigo-400">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <h4 className="text-sm font-semibold text-primary">Public Audit Result (Read-Only)</h4>
            <p className="text-xs text-secondary">
              Viewing authenticated audit run for corporate claim review. No login required.
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <Button
            variant="secondary"
            size="sm"
            onClick={handleDownloadPdf}
            disabled={isDownloadingPdf}
            className="flex-1 sm:flex-none flex items-center justify-center gap-2"
          >
            <Download className="h-4 w-4" />
            <span>{isDownloadingPdf ? 'Generating PDF...' : 'Download PDF'}</span>
          </Button>
          <Link to="/" className="flex-1 sm:flex-none">
            <Button variant="primary" size="sm" className="w-full flex items-center justify-center gap-1.5">
              <span>Try FinAudit</span>
              <ExternalLink className="h-3.5 w-3.5" />
            </Button>
          </Link>
        </div>
      </div>

      {/* Header Info */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-6 border-b border-subtle mb-8">
        <div>
          <div className="flex items-center gap-3">
            <span className="text-caption font-mono uppercase tracking-wider text-accent font-semibold">
              Report #{report.id}
            </span>
            <Badge status={report.status}>{report.status}</Badge>
            <Badge riskLevel={risk}>{risk} RISK</Badge>
          </div>
          <h1 className="text-title md:text-headline font-bold text-primary mt-2">
            {report.originalFilename}
          </h1>
          <div className="flex items-center gap-4 text-caption text-secondary mt-2">
            <span className="flex items-center gap-1.5 font-mono">
              <Calendar className="h-3.5 w-3.5" />
              Uploaded {new Date(report.uploadedAt).toLocaleDateString()}
            </span>
            {report.auditedAt && (
              <span className="font-mono">
                Audited {new Date(report.auditedAt).toLocaleTimeString()}
              </span>
            )}
          </div>
        </div>

        {/* Score Card */}
        <div className="flex items-center gap-4 p-4 rounded-xl bg-surface-subtle border border-subtle">
          <div className="text-right">
            <div className="text-caption uppercase tracking-wider text-muted font-medium">
              Compliance Score
            </div>
            <div className="text-headline font-bold text-primary font-mono">
              {score}
              <span className="text-caption text-muted font-normal">/100</span>
            </div>
          </div>
          <div
            className={`h-12 w-12 rounded-xl flex items-center justify-center font-bold text-subhead ${
              score >= 85
                ? 'bg-accent-subtle text-accent border border-accent/40'
                : score >= 70
                ? 'bg-amber-500/10 text-amber-600 border border-amber-500/20'
                : 'bg-red-500/10 text-red-600 border border-red-500/20'
            }`}
          >
            {score >= 85 ? 'A' : score >= 70 ? 'B' : 'C'}
          </div>
        </div>
      </div>

      {/* Executive Summary */}
      {report.auditSummary && (
        <Card className="mb-8 border-l-4 border-l-accent p-6">
          <h3 className="text-subhead font-semibold text-primary mb-2 flex items-center gap-2">
            <FileText className="h-4 w-4 text-accent" />
            Executive Audit Summary
          </h3>
          <p className="text-body text-secondary leading-relaxed">{report.auditSummary}</p>
        </Card>
      )}

      {/* Findings Section */}
      <div className="mb-12">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <h2 className="text-subhead font-semibold text-primary">Compliance Findings</h2>
            <span className="px-2 py-0.5 rounded-full bg-surface-subtle border border-subtle text-xs font-mono text-secondary">
              {findings.length}
            </span>
          </div>
        </div>

        {findings.length === 0 ? (
          <Card className="p-8 text-center bg-surface-subtle border-subtle">
            <CheckCircle2 className="h-10 w-10 text-emerald-500 mx-auto mb-2" />
            <h4 className="text-body font-semibold text-primary">Clean Audit</h4>
            <p className="text-caption text-secondary">
              No compliance violations or policy discrepancies detected across all submitted items.
            </p>
          </Card>
        ) : (
          <div className="space-y-4">
            {findings.map((finding) => (
              <Card
                key={finding.id}
                className="overflow-hidden border border-subtle transition-all duration-200"
              >
                <div className="p-5">
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 mb-3">
                    <div className="flex items-center gap-2">
                      <Badge severity={finding.severity}>{finding.severity}</Badge>
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[11px] font-mono font-medium bg-surface-subtle text-secondary border border-subtle">
                        {finding.ruleSource === 'DETERMINISTIC' ? (
                          <>
                            <Cpu className="h-3 w-3 text-emerald-500" />
                            Deterministic Tool
                          </>
                        ) : (
                          <>
                            <Layers className="h-3 w-3 text-indigo-500" />
                            Grounded RAG
                          </>
                        )}
                      </span>
                    </div>
                  </div>

                  <p className="text-body text-primary font-medium mb-3">{finding.description}</p>

                  {/* Policy Citation */}
                  {finding.policyReference && (
                    <div className="mt-3 p-3 rounded-lg bg-surface-subtle border border-subtle text-xs">
                      <div className="font-semibold text-primary mb-1">
                        Policy Citation: {finding.policyReference}
                      </div>
                      {finding.policyTitle && (
                        <div className="text-secondary font-mono text-[11px] mb-1">
                          Clause: {finding.policyTitle}
                        </div>
                      )}
                      {finding.policyBodyText && (
                        <p className="text-secondary text-[11px] leading-relaxed line-clamp-2">
                          "{finding.policyBodyText}"
                        </p>
                      )}
                    </div>
                  )}
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Extracted Line Items */}
      <div>
        <h2 className="text-subhead font-semibold text-primary mb-4">
          Itemized Invoices & Claims ({lineItems.length})
        </h2>
        <div className="rounded-xl border border-subtle overflow-hidden">
          <table className="w-full text-left text-sm">
            <thead className="bg-surface-subtle text-secondary font-mono text-xs uppercase border-b border-subtle">
              <tr>
                <th className="py-3 px-4">Line</th>
                <th className="py-3 px-4">Invoice ID</th>
                <th className="py-3 px-4">Vendor</th>
                <th className="py-3 px-4">Category</th>
                <th className="py-3 px-4 text-right">Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-subtle font-mono text-xs">
              {lineItems.map((item, idx) => (
                <tr key={item.id} className="hover:bg-surface-subtle/50 transition-colors">
                  <td className="py-3 px-4 text-secondary">{item.lineNumber || idx + 1}</td>
                  <td className="py-3 px-4 font-semibold text-primary">{item.invoiceId}</td>
                  <td className="py-3 px-4 text-primary font-sans">{item.vendor}</td>
                  <td className="py-3 px-4 text-secondary font-sans">{item.category}</td>
                  <td className="py-3 px-4 text-right font-bold text-primary">
                    {item.currency} {item.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
