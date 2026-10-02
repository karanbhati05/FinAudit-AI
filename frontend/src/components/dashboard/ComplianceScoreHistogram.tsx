import React, { useMemo } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import { Card } from '../ui/Card';
import { BarChart3, AlertTriangle, ShieldCheck, CheckCircle2 } from 'lucide-react';
import type { ScoreDistributionBucket } from '../../services/dashboardService';

interface ComplianceScoreHistogramProps {
  distribution: ScoreDistributionBucket[];
  totalReports: number;
}

export const ComplianceScoreHistogram: React.FC<ComplianceScoreHistogramProps> = React.memo(
  ({ distribution = [], totalReports = 0 }) => {
    const hasData = useMemo(() => {
      return totalReports > 0 && distribution.some((d) => d.count > 0);
    }, [totalReports, distribution]);

    const insight = useMemo(() => {
      if (!hasData) return null;
      const cleanBucket = distribution.find((d) => d.rangeLabel === '90–100');
      const criticalBucket = distribution.find((d) => d.rangeLabel === '0–49');

      if (cleanBucket && cleanBucket.percentage >= 50) {
        return {
          type: 'positive',
          text: `${cleanBucket.percentage}% of submissions achieved Audit Ready status (90–100 score).`,
        };
      }
      if (criticalBucket && criticalBucket.count > 0) {
        return {
          type: 'warning',
          text: `${criticalBucket.count} report${criticalBucket.count > 1 ? 's' : ''} flagged in Critical Risk bracket (<50 score).`,
        };
      }
      return {
        type: 'neutral',
        text: 'Distribution spread across compliance rating brackets.',
      };
    }, [distribution, hasData]);

    return (
      <Card padding="lg" className="border-subtle bg-surface shadow-xs flex flex-col justify-between h-full">
        <div>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
            <div>
              <div className="flex items-center gap-2">
                <span className="p-1.5 rounded-lg bg-accent/10 text-accent border border-accent/20">
                  <BarChart3 className="h-4 w-4" />
                </span>
                <h2 className="text-subhead font-bold text-primary">Compliance Score Distribution</h2>
              </div>
              <p className="text-caption text-secondary mt-1">
                Histogram of automated audit scores across all submitted statements
              </p>
            </div>

            {insight && (
              <div
                className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-caption font-medium border ${
                  insight.type === 'positive'
                    ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20'
                    : insight.type === 'warning'
                    ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20'
                    : 'bg-surface-subtle text-secondary border-subtle'
                }`}
              >
                {insight.type === 'positive' ? (
                  <ShieldCheck className="h-3.5 w-3.5" />
                ) : (
                  <AlertTriangle className="h-3.5 w-3.5" />
                )}
                <span>{insight.text}</span>
              </div>
            )}
          </div>

          {!hasData ? (
            <div className="h-60 w-full rounded-xl border border-dashed border-subtle bg-surface-subtle/30 flex flex-col items-center justify-center p-6 text-center">
              <CheckCircle2 className="h-8 w-8 text-muted mb-2" />
              <h3 className="text-body font-medium text-primary">No Report Scores Available</h3>
              <p className="text-caption text-secondary max-w-sm mt-1">
                Upload expense filings to populate the portfolio compliance score distribution.
              </p>
            </div>
          ) : (
            <div>
              <div className="h-60 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={distribution} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <XAxis
                      dataKey="rangeLabel"
                      tickLine={false}
                      axisLine={false}
                      tick={{ fill: 'var(--color-text-muted, #94a3b8)', fontSize: 11 }}
                    />
                    <YAxis
                      tickLine={false}
                      axisLine={false}
                      allowDecimals={false}
                      tick={{ fill: 'var(--color-text-muted, #94a3b8)', fontSize: 11 }}
                    />
                    <RechartsTooltip
                      content={({ active, payload }) => {
                        if (active && payload && payload.length) {
                          const data = payload[0].payload as ScoreDistributionBucket;
                          return (
                            <div className="p-3 bg-surface/95 backdrop-blur-md border border-subtle rounded-xl shadow-lg text-caption space-y-1">
                              <p className="font-semibold text-primary font-mono">Score Range: {data.rangeLabel}</p>
                              <div className="flex items-center gap-2 text-secondary">
                                <span>Reports:</span>
                                <span className="font-bold text-primary font-mono">{data.count}</span>
                                <span className="text-muted">({data.percentage}%)</span>
                              </div>
                            </div>
                          );
                        }
                        return null;
                      }}
                    />
                    <Bar dataKey="count" radius={[6, 6, 0, 0]} maxBarSize={48}>
                      {distribution.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>

              {/* Bucket Percentage Legend Pills */}
              <div className="grid grid-cols-5 gap-2 mt-4 pt-3 border-t border-subtle">
                {distribution.map((b) => (
                  <div key={b.rangeLabel} className="text-center p-1.5 rounded-lg bg-surface-subtle/50 border border-subtle/60">
                    <span className="block text-[11px] font-mono text-muted">{b.rangeLabel}</span>
                    <span className="text-xs font-bold font-mono text-primary">{b.count}</span>
                    <span className="block text-[10px] text-muted">({b.percentage}%)</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="mt-4 pt-3 border-t border-subtle flex items-center justify-between text-[11px] text-muted font-mono">
          <span>5-Tier Compliance Tiers</span>
          <span>Granular Score Histogram</span>
        </div>
      </Card>
    );
  }
);

ComplianceScoreHistogram.displayName = 'ComplianceScoreHistogram';
export default ComplianceScoreHistogram;
