import React, { useMemo } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import { Card } from '../ui/Card';
import { Badge } from '../ui/Badge';
import type { DashboardSummary } from '../../services/dashboardService';

interface ChartSectionProps {
  summary: DashboardSummary;
}

export const AuditRiskChart: React.FC<ChartSectionProps> = React.memo(({ summary }) => {
  const chartData = useMemo(
    () => [
      {
        name: 'Low Risk',
        count: summary.countByRiskLevel.LOW || 0,
        barColor: '#94a3b8',
      },
      {
        name: 'Medium Risk',
        count: summary.countByRiskLevel.MEDIUM || 0,
        barColor: '#3b82f6',
      },
      {
        name: 'High Risk',
        count: summary.countByRiskLevel.HIGH || 0,
        barColor: '#ef4444',
      },
    ],
    [summary.countByRiskLevel]
  );

  return (
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
  );
});

AuditRiskChart.displayName = 'AuditRiskChart';

export default AuditRiskChart;
