import React from 'react';
import { motion, useReducedMotion } from 'framer-motion';

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
  const shouldReduceMotion = useReducedMotion();

  const paddingClasses = {
    none: '',
    sm: 'p-4',
    md: 'p-6',
    lg: 'p-8',
    xl: 'p-10 md:p-12',
  };

  const liftClasses = hoverLift
    ? 'hover:border-strong hover:shadow-md transition-shadow duration-200 ease-out'
    : 'transition-colors duration-200';

  return (
    <motion.div
      whileHover={hoverLift && !shouldReduceMotion ? { y: -2 } : undefined}
      transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
      className={`bg-surface border border-subtle rounded-xl ${paddingClasses[padding]} ${liftClasses} ${className}`}
      {...(props as any)}
    >
      {children}
    </motion.div>
  );
};
