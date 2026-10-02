import React, { useState, useRef, useMemo, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useVirtualizer } from '@tanstack/react-virtual';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import {
  fetchDashboardSummary,
  fetchReportsList,
  DEMO_SUMMARY,
  DEMO_REPORTS,
} from '../services/dashboardService';
import type { ReportSummary, DateRange } from '../services/dashboardService';
import {
  FileText,
  UploadCloud,
  TrendingUp,
  RefreshCw,
  ExternalLink,
  ShieldCheck,
  Sparkles,
  Filter,
  DollarSign,
  Calendar,
} from 'lucide-react';
import { motion, useReducedMotion } from 'framer-motion';
import { AnimatedNumber } from '../components/ui/AnimatedNumber';
import { AUDIT_EASE } from '../utils/motion';
import { SpendRiskTrendChart } from '../components/dashboard/SpendRiskTrendChart';
import { PolicyLeaderboard } from '../components/dashboard/PolicyLeaderboard';
import { ComplianceScoreHistogram } from '../components/dashboard/ComplianceScoreHistogram';

const statGridVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.08,
    },
  },
};

const statItemVariants = {
  hidden: { opacity: 0, y: 12 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.28, ease: AUDIT_EASE },
  },
};

// Memoized Stat Card Component
interface StatCardProps {
  title: string;
  icon: React.ReactNode;
  value: React.ReactNode;
  subtitle: string;
  className?: string;
}

const StatCard = React.memo<StatCardProps>(({ title, icon, value, subtitle, className }) => (
  <Card padding="md" hoverLift className={`flex flex-col justify-between ${className || ''}`}>
    <div className="flex items-center justify-between text-secondary">
      <span className="text-caption uppercase tracking-wider font-medium">{title}</span>
      {icon}
    </div>
    <div className="mt-4">
      <div className="text-display font-bold text-primary">{value}</div>
      <p className="text-caption text-secondary mt-1">{subtitle}</p>
    </div>
  </Card>
));
StatCard.displayName = 'StatCard';

// Memoized Virtualized Report Row Component
interface VirtualRowProps {
  report: ReportSummary;
  style: React.CSSProperties;
}

const VirtualReportRow = React.memo<VirtualRowProps>(({ report, style }) => {
  return (
    <div
      style={style}
      className="flex items-center border-b border-subtle hover:bg-surface-subtle/50 transition-colors px-6 text-body"
    >
      <div className="flex-1 min-w-[200px] flex items-center gap-3 pr-4">
        <FileText className="h-4 w-4 text-accent shrink-0" />
        <div className="truncate">
          <div className="font-medium text-primary text-body truncate">
            {report.originalFilename}
          </div>
          <div className="text-caption text-muted font-mono">
            ID: #{report.id}
          </div>
        </div>
      </div>

      <div className="w-32 hidden sm:block text-caption text-secondary font-mono">
        {new Date(report.uploadedAt).toLocaleDateString(undefined, {
          month: 'short',
          day: 'numeric',
          year: 'numeric',
        })}
      </div>

      <div className="w-28">
        <Badge status={report.status}>{report.status}</Badge>
      </div>

      <div className="w-24 text-caption">
        {report.complianceScore !== undefined && report.complianceScore !== null ? (
          <span className="font-semibold text-primary font-mono text-body">
            {report.complianceScore}/100
          </span>
        ) : (
          <span className="text-muted">—</span>
        )}
      </div>

      <div className="w-28 hidden md:block">
        {report.riskLevel ? (
          <Badge riskLevel={report.riskLevel}>{report.riskLevel}</Badge>
        ) : (
          <span className="text-caption text-muted">—</span>
        )}
      </div>

      <div className="w-24 text-right">
        <Link to={`/reports/${report.id}`}>
          <Button variant="ghost" size="sm" className="gap-1.5">
            <span>Inspect</span>
            <ExternalLink className="h-3.5 w-3.5" />
          </Button>
        </Link>
      </div>
    </div>
  );
});
VirtualReportRow.displayName = 'VirtualReportRow';

