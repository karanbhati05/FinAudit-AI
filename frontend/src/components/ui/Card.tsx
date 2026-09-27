import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  hoverLift?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg' | 'xl';
}

export const Card: React.FC<CardProps> = ({
  children,
  hoverLift = false,
  padding = 'lg',
  className = '',
  ...props
}) => {
  const paddingClasses = {
    none: '',
    sm: 'p-4',
    md: 'p-6',
    lg: 'p-8',
    xl: 'p-10 md:p-12',
  };

  const liftClasses = hoverLift
    ? 'hover:-translate-y-0.5 hover:border-strong transition-all duration-200 ease-out cursor-pointer'
    : 'transition-colors duration-200';

  return (
    <div
      className={`bg-surface border border-subtle rounded-xl ${paddingClasses[padding]} ${liftClasses} ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};
