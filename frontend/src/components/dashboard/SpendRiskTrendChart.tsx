import React, { useMemo } from 'react';
import {
  ComposedChart,
  Area,
  Line,
  XAxis,
  YAxis,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  CartesianGrid,
  Legend,
} from 'recharts';
import { Card } from '../ui/Card';
import { TrendingUp, Layers, DollarSign, AlertCircle } from 'lucide-react';
import type { SpendRiskTrendPoint } from '../../services/dashboardService';

interface SpendRiskTrendChartProps {
  data: SpendRiskTrendPoint[];
  dateRange: string;
}

export const SpendRiskTrendChart: React.FC<SpendRiskTrendChartProps> = React.memo(({ data = [], dateRange }) => {
  const hasData = useMemo(() => {
    return data && data.length > 0 && data.some((d) => d.totalSpend > 0 || d.reportCount > 0);
  }, [data]);

  const maxSpend = useMemo(() => {
    if (!data.length) return 0;
    return Math.max(...data.map((d) => d.totalSpend || 0));
  }, [data]);

  const totalRangeSpend = useMemo(() => {
    return data.reduce((acc, curr) => acc + (curr.totalSpend || 0), 0);
  }, [data]);

  const formatSpendTick = (val: number): string => {
    if (val >= 1_000_000) return `$${(val / 1_000_000).toFixed(1)}M`;
    if (val >= 1_000) return `$${(val / 1_000).toFixed(0)}k`;
    return `$${val}`;
  };

  return (
    <Card padding="lg" className="border-subtle bg-surface shadow-xs flex flex-col justify-between">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-1.5 rounded-lg bg-accent/10 text-accent border border-accent/20">
              <TrendingUp className="h-4 w-4" />
            </span>
            <h2 className="text-subhead font-bold text-primary">Spend & Risk Composition Trend</h2>
          </div>
          <p className="text-caption text-secondary mt-1">
            Total corporate expenditures audited with underlying report risk severity overlay
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="px-3 py-1.5 rounded-xl bg-surface-subtle border border-subtle text-right">
            <span className="text-[11px] font-mono uppercase text-muted block">Period Spend</span>
            <span className="text-body font-mono font-bold text-primary">
              ${totalRangeSpend.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
            </span>
          </div>
        </div>
      </div>

      {!hasData ? (
        <div className="h-72 w-full rounded-xl border border-dashed border-subtle bg-surface-subtle/30 flex flex-col items-center justify-center p-6 text-center">
          <AlertCircle className="h-8 w-8 text-muted mb-2" />
          <h3 className="text-body font-medium text-primary">No Expenditure Activity In This Window</h3>
          <p className="text-caption text-secondary max-w-sm mt-1">
            No expense reports or invoice batches were filed in the selected {dateRange} timeframe.
          </p>
        </div>
      ) : (
        <div className="h-72 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={data} margin={{ top: 12, right: 12, left: -10, bottom: 0 }}>
              <defs>
                <linearGradient id="spendLineGlow" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="0%" stopColor="#818cf8" />
                  <stop offset="100%" stopColor="#4f46e5" />
                </linearGradient>
                <linearGradient id="areaHighRisk" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#ef4444" stopOpacity={0.45} />
                  <stop offset="100%" stopColor="#ef4444" stopOpacity={0.05} />
                </linearGradient>
                <linearGradient id="areaMedRisk" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#f59e0b" stopOpacity={0.4} />
                  <stop offset="100%" stopColor="#f59e0b" stopOpacity={0.05} />
                </linearGradient>
                <linearGradient id="areaLowRisk" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#10b981" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#10b981" stopOpacity={0.05} />
                </linearGradient>
              </defs>

              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-border, #334155)" opacity={0.35} />

              <XAxis
                dataKey="label"
                tickLine={false}
                axisLine={false}
                tick={{ fill: 'var(--color-text-muted, #94a3b8)', fontSize: 11 }}
              />

              {/* Left Y Axis: Total Spend ($) */}
              <YAxis
                yAxisId="spend"
                orientation="left"
                tickLine={false}
                axisLine={false}
                tickFormatter={formatSpendTick}
                domain={[0, maxSpend > 0 ? Math.ceil(maxSpend * 1.15) : 1000]}
                tick={{ fill: 'var(--color-text-muted, #94a3b8)', fontSize: 11 }}
              />

              {/* Right Y Axis: Report Count Stacked */}
              <YAxis
                yAxisId="count"
                orientation="right"
                tickLine={false}
                axisLine={false}
                allowDecimals={false}
                tick={{ fill: 'var(--color-text-muted, #94a3b8)', fontSize: 10 }}
              />

              <RechartsTooltip
                content={({ active, payload, label }) => {
                  if (active && payload && payload.length) {
                    const point = payload[0].payload as SpendRiskTrendPoint;
                    return (
                      <div className="p-3.5 bg-surface/95 backdrop-blur-md border border-subtle rounded-xl shadow-lg text-caption space-y-2 min-w-[210px]">
                        <div className="flex items-center justify-between border-b border-subtle pb-1.5">
                          <span className="font-semibold text-primary font-mono">{label}</span>
                          <span className="text-muted text-[11px]">{point.reportCount} {point.reportCount === 1 ? 'Report' : 'Reports'}</span>
                        </div>
                        <div className="flex items-center justify-between font-mono">
                          <span className="text-accent flex items-center gap-1 font-medium">
                            <DollarSign className="h-3 w-3" />
                            Total Spend:
                          </span>
                          <span className="font-bold text-primary">
                            ${point.totalSpend.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                          </span>
                        </div>
                        <div className="pt-1.5 border-t border-subtle/60 space-y-1 text-[11px] font-mono">
                          <div className="flex items-center justify-between text-rose-500">
                            <span>High Risk:</span>
                            <span className="font-bold">{point.highRisk}</span>
                          </div>
                          <div className="flex items-center justify-between text-amber-500">
                            <span>Medium Risk:</span>
                            <span className="font-bold">{point.mediumRisk}</span>
                          </div>
                          <div className="flex items-center justify-between text-emerald-500">
                            <span>Low Risk:</span>
                            <span className="font-bold">{point.lowRisk}</span>
                          </div>
                        </div>
                      </div>
                    );
                  }
                  return null;
                }}
              />

              {/* Stacked Risk Areas */}
              <Area
                yAxisId="count"
                type="monotone"
                dataKey="lowRisk"
                stackId="riskStack"
                stroke="#10b981"
                fill="url(#areaLowRisk)"
                name="Low Risk Reports"
              />
              <Area
                yAxisId="count"
                type="monotone"
                dataKey="mediumRisk"
                stackId="riskStack"
                stroke="#f59e0b"
                fill="url(#areaMedRisk)"
                name="Med Risk Reports"
              />
              <Area
                yAxisId="count"
                type="monotone"
                dataKey="highRisk"
                stackId="riskStack"
                stroke="#ef4444"
                fill="url(#areaHighRisk)"
                name="High Risk Reports"
              />

              {/* Primary Spend Line Layer */}
              <Line
                yAxisId="spend"
                type="monotone"
                dataKey="totalSpend"
                stroke="url(#spendLineGlow)"
                strokeWidth={3}
                dot={{ r: 3, fill: '#6366f1', strokeWidth: 1, stroke: '#ffffff' }}
                activeDot={{ r: 6, fill: '#6366f1', stroke: '#ffffff', strokeWidth: 2 }}
                name="Spend Audited ($)"
              />

              <Legend
                verticalAlign="bottom"
                height={32}
                iconType="circle"
                wrapperStyle={{ fontSize: '11px', paddingTop: '10px' }}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      )}

      <div className="mt-4 pt-3 border-t border-subtle flex items-center justify-between text-[11px] text-muted font-mono">
        <span className="flex items-center gap-1.5">
          <Layers className="h-3 w-3 text-accent" />
          Single-aggregation time-series bucket
        </span>
        <span>Recharts dual-axis composed layout</span>
      </div>
    </Card>
  );
});

SpendRiskTrendChart.displayName = 'SpendRiskTrendChart';
export default SpendRiskTrendChart;
