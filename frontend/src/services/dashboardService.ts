import { api } from './api';
import { queryClient } from '../queryClient';

export interface DashboardSummary {
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

export const DEMO_SUMMARY: DashboardSummary = {
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

export const fetchDashboardSummary = async (): Promise<DashboardSummary> => {
  try {
    const res = await api.get<DashboardSummary>('/dashboard/summary');
    if (res.data) return res.data;
  } catch (err) {
    console.warn('Backend summary unavailable, using fallback', err);
  }
  return DEMO_SUMMARY;
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
      queryKey: ['dashboardSummary'],
      queryFn: fetchDashboardSummary,
      staleTime: 1000 * 60 * 2,
    }),
    queryClient.prefetchQuery({
      queryKey: ['reportsList'],
      queryFn: fetchReportsList,
      staleTime: 1000 * 60 * 2,
    }),
  ]);
};
