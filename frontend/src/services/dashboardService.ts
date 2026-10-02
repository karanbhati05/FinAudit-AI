import { api } from './api';
import { queryClient } from '../queryClient';

export type DateRange = '7d' | '30d' | '90d' | 'all';

export interface SpendRiskTrendPoint {
  label: string;
  date: string;
  totalSpend: number;
  lowRisk: number;
  mediumRisk: number;
  highRisk: number;
  reportCount: number;
}

export interface ScoreDistributionBucket {
  rangeLabel: string;
  minScore: number;
  maxScore: number;
  count: number;
  percentage: number;
  color: string;
}

export interface DashboardSummary {
  totalReportsAudited: number;
  averageComplianceScore: number;
  totalSpendAudited?: number;
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
  spendAndRiskTrend?: SpendRiskTrendPoint[];
  complianceScoreDistribution?: ScoreDistributionBucket[];
  dateRange?: string;
}

export interface ReportSummary {
  id: number;
  ownerId?: number;
  originalFilename: string;
  status: 'UPLOADED' | 'PARSING' | 'AUDITING' | 'COMPLETE' | 'FAILED';
  uploadedAt: string;
  auditedAt?: string;
  complianceScore?: number;
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH';
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// Fallback demo summary matching the 4 seeded enterprise reports
export const DEMO_SUMMARY: DashboardSummary = {
  totalReportsAudited: 4,
  averageComplianceScore: 65.3,
  totalSpendAudited: 74765.0,
  countByRiskLevel: {
    LOW: 1,
    MEDIUM: 1,
    HIGH: 2,
  },
  topViolations: [
    { policyReference: 'Corporate Travel Policy - Section 4.1: Air Travel Class Restrictions', count: 3 },
    { policyReference: 'Anti-Fraud & Invoice Verification - Section 1.1: Duplicate Invoicing Trigger', count: 2 },
    { policyReference: 'Procurement & Approval Matrix - Section 6.2: Dual Sign-off Requirements', count: 2 },
    { policyReference: 'Corporate Dining & Entertainment - Section 2.3: Per Diem Caps & Alcohol Limitations', count: 2 },
    { policyReference: 'Corporate Entertainment Standards - Section 5.4: Luxury Hospitality Authorizations', count: 1 },
    { policyReference: 'Business Expense Substantiation - Section 2.1: Non-Operating Weekend Hours', count: 1 },
    { policyReference: 'Software Licensing & IT Assets - Section 3.1: Developer Tool Pack Approvals', count: 1 },
  ],
  spendAndRiskTrend: [
    { label: 'Sep 27', date: '2026-09-27', totalSpend: 0, lowRisk: 0, mediumRisk: 0, highRisk: 0, reportCount: 0 },
    { label: 'Sep 28', date: '2026-09-28', totalSpend: 0, lowRisk: 0, mediumRisk: 0, highRisk: 0, reportCount: 0 },
    { label: 'Sep 29', date: '2026-09-29', totalSpend: 0, lowRisk: 0, mediumRisk: 0, highRisk: 0, reportCount: 0 },
    { label: 'Sep 30', date: '2026-09-30', totalSpend: 0, lowRisk: 0, mediumRisk: 0, highRisk: 0, reportCount: 0 },
    { label: 'Oct 1', date: '2026-10-01', totalSpend: 8470.0, lowRisk: 0, mediumRisk: 0, highRisk: 1, reportCount: 1 },
    { label: 'Oct 2', date: '2026-10-02', totalSpend: 12900.0, lowRisk: 1, mediumRisk: 0, highRisk: 0, reportCount: 1 },
    { label: 'Oct 3', date: '2026-10-03', totalSpend: 53395.0, lowRisk: 0, mediumRisk: 1, highRisk: 1, reportCount: 2 },
  ],
  complianceScoreDistribution: [
    { rangeLabel: '0–49', minScore: 0, maxScore: 49, count: 1, percentage: 25.0, color: '#ef4444' },
    { rangeLabel: '50–69', minScore: 50, maxScore: 69, count: 1, percentage: 25.0, color: '#f97316' },
    { rangeLabel: '70–79', minScore: 70, maxScore: 79, count: 1, percentage: 25.0, color: '#f59e0b' },
    { rangeLabel: '80–89', minScore: 80, maxScore: 89, count: 0, percentage: 0.0, color: '#06b6d4' },
    { rangeLabel: '90–100', minScore: 90, maxScore: 100, count: 1, percentage: 25.0, color: '#10b981' },
  ],
  dateRange: '30d',
};

export const DEMO_REPORTS: ReportSummary[] = [
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

export const fetchDashboardSummary = async (range: DateRange = '30d'): Promise<DashboardSummary> => {
  try {
    const res = await api.get<DashboardSummary>(`/dashboard/summary?range=${range}`);
    if (res.data) return res.data;
  } catch (err) {
    console.warn(`Backend summary for range ${range} unavailable, using fallback`, err);
  }
  return { ...DEMO_SUMMARY, dateRange: range };
};

export const fetchReportsList = async (): Promise<ReportSummary[]> => {
  try {
    const res = await api.get<PageResponse<ReportSummary>>(
      '/reports?page=0&size=100&sort=uploadedAt,desc'
    );
    if (res.data?.content && res.data.content.length > 0) {
      return res.data.content;
    }
  } catch (err) {
    console.warn('Backend reports unavailable, using fallback', err);
  }
  return DEMO_REPORTS;
};

/**
 * Preload/prefetch helper to prime React Query cache on successful login
 */
export const prefetchDashboardData = async (): Promise<void> => {
  await Promise.allSettled([
    queryClient.prefetchQuery({
      queryKey: ['dashboardSummary', '30d'],
      queryFn: () => fetchDashboardSummary('30d'),
      staleTime: 1000 * 60 * 5,
    }),
    queryClient.prefetchQuery({
      queryKey: ['reportsList'],
      queryFn: fetchReportsList,
      staleTime: 1000 * 60 * 5,
    }),
  ]);
};
