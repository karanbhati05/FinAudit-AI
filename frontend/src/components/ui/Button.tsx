import React from 'react';
import { motion, useReducedMotion } from 'framer-motion';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  className = '',
  disabled,
  ...props
}) => {
  const shouldReduceMotion = useReducedMotion();

  const baseClasses =
    'inline-flex items-center justify-center font-medium rounded-lg transition-colors duration-150 ease-out focus:outline-none focus:ring-2 focus:ring-accent/40 disabled:opacity-50 disabled:cursor-not-allowed select-none cursor-pointer';

  const sizeClasses = {
    sm: 'text-caption px-3 py-1.5 gap-1.5 h-8',
    md: 'text-body px-4 py-2 gap-2 h-10',
    lg: 'text-body px-6 py-2.5 gap-2.5 h-12 text-[16px]',
  };

  const variantClasses = {
    primary:
      'bg-accent text-white hover:opacity-95 shadow-sm',
    secondary:
      'bg-surface-subtle text-primary border border-subtle hover:bg-surface-hover hover:border-strong',
    ghost:
      'bg-transparent text-secondary hover:text-primary hover:bg-surface-subtle',
    danger:
      'bg-red-600/10 text-red-600 border border-red-600/20 hover:bg-red-600/15 dark:bg-red-950/40 dark:text-red-400 dark:border-red-900/40',
  };

  const isDisabled = disabled || isLoading;

  return (
    <motion.button
      whileTap={isDisabled || shouldReduceMotion ? undefined : { scale: 0.97 }}
      transition={{ duration: 0.1, ease: 'easeOut' }}
      className={`${baseClasses} ${sizeClasses[size]} ${variantClasses[variant]} ${className}`}
      disabled={isDisabled}
      {...(props as any)}
    >
      {isLoading ? (
        <span className="flex items-center gap-2">
          <span className="h-4 w-4 rounded-full border-2 border-current border-t-transparent animate-spin" />
          <span>{children}</span>
        </span>
      ) : (
        children
      )}
    </motion.button>
  );
};
