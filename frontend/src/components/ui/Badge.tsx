import React from 'react';

export type SeverityType = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type ReportStatusType = 'UPLOADED' | 'PARSING' | 'AUDITING' | 'COMPLETE' | 'FAILED';
export type RiskLevelType = 'LOW' | 'MEDIUM' | 'HIGH';

export interface BadgeProps {
  children?: React.ReactNode;
  severity?: SeverityType;
  status?: ReportStatusType;
  riskLevel?: RiskLevelType;
  variant?: 'accent' | 'neutral' | 'subtle';
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  severity,
  status,
  riskLevel,
  variant = 'neutral',
  className = '',
}) => {
  let styleClasses = 'bg-surface-subtle text-secondary border-subtle';

  if (severity) {
    switch (severity) {
      case 'CRITICAL':
        // High-contrast accent + bold neutral prominence
        styleClasses =
          'bg-accent text-white border-accent font-semibold tracking-wide';
        break;
      case 'HIGH':
        styleClasses =
          'bg-accent-subtle text-accent border border-accent/40 font-medium';
        break;
      case 'MEDIUM':
        styleClasses =
          'bg-surface-subtle text-primary border border-strong font-medium';
        break;
      case 'LOW':
        styleClasses =
          'bg-surface-subtle/60 text-muted border border-subtle font-normal';
        break;
    }
  } else if (riskLevel) {
    switch (riskLevel) {
      case 'HIGH':
        styleClasses = 'bg-accent text-white border-accent font-semibold';
        break;
      case 'MEDIUM':
        styleClasses = 'bg-accent-subtle text-accent border border-accent/30';
        break;
      case 'LOW':
        styleClasses = 'bg-surface-subtle text-secondary border border-subtle';
        break;
    }
  } else if (status) {
    switch (status) {
      case 'COMPLETE':
        styleClasses = 'bg-accent-subtle text-accent border border-accent/30';
        break;
      case 'AUDITING':
      case 'PARSING':
        styleClasses = 'bg-surface-subtle text-primary border border-accent/40 animate-pulse';
        break;
      case 'UPLOADED':
        styleClasses = 'bg-surface-subtle text-muted border border-subtle';
        break;
      case 'FAILED':
        styleClasses = 'bg-red-500/10 text-red-600 dark:text-red-400 border border-red-500/20';
        break;
    }
  } else {
    switch (variant) {
      case 'accent':
        styleClasses = 'bg-accent-subtle text-accent border border-accent/30';
        break;
      case 'neutral':
        styleClasses = 'bg-surface-subtle text-primary border border-subtle';
        break;
      case 'subtle':
        styleClasses = 'bg-transparent text-muted border border-subtle';
        break;
    }
  }

  const content = children || severity || status || riskLevel;

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-md text-caption uppercase tracking-wider border ${styleClasses} ${className}`}
    >
      {content}
    </span>
  );
};
