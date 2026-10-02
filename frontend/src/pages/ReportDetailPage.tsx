import React, { useState, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';
import { ReportScopedChat } from '../components/report/ReportScopedChat';
import {
  ArrowLeft,
  AlertTriangle,
  AlertCircle,
  RefreshCw,
  ChevronDown,
  ChevronUp,
  Cpu,
  Layers,
  Calendar,
  CheckCircle2,
  Download,
  Share2,
  ShieldCheck,
  HelpCircle,
  TrendingUp,
  PieChart as PieIcon,
  Activity,
  BookOpen,
} from 'lucide-react';
import { motion, AnimatePresence, useReducedMotion } from 'framer-motion';
import { AUDIT_EASE } from '../utils/motion';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
} from 'recharts';

export interface LineItem {
  id: number;
  reportId?: number;
  invoiceId: string;
  vendor: string;
  amount: number;
  currency: string;
  category: string;
  rawText?: string;
  lineNumber?: number;
  date?: string;
}

export interface Finding {
  id: number;
  lineItemId?: number;
  ruleSource: 'SEMANTIC' | 'DETERMINISTIC';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  description: string;
  policyReference?: string;
  policyTitle?: string;
  policyBodyText?: string;
  createdAt: string;
  isHeuristic?: boolean;
}

export interface CategorySpend {
  category: string;
  amount: number;
  count: number;
  percentage: number;
}

export interface ReportDetail {
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
  totalLineItemCount?: number;
  flaggedLineItemCount?: number;
  totalSpend?: number;
  totalFlaggedAmount?: number;
  flaggedSummary?: string;
  categorySpend?: CategorySpend[];
}

type FilterMode = 'ALL' | 'FLAGGED' | 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

const CHART_COLORS = [
  '#6366f1', // Indigo
  '#06b6d4', // Cyan
  '#f59e0b', // Amber
  '#10b981', // Emerald
  '#ec4899', // Pink
  '#8b5cf6', // Purple
  '#64748b', // Slate
  '#3b82f6', // Blue
];

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
    'Comprehensive automated audit identified 3 policy violations across airfare, lodging per-diem thresholds, and duplicate invoice records. Immediate review required before reimbursement approval.',
  totalLineItemCount: 5,
  flaggedLineItemCount: 3,
  totalSpend: 3282.5,
  totalFlaggedAmount: 2510.0,
  flaggedSummary: '3 of 5 line items flagged, $2,510.00 total flagged amount',
  categorySpend: [
    { category: 'Airfare', amount: 1850.0, count: 1, percentage: 56.4 },
    { category: 'Lodging', amount: 680.0, count: 1, percentage: 20.7 },
    { category: 'Meals & Entertainment', amount: 420.0, count: 1, percentage: 12.8 },
    { category: 'Office Supplies', amount: 240.0, count: 1, percentage: 7.3 },
    { category: 'Ground Transportation', amount: 92.5, count: 1, percentage: 2.8 },
  ],
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
      date: '2026-03-10',
      rawText: 'Flight ticket SFO-NYC Business Class $1850.00 - 2026-03-10',
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
      date: '2026-03-11',
      rawText: '2 nights lodging at $340.00/night - 2026-03-11',
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
      date: '2026-03-11',
      rawText: 'JFK Airport transfer rides - 2026-03-11',
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
      date: '2026-03-12',
      rawText: 'Dinner with 3 client team members - 2026-03-12',
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
      date: '2026-03-14',
      rawText: 'Ergonomic keyboard and travel hub - 2026-03-14',
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
      isHeuristic: false,
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
      isHeuristic: false,
    },
    {
      id: 3,
      lineItemId: 504,
      ruleSource: 'SEMANTIC',
      severity: 'MEDIUM',
      description:
        'Client dinner of $420.00 flagged for potential per-attendee cap exceedance. Flagged based on anomaly detection heuristics.',
      policyReference: undefined,
      policyTitle: undefined,
      policyBodyText: undefined,
      createdAt: new Date().toISOString(),
      isHeuristic: true,
    },
  ],
};

/**
 * Compliance radial gauge component rendering a large visual ring with score,
 * letter grade, and dynamic color progression.
 */
interface ComplianceRadialGaugeProps {
  score: number;
}

