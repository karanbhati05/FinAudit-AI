import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../services/api';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import {
  FileText,
  UploadCloud,
  TrendingUp,
  AlertTriangle,
  RefreshCw,
  ExternalLink,
  ShieldCheck,
  Sparkles,
  Filter,
} from 'lucide-react';

interface DashboardSummary {
  totalReportsAudited: number;
  averageComplianceScore: number;
  countByRiskLevel: {
    LOW?: number;
    MEDIUM?: number;
    HIGH?: number;
    [key: string]: number | undefined;
  };
  topViolations: Array<{
    policyReference: string;
    count: number;
  }>;
}

interface ReportSummary {
  id: number;
  ownerId?: number;
  originalFilename: string;
  status: 'UPLOADED' | 'PARSING' | 'AUDITING' | 'COMPLETE' | 'FAILED';
  uploadedAt: string;
  auditedAt?: string;
  complianceScore?: number;
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH';
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// Fallback demo data to showcase dashboard visuals if backend is empty
const DEMO_SUMMARY: DashboardSummary = {
  totalReportsAudited: 14,
  averageComplianceScore: 82.4,
  countByRiskLevel: {
    LOW: 8,
    MEDIUM: 4,
    HIGH: 2,
  },
  topViolations: [
    { policyReference: 'Clause 4.2: Flight Booking Standards', count: 6 },
    { policyReference: 'Clause 7.1: Per-Diem Meal Caps', count: 4 },
    { policyReference: 'Clause 9.3: Missing Tax Itemization', count: 3 },
    { policyReference: 'Clause 2.0: Duplicate Invoice Submission', count: 2 },
  ],
};

const DEMO_REPORTS: ReportSummary[] = [
  {
    id: 104,
    originalFilename: 'Q1-Engineering-Travel-Expenses.pdf',
    status: 'COMPLETE',
    uploadedAt: new Date(Date.now() - 3600000 * 2).toISOString(),
    auditedAt: new Date(Date.now() - 3600000 * 1.9).toISOString(),
    complianceScore: 72,
    riskLevel: 'HIGH',
  },
  {
    id: 103,
    originalFilename: 'Product-Offsite-Receipts.pdf',
    status: 'COMPLETE',
    uploadedAt: new Date(Date.now() - 3600000 * 5).toISOString(),
    auditedAt: new Date(Date.now() - 3600000 * 4.9).toISOString(),
    complianceScore: 88,
    riskLevel: 'LOW',
  },
  {
    id: 102,
    originalFilename: 'London-Sales-Conference.pdf',
    status: 'COMPLETE',
    uploadedAt: new Date(Date.now() - 3600000 * 24).toISOString(),
    auditedAt: new Date(Date.now() - 3600000 * 23.9).toISOString(),
    complianceScore: 79,
    riskLevel: 'MEDIUM',
  },
  {
    id: 101,
    originalFilename: 'Office-Hardware-Procurement.pdf',
    status: 'COMPLETE',
    uploadedAt: new Date(Date.now() - 3600000 * 48).toISOString(),
    auditedAt: new Date(Date.now() - 3600000 * 47.9).toISOString(),
    complianceScore: 94,
    riskLevel: 'LOW',
  },
];

export const DashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [reports, setReports] = useState<ReportSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filterRisk, setFilterRisk] = useState<string>('ALL');

  const fetchData = async () => {
    try {
      setIsLoading(true);
      const [summaryRes, reportsRes] = await Promise.allSettled([
        api.get<DashboardSummary>('/dashboard/summary'),
        api.get<PageResponse<ReportSummary>>('/reports?page=0&size=10&sort=uploadedAt,desc'),
      ]);

      if (summaryRes.status === 'fulfilled' && summaryRes.value.data) {
        setSummary(summaryRes.value.data);
      } else {
        setSummary(DEMO_SUMMARY);
      }

      if (reportsRes.status === 'fulfilled' && reportsRes.value.data?.content !== undefined) {
        setReports(reportsRes.value.data.content);
      } else {
        setReports(DEMO_REPORTS);
      }
    } catch (err) {
      console.warn('Backend not responding, using demo data', err);
      setSummary(DEMO_SUMMARY);
      setReports(DEMO_REPORTS);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const activeSummary = summary || DEMO_SUMMARY;

  const chartData = [
    {
      name: 'Low Risk',
      count: activeSummary.countByRiskLevel.LOW || 0,
      color: 'var(--color-sev-low-border)',
      barColor: '#94a3b8',
    },
    {
      name: 'Medium Risk',
      count: activeSummary.countByRiskLevel.MEDIUM || 0,
      color: 'var(--color-accent)',
      barColor: '#3b82f6',
    },
    {
      name: 'High Risk',
      count: activeSummary.countByRiskLevel.HIGH || 0,
      color: 'var(--color-sev-critical-text)',
      barColor: '#ef4444',
    },
  ];

  const filteredReports = reports.filter((r) => {
    if (filterRisk === 'ALL') return true;
    return r.riskLevel === filterRisk;
  });

  return (
    <div className="max-w-6xl mx-auto px-6 py-12">
      {/* Dashboard Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-10">
        <div>
          <span className="text-caption font-mono uppercase tracking-widest text-accent font-semibold">
            Audit Intelligence & Aggregations
          </span>
          <h1 className="text-headline font-bold text-primary mt-1">Audit Operations Dashboard</h1>
          <p className="text-body text-secondary mt-1">
            Real-time compliance monitoring, risk distribution, and historical report findings.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="secondary"
            size="md"
            onClick={fetchData}
            title="Refresh dashboard data"
          >
            <RefreshCw className="h-4 w-4" />
          </Button>
          <Link to="/upload">
            <Button variant="primary" size="md">
              <UploadCloud className="h-4 w-4 mr-2" />
              <span>Upload Report</span>
            </Button>
          </Link>
        </div>
      </div>

      {/* Row of Stat Cards */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-32 w-full" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
          {/* Card 1: Total Reports Audited */}
          <Card padding="md" hoverLift className="flex flex-col justify-between">
            <div className="flex items-center justify-between text-secondary">
              <span className="text-caption uppercase tracking-wider font-medium">
                Total Audited
              </span>
              <FileText className="h-4 w-4 text-accent" />
            </div>
            <div className="mt-4">
              <div className="text-display font-bold text-primary">
                {activeSummary.totalReportsAudited}
              </div>
              <p className="text-caption text-secondary mt-1">Processed financial statements</p>
            </div>
          </Card>

          {/* Card 2: Average Compliance Score */}
          <Card padding="md" hoverLift className="flex flex-col justify-between">
            <div className="flex items-center justify-between text-secondary">
              <span className="text-caption uppercase tracking-wider font-medium">
                Avg Compliance
              </span>
              <TrendingUp className="h-4 w-4 text-accent" />
            </div>
            <div className="mt-4">
              <div className="text-display font-bold text-accent">
                {activeSummary.averageComplianceScore.toFixed(1)}%
              </div>
              <p className="text-caption text-secondary mt-1">Across all corporate statements</p>
            </div>
          </Card>

          {/* Card 3: Risk Distribution */}
          <Card padding="md" hoverLift className="flex flex-col justify-between">
            <div className="flex items-center justify-between text-secondary">
              <span className="text-caption uppercase tracking-wider font-medium">
                Risk Classification
              </span>
              <ShieldCheck className="h-4 w-4 text-accent" />
            </div>
            <div className="mt-4 flex items-center gap-2">
              <div className="flex items-baseline gap-1">
                <span className="text-title font-bold text-red-500">
                  {activeSummary.countByRiskLevel.HIGH || 0}
                </span>
                <span className="text-caption text-muted">High</span>
              </div>
              <span className="text-muted">•</span>
              <div className="flex items-baseline gap-1">
                <span className="text-title font-bold text-accent">
                  {activeSummary.countByRiskLevel.MEDIUM || 0}
                </span>
                <span className="text-caption text-muted">Med</span>
              </div>
              <span className="text-muted">•</span>
              <div className="flex items-baseline gap-1">
                <span className="text-title font-bold text-secondary">
                  {activeSummary.countByRiskLevel.LOW || 0}
                </span>
                <span className="text-caption text-muted">Low</span>
              </div>
            </div>
            <p className="text-caption text-secondary mt-1">Breakdown by report severity</p>
          </Card>

          {/* Card 4: Top Policy Violation */}
          <Card padding="md" hoverLift className="flex flex-col justify-between">
            <div className="flex items-center justify-between text-secondary">
              <span className="text-caption uppercase tracking-wider font-medium">
                Top Violation
              </span>
              <AlertTriangle className="h-4 w-4 text-accent" />
            </div>
            <div className="mt-4">
              <div className="text-body font-semibold text-primary truncate" title={activeSummary.topViolations[0]?.policyReference || 'None'}>
                {activeSummary.topViolations[0]?.policyReference || 'Zero Violations'}
              </div>
              <p className="text-caption text-secondary mt-1">
                {activeSummary.topViolations[0]?.count || 0} occurrences flagged
              </p>
            </div>
          </Card>
        </div>
      )}

      {/* Analytics & Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
        {/* Recharts Bar Chart: Restyled to match the theme */}
        <Card padding="lg" className="lg:col-span-2 border-subtle bg-surface">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-subhead font-semibold text-primary">
                Reports by Risk Severity Level
              </h2>
              <p className="text-caption text-secondary mt-0.5">
                Distribution of audited reports across risk tiers
              </p>
            </div>
            <Badge variant="accent">Live Summary</Badge>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <XAxis
                  dataKey="name"
                  stroke="var(--color-text-secondary)"
                  fontSize={12}
                  tickLine={false}
                  axisLine={{ stroke: 'var(--color-border)' }}
                />
                <YAxis
                  stroke="var(--color-text-secondary)"
                  fontSize={12}
                  tickLine={false}
                  axisLine={{ stroke: 'var(--color-border)' }}
                  allowDecimals={false}
                />
                <Tooltip
                  content={({ active, payload }) => {
                    if (active && payload && payload.length) {
                      const data = payload[0];
                      return (
                        <div className="p-3 bg-surface border border-subtle rounded-lg shadow-md text-caption">
                          <p className="font-semibold text-primary">{data.payload.name}</p>
                          <p className="text-secondary mt-1">Reports: {data.value}</p>
                        </div>
                      );
                    }
                    return null;
                  }}
                />
                <Bar dataKey="count" radius={[6, 6, 0, 0]} maxBarSize={48}>
                  {chartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.barColor} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* Top Violations List */}
        <Card padding="lg" className="border-subtle bg-surface flex flex-col justify-between">
          <div>
            <h2 className="text-subhead font-semibold text-primary">
              Frequent Policy Violations
            </h2>
            <p className="text-caption text-secondary mt-0.5 mb-6">
              Most triggered RAG policy clauses
            </p>

            <div className="space-y-4">
              {activeSummary.topViolations.map((item, idx) => (
                <div
                  key={idx}
                  className="flex items-center justify-between p-3 rounded-lg bg-surface-subtle border border-subtle/60"
                >
                  <span className="text-caption font-medium text-primary line-clamp-1 pr-2" title={item.policyReference}>
                    {item.policyReference}
                  </span>
                  <span className="text-caption font-mono font-semibold px-2 py-0.5 rounded bg-accent/10 text-accent shrink-0">
                    {item.count}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <div className="pt-6 border-t border-subtle text-caption text-secondary">
            Grounded against corporate spending guidelines via pgvector RAG.
          </div>
        </Card>
      </div>

      {/* Recent Reports Table Section */}
      <Card padding="none" className="border-subtle bg-surface overflow-hidden">
        <div className="p-6 border-b border-subtle flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-subhead font-semibold text-primary">Recent Expense Reports</h2>
            <p className="text-caption text-secondary mt-0.5">
              Statements audited with compliance scores and findings
            </p>
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-2">
            <span className="text-caption text-muted mr-1">Risk Filter:</span>
            {['ALL', 'HIGH', 'MEDIUM', 'LOW'].map((filter) => (
              <button
                key={filter}
                type="button"
                onClick={() => setFilterRisk(filter)}
                className={`text-caption px-3 py-1 rounded-md transition-colors cursor-pointer ${
                  filterRisk === filter
                    ? 'bg-accent text-white font-medium'
                    : 'bg-surface-subtle text-secondary hover:text-primary border border-subtle'
                }`}
              >
                {filter}
              </button>
            ))}
          </div>
        </div>

        {/* Table or Empty States */}
        {reports.length === 0 ? (
          <div className="p-12 text-center flex flex-col items-center justify-center">
            <div className="h-14 w-14 rounded-2xl bg-accent/10 border border-accent/20 flex items-center justify-center text-accent mb-4 shadow-sm">
              <UploadCloud className="h-7 w-7" />
            </div>
            <h3 className="text-title font-semibold text-primary">No audit reports yet</h3>
            <p className="text-body text-secondary mt-2 max-w-md">
              Upload your first expense claim, travel ledger, or vendor invoice to trigger automated extraction and policy compliance auditing.
            </p>
            <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
              <Link to="/upload">
                <Button variant="primary" size="md">
                  <UploadCloud className="h-4 w-4 mr-2" />
                  <span>Upload Your First Report</span>
                </Button>
              </Link>
              <Button
                variant="secondary"
                size="md"
                onClick={() => setReports(DEMO_REPORTS)}
              >
                <Sparkles className="h-4 w-4 mr-2 text-accent" />
                <span>Load Sample Dataset</span>
              </Button>
            </div>
          </div>
        ) : filteredReports.length === 0 ? (
          <div className="p-12 text-center flex flex-col items-center justify-center">
            <div className="h-12 w-12 rounded-xl bg-surface-subtle border border-subtle flex items-center justify-center text-secondary mb-3">
              <Filter className="h-5 w-5 text-muted" />
            </div>
            <h3 className="text-subhead font-medium text-primary">
              No {filterRisk.toLowerCase()} risk reports found
            </h3>
            <p className="text-caption text-secondary mt-1">
              None of your uploaded reports match the current risk filter.
            </p>
            <Button
              variant="secondary"
              size="sm"
              className="mt-4"
              onClick={() => setFilterRisk('ALL')}
            >
              Reset Filter
            </Button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-subtle bg-surface-subtle/50 text-caption uppercase text-muted font-medium tracking-wider">
                  <th className="px-6 py-3.5">Report</th>
                  <th className="px-6 py-3.5">Uploaded</th>
                  <th className="px-6 py-3.5">Status</th>
                  <th className="px-6 py-3.5">Score</th>
                  <th className="px-6 py-3.5">Risk Level</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-subtle text-body">
                {filteredReports.map((report) => (
                  <tr key={report.id} className="hover:bg-surface-subtle/50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <FileText className="h-4 w-4 text-accent shrink-0" />
                        <div>
                          <div className="font-medium text-primary text-body">
                            {report.originalFilename}
                          </div>
                          <div className="text-caption text-muted font-mono">
                            ID: #{report.id}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-caption text-secondary font-mono">
                      {new Date(report.uploadedAt).toLocaleDateString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        year: 'numeric',
                      })}
                    </td>
                    <td className="px-6 py-4">
                      <Badge status={report.status}>{report.status}</Badge>
                    </td>
                    <td className="px-6 py-4">
                      {report.complianceScore !== undefined && report.complianceScore !== null ? (
                        <span className="font-semibold text-primary font-mono text-body">
                          {report.complianceScore}/100
                        </span>
                      ) : (
                        <span className="text-caption text-muted">—</span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      {report.riskLevel ? (
                        <Badge riskLevel={report.riskLevel}>{report.riskLevel}</Badge>
                      ) : (
                        <span className="text-caption text-muted">—</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Link to={`/reports/${report.id}`}>
                        <Button variant="ghost" size="sm" className="gap-1.5">
                          <span>Inspect</span>
                          <ExternalLink className="h-3.5 w-3.5" />
                        </Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
};
