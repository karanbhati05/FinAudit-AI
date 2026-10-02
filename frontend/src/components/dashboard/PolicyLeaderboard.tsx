import React, { useMemo } from 'react';
import { Card } from '../ui/Card';
import { ShieldAlert, Award, CheckCircle2, BookOpen } from 'lucide-react';

interface PolicyViolation {
  policyReference: string;
  count: number;
}

interface PolicyLeaderboardProps {
  violations: PolicyViolation[];
}

export const PolicyLeaderboard: React.FC<PolicyLeaderboardProps> = React.memo(({ violations = [] }) => {
  const maxCount = useMemo(() => {
    if (!violations.length) return 1;
    return Math.max(...violations.map((v) => v.count), 1);
  }, [violations]);

  const totalViolations = useMemo(() => {
    return violations.reduce((acc, curr) => acc + curr.count, 0);
  }, [violations]);

  return (
    <Card padding="lg" className="border-subtle bg-surface shadow-xs flex flex-col justify-between h-full">
      <div>
        <div className="flex items-start justify-between gap-4 mb-6">
          <div>
            <div className="flex items-center gap-2">
              <span className="p-1.5 rounded-lg bg-rose-500/10 text-rose-500 border border-rose-500/20">
                <ShieldAlert className="h-4 w-4" />
              </span>
              <h2 className="text-subhead font-bold text-primary">Top Flagged Policies Leaderboard</h2>
            </div>
            <p className="text-caption text-secondary mt-1">
              Corporate spending clauses triggering the most RAG auditor compliance violations
            </p>
          </div>

          <div className="px-3 py-1 rounded-xl bg-accent-subtle border border-accent/20 text-accent font-mono text-xs font-semibold shrink-0">
            {totalViolations} {totalViolations === 1 ? 'Violation' : 'Violations'}
          </div>
        </div>

        {violations.length === 0 ? (
          <div className="p-8 rounded-xl border border-dashed border-subtle bg-surface-subtle/30 text-center flex flex-col items-center justify-center">
            <CheckCircle2 className="h-8 w-8 text-emerald-500 mb-2" />
            <h3 className="text-body font-semibold text-primary">No Policy Violations Flagged</h3>
            <p className="text-caption text-secondary mt-1 max-w-sm">
              All line items and expense submissions in this date range conform strictly to governing guidelines.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {violations.map((item, idx) => {
              const percentage = Math.min(Math.round((item.count / maxCount) * 100), 100);
              const rank = idx + 1;

              // Distinct badge for top 3
              const rankBadge =
                rank === 1
                  ? 'bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30'
                  : rank === 2
                  ? 'bg-slate-300/20 text-slate-700 dark:text-slate-300 border-slate-400/30'
                  : rank === 3
                  ? 'bg-amber-700/15 text-amber-700 dark:text-amber-300 border-amber-700/30'
                  : 'bg-surface-subtle text-secondary border-subtle';

              return (
                <div
                  key={idx}
                  className="p-3.5 rounded-xl bg-surface-subtle/60 hover:bg-surface-subtle transition-all border border-subtle group"
                >
                  <div className="flex items-center justify-between gap-3 mb-2">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span
                        className={`w-6 h-6 rounded-md flex items-center justify-center text-xs font-mono font-bold border shrink-0 ${rankBadge}`}
                      >
                        {rank === 1 ? <Award className="h-3.5 w-3.5" /> : `#${rank}`}
                      </span>
                      <span
                        className="text-body font-medium text-primary truncate group-hover:text-accent transition-colors"
                        title={item.policyReference}
                      >
                        {item.policyReference}
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      <span className="text-caption font-mono font-bold text-primary px-2 py-0.5 rounded-md bg-surface border border-subtle">
                        {item.count} {item.count === 1 ? 'flag' : 'flags'}
                      </span>
                    </div>
                  </div>

                  {/* Horizontal Proportional Progress Bar */}
                  <div className="w-full bg-surface h-2 rounded-full overflow-hidden border border-subtle/50">
                    <div
                      className="h-full rounded-full transition-all duration-700 ease-out bg-gradient-to-r from-accent to-rose-500"
                      style={{ width: `${percentage}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <div className="mt-6 pt-4 border-t border-subtle flex items-center justify-between text-[11px] text-muted font-mono">
        <span className="flex items-center gap-1.5">
          <BookOpen className="h-3.5 w-3.5 text-accent" />
          Indexed Corporate Policy Knowledge Base
        </span>
        <span>RAG Vector Grounding</span>
      </div>
    </Card>
  );
});

PolicyLeaderboard.displayName = 'PolicyLeaderboard';
export default PolicyLeaderboard;