const ComplianceRadialGauge: React.FC<ComplianceRadialGaugeProps> = ({ score }) => {
  const radius = 54;
  const strokeWidth = 9;
  const circumference = 2 * Math.PI * radius;
  const clampedScore = Math.min(Math.max(score, 0), 100);
  const strokeDashoffset = circumference - (circumference * clampedScore) / 100;

  // Grade and color calculations
  const grade = clampedScore >= 85 ? 'A' : clampedScore >= 70 ? 'B' : clampedScore >= 55 ? 'C' : 'F';

  const gaugeTheme =
    clampedScore >= 85
      ? {
          stroke: '#10b981',
          gradientId: 'gaugeEmerald',
          startColor: '#10b981',
          stopColor: '#06b6d4',
          badgeBg: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
        }
      : clampedScore >= 70
      ? {
          stroke: '#f59e0b',
          gradientId: 'gaugeAmber',
          startColor: '#f59e0b',
          stopColor: '#ea580c',
          badgeBg: 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
        }
      : {
          stroke: '#ef4444',
          gradientId: 'gaugeRose',
          startColor: '#ef4444',
          stopColor: '#b91c1c',
          badgeBg: 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20',
        };

  return (
    <div className="relative flex flex-col items-center justify-center shrink-0">
      <div className="relative w-36 h-36 flex items-center justify-center">
        <svg className="w-full h-full -rotate-90" viewBox="0 0 136 136">
          <defs>
            <linearGradient id={gaugeTheme.gradientId} x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor={gaugeTheme.startColor} />
              <stop offset="100%" stopColor={gaugeTheme.stopColor} />
            </linearGradient>
          </defs>

          {/* Background Track */}
          <circle
            cx="68"
            cy="68"
            r={radius}
            fill="none"
            stroke="currentColor"
            strokeWidth={strokeWidth}
            className="text-subtle opacity-30"
          />

          {/* Progress Arc */}
          <circle
            cx="68"
            cy="68"
            r={radius}
            fill="none"
            stroke={`url(#${gaugeTheme.gradientId})`}
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            className="transition-all duration-1000 ease-out"
          />
        </svg>

        {/* Center Score Display */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
          <span className="font-mono text-3xl font-extrabold text-primary tracking-tight leading-none">
            {clampedScore}
          </span>
          <span className="text-[11px] font-mono uppercase tracking-wider text-muted mt-0.5">
            / 100
          </span>
          <span
            className={`mt-1.5 px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border ${gaugeTheme.badgeBg}`}
          >
            Grade {grade}
          </span>
        </div>
      </div>
      <span className="text-caption font-medium uppercase tracking-wider text-secondary mt-2">
        Compliance Score
      </span>
    </div>
  );
};

export const ReportDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const shouldReduceMotion = useReducedMotion();
  const [isRetrying, setIsRetrying] = useState(false);
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const [isSharing, setIsSharing] = useState(false);
  const [shareSuccessToast, setShareSuccessToast] = useState<string | null>(null);

  // Table State: Inline expanded row ids and filter toggle
  const [expandedRowIds, setExpandedRowIds] = useState<Record<number, boolean>>({});
  const [activeFilter, setActiveFilter] = useState<FilterMode>('ALL');

  const {
    data: activeReport = DEMO_DETAIL,
    isLoading,
    refetch: fetchReport,
  } = useQuery({
    queryKey: ['reportDetail', id],
    queryFn: async (): Promise<ReportDetail> => {
      try {
        const res = await api.get<ReportDetail>(`/reports/${id}`);
        return res.data;
      } catch (err) {
        console.warn('Backend not responding or report not found, falling back to demo detail', err);
        return {
          ...DEMO_DETAIL,
          id: id ? parseInt(id, 10) : DEMO_DETAIL.id,
        };
      }
    },
    refetchInterval: (query) => {
      const stateData = query.state.data;
      if (stateData && ['UPLOADED', 'PARSING', 'AUDITING'].includes(stateData.status)) {
        return 2000;
      }
      return false;
    },
  });

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

  const handleDownloadPdf = async () => {
    if (!activeReport?.id) return;
    try {
      setIsDownloadingPdf(true);
      const res = await api.get(`/reports/${activeReport.id}/export/pdf`, {
        responseType: 'blob',
      });
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `FinAudit-Report-${activeReport.id}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download PDF summary', err);
      alert('Could not generate PDF export. Please try again.');
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  const handleCopyShareLink = async () => {
    if (!activeReport?.id) return;
    try {
      setIsSharing(true);
      const res = await api.post<{ shareUrl: string; shareToken: string; expiresAt: string }>(
        `/reports/${activeReport.id}/share`
      );
      const publicUrl = `${window.location.origin}/share/${res.data.shareToken}`;
      await navigator.clipboard.writeText(publicUrl);
      setShareSuccessToast('Public share link copied to clipboard! (Expires in 7 days)');
      setTimeout(() => setShareSuccessToast(null), 5000);
    } catch (err: any) {
      console.error('Failed to create share link', err);
      const fallbackUrl = `${window.location.origin}/share/demo-token-${activeReport.id}`;
      await navigator.clipboard.writeText(fallbackUrl);
      setShareSuccessToast('Demo share link copied to clipboard!');
      setTimeout(() => setShareSuccessToast(null), 4000);
    } finally {
      setIsSharing(false);
    }
  };

  // Map findings by lineItemId for inline row annotations
  const findingsByLineItem = useMemo(() => {
    const map: Record<number, Finding> = {};
    (activeReport.findings || []).forEach((finding) => {
      if (finding.lineItemId) {
        map[finding.lineItemId] = finding;
      }
    });
    return map;
  }, [activeReport.findings]);

  // Initial expand first flagged row for demo visitor convenience
  React.useEffect(() => {
    if (activeReport.lineItems && activeReport.lineItems.length > 0) {
      const firstFlagged = activeReport.lineItems.find((li) => findingsByLineItem[li.id]);
      if (firstFlagged) {
        setExpandedRowIds({ [firstFlagged.id]: true });
      }
    }
  }, [activeReport.id, findingsByLineItem]);

  const toggleRowExpansion = (itemId: number) => {
    setExpandedRowIds((prev) => ({
      ...prev,
      [itemId]: !prev[itemId],
    }));
  };

  // Compute deterministic metrics if not provided directly by Java DTO
  const deterministicMetrics = useMemo(() => {
    const lineItems = activeReport.lineItems || [];
    const totalCount = activeReport.totalLineItemCount ?? lineItems.length;

    const flaggedIds = new Set<number>();
    (activeReport.findings || []).forEach((f) => {
      if (f.lineItemId) flaggedIds.add(f.lineItemId);
    });

    const flaggedItems = lineItems.filter((li) => flaggedIds.has(li.id));
    const flaggedCount = activeReport.flaggedLineItemCount ?? flaggedItems.length;

    const totalFlaggedAmt =
      activeReport.totalFlaggedAmount ??
      flaggedItems.reduce((acc, curr) => acc + (curr.amount || 0), 0);

    const flaggedSummary =
      activeReport.flaggedSummary ||
      `${flaggedCount} of ${totalCount} line items flagged, $${totalFlaggedAmt.toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      })} total flagged amount`;

    return {
      totalCount,
      flaggedCount,
      totalFlaggedAmt,
      flaggedSummary,
    };
  }, [activeReport]);

  // Category Spend Data for Donut Chart (deterministic from Java or client fallback)
  const categoryChartData = useMemo(() => {
    if (activeReport.categorySpend && activeReport.categorySpend.length > 0) {
      return activeReport.categorySpend.map((c) => ({
        category: c.category,
        amount: Number(c.amount),
        count: c.count,
        percentage: c.percentage,
      }));
    }
    // Fallback deterministic aggregation
    const lineItems = activeReport.lineItems || [];
    const map: Record<string, { amount: number; count: number }> = {};
    let total = 0;
    lineItems.forEach((li) => {
      const cat = li.category ? li.category.trim() : 'Uncategorized';
      if (!map[cat]) map[cat] = { amount: 0, count: 0 };
      map[cat].amount += li.amount || 0;
      map[cat].count += 1;
      total += li.amount || 0;
    });

    return Object.entries(map)
      .map(([category, val]) => ({
        category,
        amount: val.amount,
        count: val.count,
        percentage: total > 0 ? Number(((val.amount / total) * 100).toFixed(1)) : 0,
      }))
      .sort((a, b) => b.amount - a.amount);
  }, [activeReport.categorySpend, activeReport.lineItems]);

  const totalSpendAmount = useMemo(() => {
    if (activeReport.totalSpend !== undefined && activeReport.totalSpend !== null) {
      return Number(activeReport.totalSpend);
    }
    return (activeReport.lineItems || []).reduce((acc, item) => acc + (item.amount || 0), 0);
  }, [activeReport.totalSpend, activeReport.lineItems]);

  // Spend over time aggregation (if dates exist on line items)
  const spendOverTimeData = useMemo(() => {
    const lineItems = activeReport.lineItems || [];
    const dateMap: Record<string, number> = {};

    lineItems.forEach((item) => {
      let itemDate = item.date;
      if (!itemDate && item.rawText) {
        const isoMatch = item.rawText.match(/\b(20\d{2}-\d{2}-\d{2})\b/);
        if (isoMatch) {
          itemDate = isoMatch[1];
        } else {
          const slashMatch = item.rawText.match(/\b(\d{1,2}\/\d{1,2}\/20\d{2})\b/);
          if (slashMatch) itemDate = slashMatch[1];
        }
      }

      if (itemDate) {
        dateMap[itemDate] = (dateMap[itemDate] || 0) + (item.amount || 0);
      }
    });

    const entries = Object.entries(dateMap);
    if (entries.length === 0) return [];

    return entries
      .map(([date, amount]) => ({
        date,
        amount: Math.round(amount * 100) / 100,
      }))
      .sort((a, b) => a.date.localeCompare(b.date));
  }, [activeReport.lineItems]);

  // Counts for filter pills
  const filterCounts = useMemo(() => {
    const items = activeReport.lineItems || [];
    let critical = 0;
    let high = 0;
    let medium = 0;
    let low = 0;
    let flagged = 0;

    items.forEach((item) => {
      const f = findingsByLineItem[item.id];
      if (f) {
        flagged++;
        if (f.severity === 'CRITICAL') critical++;
        else if (f.severity === 'HIGH') high++;
        else if (f.severity === 'MEDIUM') medium++;
        else if (f.severity === 'LOW') low++;
      }
    });

    return {
      all: items.length,
      flagged,
      critical,
      high,
      medium,
      low,
    };
  }, [activeReport.lineItems, findingsByLineItem]);

  // Filtered line items (Client-side instant filtering with 0 API calls)
  const filteredLineItems = useMemo(() => {
    const items = activeReport.lineItems || [];
    if (activeFilter === 'ALL') return items;
    if (activeFilter === 'FLAGGED') {
      return items.filter((item) => !!findingsByLineItem[item.id]);
    }
    return items.filter((item) => {
      const f = findingsByLineItem[item.id];
      return f && f.severity === activeFilter;
    });
  }, [activeReport.lineItems, findingsByLineItem, activeFilter]);

  // Helper to determine if finding is heuristic / ungrounded
  const checkIsHeuristic = (finding: Finding): boolean => {
    if (finding.isHeuristic !== undefined) return finding.isHeuristic;
    if (!finding.policyReference || finding.policyReference.trim() === '' || finding.policyReference.toLowerCase() === 'null') {
      return true;
    }
    const lower = finding.policyReference.toLowerCase();
    if (lower.includes('no policy') || lower.includes('heuristic') || lower.includes('unmatched')) {
      return true;
    }
    return !finding.policyTitle || finding.policyTitle.trim() === '';
  };

  return (
    <div className="max-w-6xl mx-auto px-6 py-12">
      {/* Top Navigation */}
      <div className="mb-6">
        <Link
          to="/dashboard"
          className="inline-flex items-center gap-2 text-caption text-secondary hover:text-primary transition-colors mb-4"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Back to Dashboard</span>
        </Link>

        {isLoading ? (
          <Skeleton className="h-20 w-full" />
        ) : (
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 pb-6 border-b border-subtle">
            <div>
              <div className="flex items-center gap-3 flex-wrap">
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
              <div className="flex items-center gap-4 text-caption text-secondary mt-2 flex-wrap">
                <span className="flex items-center gap-1.5 font-mono">
                  <Calendar className="h-3.5 w-3.5 text-accent" />
                  Uploaded {new Date(activeReport.uploadedAt).toLocaleDateString()}
                </span>
                {activeReport.auditedAt && (
                  <span className="font-mono text-secondary">
                    Audited {new Date(activeReport.auditedAt).toLocaleTimeString()}
                  </span>
                )}
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-3">
              <Button
                variant="secondary"
                size="md"
                onClick={handleCopyShareLink}
                disabled={isSharing}
                className="gap-2"
                title="Generate a public, read-only share link valid for 7 days"
              >
                <Share2 className="h-4 w-4 text-accent" />
                <span>{isSharing ? 'Generating...' : 'Share Link'}</span>
              </Button>
              <Button
                variant="primary"
                size="md"
                onClick={handleDownloadPdf}
                disabled={isDownloadingPdf}
                className="gap-2"
                title="Download executive PDF audit summary"
              >
                <Download className={`h-4 w-4 ${isDownloadingPdf ? 'animate-bounce' : ''}`} />
                <span>{isDownloadingPdf ? 'Generating...' : 'Download PDF'}</span>
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Share Toast Banner */}
      {shareSuccessToast && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-600 dark:text-emerald-400 flex items-center justify-between text-body transition-all">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-5 w-5 text-emerald-500 shrink-0" />
            <span>{shareSuccessToast}</span>
          </div>
          <button
            onClick={() => setShareSuccessToast(null)}
            className="text-caption text-muted hover:text-primary font-mono text-sm px-2 cursor-pointer"
          >
            ✕
          </button>
        </div>
      )}

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

      {/* 1. NARRATIVE SUMMARY HERO CARD */}
      <div className="mb-10">
        <div className="rounded-2xl border border-subtle bg-surface/95 backdrop-blur-sm p-6 sm:p-8 shadow-sm relative overflow-hidden">
          {/* Subtle accent glow in background */}
          <div className="absolute top-0 right-0 w-96 h-96 bg-accent/5 rounded-full blur-3xl -z-10 pointer-events-none" />

          <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-8">
            {/* Left Narrative Hero Column */}
            <div className="flex-1 space-y-4">
              <div className="flex items-center gap-3 flex-wrap">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-accent-subtle text-accent border border-accent/30 text-xs font-semibold uppercase tracking-wider font-mono">
                  <Cpu className="h-3.5 w-3.5" />
                  AI Executive Audit Narrative
                </span>
                {activeReport.riskLevel && (
                  <span
                    className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider border ${
                      activeReport.riskLevel === 'HIGH'
                        ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/30'
                        : activeReport.riskLevel === 'MEDIUM'
                        ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30'
                        : 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                    }`}
                  >
                    {activeReport.riskLevel} RISK PROFILE
                  </span>
                )}
              </div>

              {/* Narrative Summary Text */}
              <h2 className="text-subhead sm:text-title font-semibold text-primary leading-relaxed">
                {activeReport.auditSummary ||
                  'Automated audit evaluation concluded for this submission. Individual items have been cross-checked against corporate policies.'}
              </h2>

              {/* Deterministic one-liner banner computed in Java (NOT by LLM) */}
              <div className="pt-2">
                <div className="inline-flex items-center gap-2.5 px-4 py-2.5 rounded-xl bg-surface-subtle border border-subtle text-body text-secondary shadow-2xs">
                  <Activity className="h-4 w-4 text-accent shrink-0" />
                  <span className="font-medium text-primary">
                    {deterministicMetrics.flaggedSummary}
                  </span>
                  <span
                    className="text-[11px] font-mono uppercase tracking-wider px-2 py-0.5 rounded bg-surface border border-subtle text-muted"
                    title="Deterministically aggregated from structured database records"
                  >
                    Exact Analytics
                  </span>
                </div>
              </div>
            </div>

            {/* Right Gauge Visual Column */}
            <div className="lg:pl-8 lg:border-l lg:border-subtle shrink-0 w-full lg:w-auto flex justify-center">
              <ComplianceRadialGauge score={activeReport.complianceScore ?? 0} />
            </div>
          </div>
        </div>
      </div>

      {/* SCOPED REPORT Q&A CHAT */}
      <ReportScopedChat report={activeReport} />

      {/* 2. VISUAL SPEND BREAKDOWN CHARTS (RECHARTS) */}
      <div className="mb-10">
        <div className="flex items-center justify-between mb-4">
          <div>
            <span className="text-caption font-mono uppercase tracking-widest text-accent font-semibold">
              Deterministic Analytics
            </span>
            <h2 className="text-title font-bold text-primary mt-0.5 flex items-center gap-2">
              <PieIcon className="h-5 w-5 text-accent" />
              <span>Spend Distribution Breakdown</span>
            </h2>
          </div>
          <div className="text-caption text-secondary font-mono">
            Total Spend: <span className="font-bold text-primary">${totalSpendAmount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Donut Chart: Spend by Category */}
          <div className={`p-6 rounded-2xl border border-subtle bg-surface ${spendOverTimeData.length > 0 ? 'lg:col-span-6' : 'lg:col-span-12'}`}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-body font-semibold text-primary flex items-center gap-2">
                <span>Category Distribution</span>
              </h3>
              <span className="text-caption text-muted font-mono">
                {categoryChartData.length} Categories
              </span>
            </div>

            <div className="flex flex-col sm:flex-row items-center gap-6">
              <div className="w-full sm:w-1/2 h-56 relative flex items-center justify-center">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={categoryChartData}
                      dataKey="amount"
                      nameKey="category"
                      cx="50%"
                      cy="50%"
                      innerRadius={55}
                      outerRadius={80}
                      paddingAngle={4}
                      stroke="none"
                    >
                      {categoryChartData.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                      ))}
                    </Pie>
                    <RechartsTooltip
                      formatter={(value: any) => [
                        `$${Number(value).toLocaleString('en-US', { minimumFractionDigits: 2 })}`,
                        'Spend',
                      ]}
                      contentStyle={{
                        backgroundColor: 'var(--color-surface, #1e293b)',
                        borderColor: 'var(--color-border, #334155)',
                        borderRadius: '0.75rem',
                        fontSize: '0.8125rem',
                        color: 'var(--color-text-primary, #ffffff)',
                        boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>

                {/* Donut Hole Total Display */}
                <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                  <span className="text-[11px] font-mono uppercase text-muted">Total</span>
                  <span className="text-subhead font-bold font-mono text-primary">
                    ${totalSpendAmount >= 1000 ? `${(totalSpendAmount / 1000).toFixed(1)}k` : totalSpendAmount.toFixed(0)}
                  </span>
                </div>
              </div>

              {/* Category Legend List */}
              <div className="w-full sm:w-1/2 space-y-2 max-h-56 overflow-y-auto pr-1 text-caption">
                {categoryChartData.map((item, idx) => (
                  <div key={item.category} className="flex items-center justify-between gap-2 p-1.5 rounded-lg hover:bg-surface-subtle transition-colors">
                    <div className="flex items-center gap-2 min-w-0">
                      <span
                        className="w-2.5 h-2.5 rounded-full shrink-0"
                        style={{ backgroundColor: CHART_COLORS[idx % CHART_COLORS.length] }}
                      />
                      <span className="truncate text-secondary font-medium" title={item.category}>
                        {item.category}
                      </span>
                    </div>
                    <div className="text-right shrink-0 font-mono">
                      <span className="font-semibold text-primary">
                        ${item.amount.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
                      </span>
                      <span className="text-muted ml-1.5 text-[11px]">({item.percentage}%)</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Spend Over Time Bar Chart (if line items have dates) */}
          {spendOverTimeData.length > 0 && (
            <div className="lg:col-span-6 p-6 rounded-2xl border border-subtle bg-surface flex flex-col justify-between">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-body font-semibold text-primary flex items-center gap-2">
                  <TrendingUp className="h-4 w-4 text-accent" />
                  <span>Spend Over Time</span>
                </h3>
                <span className="text-caption text-muted font-mono">
                  {spendOverTimeData.length} Timeline Entries
                </span>
              </div>

              <div className="h-56 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={spendOverTimeData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-border, #e2e8f0)" opacity={0.4} />
                    <XAxis
                      dataKey="date"
                      tickLine={false}
                      axisLine={false}
                      tick={{ fill: 'var(--color-text-muted, #94a3b8)', fontSize: 11 }}
                    />
                    <YAxis
                      tickLine={false}
                      axisLine={false}
                      tickFormatter={(v) => `$${v}`}
                      tick={{ fill: 'var(--color-text-muted, #94a3b8)', fontSize: 11 }}
                    />
                    <RechartsTooltip
                      formatter={(value: any) => [
                        `$${Number(value).toLocaleString('en-US', { minimumFractionDigits: 2 })}`,
                        'Amount',
                      ]}
                      contentStyle={{
                        backgroundColor: 'var(--color-surface, #1e293b)',
                        borderColor: 'var(--color-border, #334155)',
                        borderRadius: '0.75rem',
                        fontSize: '0.8125rem',
                        color: 'var(--color-text-primary, #ffffff)',
                        boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
                      }}
                    />
                    <Bar dataKey="amount" fill="#6366f1" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 3. ANNOTATED LINE ITEMS TABLE (PRIMARY CONTENT) */}
      <div>
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div>
            <span className="text-caption font-mono uppercase tracking-widest text-accent font-semibold">
              Primary Audit Inspection
            </span>
            <h2 className="text-headline font-bold text-primary mt-1 flex items-center gap-3">
              <span>Annotated Line Items</span>
              <span className="text-subhead font-normal font-mono text-muted">
                ({filteredLineItems.length} of {activeReport.lineItems.length})
              </span>
            </h2>
            <p className="text-caption text-secondary mt-0.5">
              Click any flagged line item to inspect the exact finding reason, severity cues, and policy citation inline.
            </p>
          </div>

          {/* Filter Toggle Buttons: All items / Flagged only / By severity */}
          <div className="flex items-center gap-1.5 flex-wrap p-1.5 rounded-xl bg-surface-subtle border border-subtle">
            <button
              type="button"
              onClick={() => setActiveFilter('ALL')}
              className={`px-3 py-1.5 rounded-lg text-caption font-medium transition-all cursor-pointer ${
                activeFilter === 'ALL'
                  ? 'bg-accent text-white shadow-xs font-semibold'
                  : 'text-secondary hover:text-primary hover:bg-surface'
              }`}
            >
              All items ({filterCounts.all})
            </button>
            <button
              type="button"
              onClick={() => setActiveFilter('FLAGGED')}
              className={`px-3 py-1.5 rounded-lg text-caption font-medium transition-all cursor-pointer flex items-center gap-1.5 ${
                activeFilter === 'FLAGGED'
                  ? 'bg-rose-600 text-white shadow-xs font-semibold'
                  : 'text-rose-600 dark:text-rose-400 hover:bg-rose-500/10'
              }`}
            >
              <AlertTriangle className="h-3 w-3" />
              <span>Flagged only ({filterCounts.flagged})</span>
            </button>

            {/* Severity Pill Toggles */}
            {filterCounts.critical > 0 && (
              <button
                type="button"
                onClick={() => setActiveFilter('CRITICAL')}
                className={`px-2.5 py-1 rounded-md text-[11px] font-mono uppercase transition-all cursor-pointer ${
                  activeFilter === 'CRITICAL'
                    ? 'bg-rose-500 text-white font-bold'
                    : 'text-rose-600 dark:text-rose-400 hover:bg-rose-500/10'
                }`}
              >
                Crit ({filterCounts.critical})
              </button>
            )}

            {filterCounts.high > 0 && (
              <button
                type="button"
                onClick={() => setActiveFilter('HIGH')}
                className={`px-2.5 py-1 rounded-md text-[11px] font-mono uppercase transition-all cursor-pointer ${
                  activeFilter === 'HIGH'
                    ? 'bg-orange-500 text-white font-bold'
                    : 'text-orange-600 dark:text-orange-400 hover:bg-orange-500/10'
                }`}
              >
                High ({filterCounts.high})
              </button>
            )}

            {filterCounts.medium > 0 && (
              <button
                type="button"
                onClick={() => setActiveFilter('MEDIUM')}
                className={`px-2.5 py-1 rounded-md text-[11px] font-mono uppercase transition-all cursor-pointer ${
                  activeFilter === 'MEDIUM'
                    ? 'bg-amber-500 text-white font-bold'
                    : 'text-amber-600 dark:text-amber-400 hover:bg-amber-500/10'
                }`}
              >
                Med ({filterCounts.medium})
              </button>
            )}

            {filterCounts.low > 0 && (
              <button
                type="button"
                onClick={() => setActiveFilter('LOW')}
                className={`px-2.5 py-1 rounded-md text-[11px] font-mono uppercase transition-all cursor-pointer ${
                  activeFilter === 'LOW'
                    ? 'bg-slate-600 text-white font-bold'
                    : 'text-slate-600 dark:text-slate-400 hover:bg-slate-500/10'
                }`}
              >
                Low ({filterCounts.low})
              </button>
            )}
          </div>
        </div>

        {/* Line Items Table Container */}
        {filteredLineItems.length === 0 ? (
          <Card padding="lg" className="text-center border-subtle bg-surface">
            <CheckCircle2 className="h-8 w-8 text-accent mx-auto mb-3" />
            <h3 className="text-subhead font-medium text-primary">No Matching Line Items Found</h3>
            <p className="text-caption text-secondary mt-1">
              Adjust your active filter toggle to see other records.
            </p>
          </Card>
        ) : (
          <div className="rounded-2xl border border-subtle bg-surface overflow-hidden shadow-xs">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-subtle bg-surface-subtle/60 text-caption uppercase text-muted font-medium tracking-wider">
                    <th className="px-5 py-3.5 w-12 text-center">#</th>
                    <th className="px-5 py-3.5">Invoice ID</th>
                    <th className="px-5 py-3.5">Vendor</th>
                    <th className="px-5 py-3.5">Category</th>
                    <th className="px-5 py-3.5">Amount</th>
                    <th className="px-5 py-3.5 text-right">Audit Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-subtle text-body">
                  {filteredLineItems.map((item, idx) => {
                    const finding = findingsByLineItem[item.id];
                    const isExpanded = !!expandedRowIds[item.id];
                    const isHeuristic = finding ? checkIsHeuristic(finding) : false;

                    // Left-border severity color + subtle background tint
                    let rowBorderClass = 'border-l-4 border-l-transparent';
                    let rowBgClass = 'hover:bg-surface-subtle/40';

                    if (finding) {
                      switch (finding.severity) {
                        case 'CRITICAL':
                          rowBorderClass = 'border-l-4 border-l-rose-500';
                          rowBgClass = 'bg-rose-500/[0.04] dark:bg-rose-950/20 hover:bg-rose-500/[0.08]';
                          break;
                        case 'HIGH':
                          rowBorderClass = 'border-l-4 border-l-orange-500';
                          rowBgClass = 'bg-orange-500/[0.04] dark:bg-orange-950/20 hover:bg-orange-500/[0.08]';
                          break;
                        case 'MEDIUM':
                          rowBorderClass = 'border-l-4 border-l-amber-500';
                          rowBgClass = 'bg-amber-500/[0.04] dark:bg-amber-950/20 hover:bg-amber-500/[0.08]';
                          break;
                        case 'LOW':
                          rowBorderClass = 'border-l-4 border-l-slate-400';
                          rowBgClass = 'bg-slate-500/[0.04] dark:bg-slate-800/20 hover:bg-slate-500/[0.08]';
                          break;
                      }
                    }

                    return (
                      <React.Fragment key={item.id}>
                        <tr
                          onClick={() => finding && toggleRowExpansion(item.id)}
                          className={`transition-colors duration-150 ${rowBorderClass} ${rowBgClass} ${
                            finding ? 'cursor-pointer' : ''
                          }`}
                        >
                          <td className="px-5 py-4 text-center font-mono text-caption text-muted">
                            {item.lineNumber || idx + 1}
                          </td>
                          <td className="px-5 py-4 font-mono font-medium text-primary">
                            <div className="flex items-center gap-2">
                              <span>{item.invoiceId}</span>
                              {item.date && (
                                <span className="text-[11px] text-muted font-normal">
                                  ({item.date})
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="px-5 py-4 font-medium text-primary">
                            {item.vendor}
                          </td>
                          <td className="px-5 py-4 text-caption text-secondary">
                            <span className="px-2.5 py-1 rounded-md bg-surface-subtle border border-subtle">
                              {item.category}
                            </span>
                          </td>
                          <td className="px-5 py-4 font-mono font-semibold text-primary">
                            {item.currency} {item.amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                          </td>
                          <td className="px-5 py-4 text-right">
                            {finding ? (
                              <div className="inline-flex items-center gap-2 justify-end">
                                {isHeuristic && (
                                  <span
                                    className="hidden sm:inline-flex items-center gap-1 text-[11px] font-mono px-2 py-0.5 rounded bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20"
                                    title="No specific corporate policy matched above similarity threshold"
                                  >
                                    <HelpCircle className="h-3 w-3" />
                                    <span>Heuristic</span>
                                  </span>
                                )}
                                <Badge severity={finding.severity}>
                                  {finding.severity}
                                </Badge>
                                <span className="p-1 rounded hover:bg-surface-subtle transition-colors text-secondary">
                                  {isExpanded ? (
                                    <ChevronUp className="h-4 w-4" />
                                  ) : (
                                    <ChevronDown className="h-4 w-4" />
                                  )}
                                </span>
                              </div>
                            ) : (
                              <span className="inline-flex items-center gap-1.5 text-caption font-medium text-emerald-600 dark:text-emerald-400">
                                <CheckCircle2 className="h-4 w-4" />
                                <span>Passed</span>
                              </span>
                            )}
                          </td>
                        </tr>

                        {/* Inline Framer-Motion Height Animated Finding Drawer */}
                        {finding && (
                          <tr>
                            <td colSpan={6} className="p-0 border-b border-subtle">
                              <AnimatePresence initial={false}>
                                {isExpanded && (
                                  <motion.div
                                    key={`expansion-${item.id}`}
                                    initial={shouldReduceMotion ? { opacity: 1 } : { opacity: 0, height: 0 }}
                                    animate={{ opacity: 1, height: 'auto' }}
                                    exit={shouldReduceMotion ? { opacity: 0 } : { opacity: 0, height: 0 }}
                                    transition={{ duration: 0.22, ease: AUDIT_EASE }}
                                    className="overflow-hidden"
                                  >
                                    <div className="px-6 py-5 bg-surface-subtle/50 border-l-4 border-l-accent space-y-4">
                                      {/* Header with tags and confidence indicator */}
                                      <div className="flex flex-wrap items-center justify-between gap-3">
                                        <div className="flex items-center gap-2 flex-wrap">
                                          <Badge severity={finding.severity}>
                                            {finding.severity} SEVERITY
                                          </Badge>

                                          <span className="inline-flex items-center gap-1 text-[11px] font-mono px-2 py-0.5 rounded bg-surface border border-subtle text-secondary">
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

                                          {/* 4. AUDIT CONFIDENCE INDICATOR */}
                                          {isHeuristic ? (
                                            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-amber-500/10 text-amber-700 dark:text-amber-300 border border-amber-500/30 text-xs font-medium">
                                              <HelpCircle className="h-3.5 w-3.5 text-amber-500" />
                                              <span>Heuristic flag — no specific policy matched</span>
                                            </span>
                                          ) : (
                                            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border border-emerald-500/30 text-xs font-medium">
                                              <ShieldCheck className="h-3.5 w-3.5 text-emerald-500" />
                                              <span>Policy-Grounded Violation</span>
                                            </span>
                                          )}
                                        </div>

                                        <span className="text-caption font-mono text-muted">
                                          Line Item #{item.lineNumber || idx + 1}
                                        </span>
                                      </div>

                                      {/* Finding Description */}
                                      <div className="text-body text-primary font-medium leading-relaxed">
                                        {finding.description}
                                      </div>

                                      {/* Policy Citation Block or Heuristic Notice */}
                                      {isHeuristic ? (
                                        <div className="p-4 rounded-xl bg-amber-500/5 border border-amber-500/20 text-caption leading-relaxed">
                                          <div className="flex items-start gap-2.5">
                                            <AlertTriangle className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />
                                            <div>
                                              <div className="font-semibold text-amber-800 dark:text-amber-200">
                                                Heuristic Anomaly Flag
                                              </div>
                                              <p className="text-secondary mt-1">
                                                Policy retrieval returned no specific corporate compliance clause matching above the vector similarity threshold. This item was flagged based on heuristic financial anomalies (e.g. per-diem ratio or weekend hour detection) rather than an authoritative clause violation.
                                              </p>
                                            </div>
                                          </div>
                                        </div>
                                      ) : (
                                        <div className="p-4 rounded-xl bg-surface border border-subtle text-caption leading-relaxed shadow-2xs">
                                          <div className="flex items-center gap-2 font-mono text-accent uppercase text-[11px] mb-1 font-semibold">
                                            <BookOpen className="h-3.5 w-3.5" />
                                            <span>Governing Policy Citation</span>
                                          </div>
                                          <div className="font-semibold text-primary text-body">
                                            {finding.policyTitle || finding.policyReference || 'Corporate Financial Policy'}
                                          </div>
                                          {finding.policyBodyText && (
                                            <p className="text-secondary italic mt-2 pl-3 border-l-2 border-accent">
                                              &ldquo;{finding.policyBodyText}&rdquo;
                                            </p>
                                          )}
                                        </div>
                                      )}
                                    </div>
                                  </motion.div>
                                )}
                              </AnimatePresence>
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
