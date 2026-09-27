import React from 'react';

export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  className?: string;
}

export const Skeleton: React.FC<SkeletonProps> = ({ className = '', ...props }) => {
  return (
    <div
      className={`bg-surface-subtle border border-subtle/50 rounded-lg animate-shimmer ${className}`}
      {...props}
    />
  );
};