export const DashboardPage: React.FC = () => {
  const queryClient = useQueryClient();
  const shouldReduceMotion = useReducedMotion();
  const [dateRange, setDateRange] = useState<DateRange>('30d');
  const [filterRisk, setFilterRisk] = useState<string>('ALL');

  // React Query for summary with per-range key caching
  const {
    data: summary = DEMO_SUMMARY,
    isLoading: isLoadingSummary,
    isFetching: isFetchingSummary,
    refetch: refetchSummary,
  } = useQuery({
    queryKey: ['dashboardSummary', dateRange],
    queryFn: () => fetchDashboardSummary(dateRange),
    staleTime: 1000 * 60 * 5, // 5 min cache per range so switching ranges is instantaneous
  });

  // React Query for reports
  const {
    data: reports = DEMO_REPORTS,
    isLoading: isLoadingReports,
    isFetching: isFetchingReports,
    refetch: refetchReports,
  } = useQuery({
    queryKey: ['reportsList'],
    queryFn: fetchReportsList,
    staleTime: 1000 * 60 * 5,
  });

  const isRefreshing = isFetchingSummary || isFetchingReports;
  const isInitialLoading = isLoadingSummary && isLoadingReports;

  const handleRefresh = useCallback(() => {
    refetchSummary();
    refetchReports();
  }, [refetchSummary, refetchReports]);

  const handleLoadSampleDataset = useCallback(() => {
    queryClient.setQueryData(['reportsList'], DEMO_REPORTS);
  }, [queryClient]);

  const filteredReports = useMemo(() => {
    if (filterRisk === 'ALL') return reports;
    return reports.filter((r) => r.riskLevel === filterRisk);
  }, [reports, filterRisk]);

  // Virtualization ref and configuration
  const tableContainerRef = useRef<HTMLDivElement>(null);

  const rowVirtualizer = useVirtualizer({
    count: filteredReports.length,
    getScrollElement: () => tableContainerRef.current,
    estimateSize: () => 64, // 64px row height
    overscan: 5,
  });

  return (
    <div className="max-w-6xl mx-auto px-6 py-12">
      {/* Dashboard Top Header & Range Controls */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 mb-8 pb-6 border-b border-subtle">
        <div>
          <span className="text-caption font-mono uppercase tracking-widest text-accent font-semibold">
            Enterprise Financial Compliance Intelligence
          </span>
          <h1 className="text-headline font-bold text-primary mt-1">Audit Operations Portfolio</h1>
          <p className="text-body text-secondary mt-1">
            Real-time compliance surveillance, risk distribution, spend analytics, and governing policy leaderboards.
          </p>
        </div>

        {/* Action Controls & Date Range Filter */}
        <div className="flex flex-wrap items-center gap-3">
          {/* 4. DATE-RANGE FILTER PILLS */}
          <div className="flex items-center gap-1 p-1 rounded-xl bg-surface-subtle border border-subtle">
            <span className="text-[11px] font-mono uppercase text-muted px-2 hidden sm:inline-flex items-center gap-1">
              <Calendar className="h-3 w-3" />
              Range:
            </span>
            {(['7d', '30d', '90d', 'all'] as const).map((range) => {
              const label =
                range === '7d'
                  ? '7 Days'
                  : range === '30d'
                  ? '30 Days'
                  : range === '90d'
                  ? '90 Days'
                  : 'All Time';
              const isActive = dateRange === range;
              return (
                <button
                  key={range}
                  type="button"
                  onClick={() => setDateRange(range)}
                  className={`px-3 py-1.5 rounded-lg text-caption font-medium transition-all cursor-pointer ${
                    isActive
                      ? 'bg-accent text-white shadow-xs font-semibold'
                      : 'text-secondary hover:text-primary hover:bg-surface'
                  }`}
                >
                  {label}
                </button>
              );
            })}
          </div>

          <Button
            variant="secondary"
            size="md"
            onClick={handleRefresh}
            title="Refresh dashboard data"
            disabled={isRefreshing}
          >
            <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin text-accent' : ''}`} />
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
      {isInitialLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-32 w-full" />
          ))}
        </div>
      ) : (
        <motion.div
          variants={shouldReduceMotion ? undefined : statGridVariants}
          initial={shouldReduceMotion ? undefined : 'hidden'}
          animate={shouldReduceMotion ? undefined : 'show'}
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10"
        >
          {/* Card 1: Total Reports Audited */}
          <motion.div variants={shouldReduceMotion ? undefined : statItemVariants}>
            <StatCard
              title="Reports Audited"
              icon={<FileText className="h-4 w-4 text-accent" />}
              value={<AnimatedNumber value={summary.totalReportsAudited} />}
              subtitle="Processed corporate filings"
            />
          </motion.div>

          {/* Card 2: Total Spend Audited */}
          <motion.div variants={shouldReduceMotion ? undefined : statItemVariants}>
            <StatCard
              title="Total Spend Audited"
              icon={<DollarSign className="h-4 w-4 text-emerald-500" />}
              value={
                <span className="font-mono">
                  ${(summary.totalSpendAudited || 0).toLocaleString('en-US', {
                    minimumFractionDigits: 0,
                    maximumFractionDigits: 0,
                  })}
                </span>
              }
              subtitle="Aggregated line item expenditure"
            />
          </motion.div>

          {/* Card 3: Average Compliance Score */}
          <motion.div variants={shouldReduceMotion ? undefined : statItemVariants}>
            <StatCard
              title="Avg Compliance"
              icon={<TrendingUp className="h-4 w-4 text-accent" />}
              value={
                <AnimatedNumber
                  value={summary.averageComplianceScore}
                  decimals={1}
                  suffix="%"
                />
              }
              subtitle="Across selected date range"
            />
          </motion.div>

          {/* Card 4: Risk Distribution */}
          <motion.div variants={shouldReduceMotion ? undefined : statItemVariants}>
            <StatCard
              title="Risk Classification"
              icon={<ShieldCheck className="h-4 w-4 text-accent" />}
              value={
                <div className="flex items-center gap-2">
                  <div className="flex items-baseline gap-1">
                    <span className="text-title font-bold text-red-500 font-mono">
                      <AnimatedNumber value={summary.countByRiskLevel.HIGH || 0} />
                    </span>
                    <span className="text-caption text-muted">High</span>
                  </div>
                  <span className="text-muted">•</span>
                  <div className="flex items-baseline gap-1">
                    <span className="text-title font-bold text-accent font-mono">
                      <AnimatedNumber value={summary.countByRiskLevel.MEDIUM || 0} />
                    </span>
                    <span className="text-caption text-muted">Med</span>
                  </div>
                  <span className="text-muted">•</span>
                  <div className="flex items-baseline gap-1">
                    <span className="text-title font-bold text-emerald-500 font-mono">
                      <AnimatedNumber value={summary.countByRiskLevel.LOW || 0} />
                    </span>
                    <span className="text-caption text-muted">Low</span>
                  </div>
                </div>
              }
              subtitle="Breakdown by report severity"
            />
          </motion.div>
        </motion.div>
      )}

      {/* 1. SPEND & RISK TREND LINE (STACKED AREA + LINE COMPOSITION) */}
      <div className="mb-10">
        <SpendRiskTrendChart
          data={summary.spendAndRiskTrend || []}
          dateRange={dateRange}
        />
      </div>

      {/* 2 & 3. POLICY LEADERBOARD & COMPLIANCE SCORE HISTOGRAM */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mb-12 items-stretch">
        {/* Top Flagged Policies Leaderboard (7 Cols) */}
        <div className="lg:col-span-7">
          <PolicyLeaderboard violations={summary.topViolations || []} />
        </div>

        {/* Compliance Score Distribution Histogram (5 Cols) */}
        <div className="lg:col-span-5">
          <ComplianceScoreHistogram
            distribution={summary.complianceScoreDistribution || []}
            totalReports={summary.totalReportsAudited}
          />
        </div>
      </div>

      {/* Recent Reports Table Section (Virtualized) */}
      <Card padding="none" className="border-subtle bg-surface overflow-hidden shadow-xs">
        <div className="p-6 border-b border-subtle flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-subhead font-semibold text-primary">Recent Expense Reports</h2>
              <span className="px-2 py-0.5 rounded-full text-caption font-mono bg-accent/10 text-accent font-semibold">
                {filteredReports.length} {filteredReports.length === 1 ? 'Report' : 'Reports'}
              </span>
            </div>
            <p className="text-caption text-secondary mt-0.5">
              Virtualized high-throughput ledger (supports 1,000+ entries with zero lag)
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
                onClick={handleLoadSampleDataset}
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
          <div>
            {/* Table Header */}
            <div className="flex items-center border-b border-subtle bg-surface-subtle/50 text-caption uppercase text-muted font-medium tracking-wider px-6 py-3.5">
              <div className="flex-1 min-w-[200px] pr-4">Report</div>
              <div className="w-32 hidden sm:block">Uploaded</div>
              <div className="w-28">Status</div>
              <div className="w-24">Score</div>
              <div className="w-28 hidden md:block">Risk Level</div>
              <div className="w-24 text-right">Actions</div>
            </div>

            {/* Virtualized Scroll Container */}
            <div
              ref={tableContainerRef}
              className="overflow-y-auto max-h-[480px] relative divide-y divide-subtle will-change-transform"
            >
              <div
                style={{
                  height: `${rowVirtualizer.getTotalSize()}px`,
                  width: '100%',
                  position: 'relative',
                }}
              >
                {rowVirtualizer.getVirtualItems().map((virtualRow) => {
                  const report = filteredReports[virtualRow.index];
                  return (
                    <VirtualReportRow
                      key={report.id}
                      report={report}
                      style={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        width: '100%',
                        height: `${virtualRow.size}px`,
                        transform: `translateY(${virtualRow.start}px)`,
                      }}
                    />
                  );
                })}
              </div>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
};
