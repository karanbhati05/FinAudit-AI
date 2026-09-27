import React from 'react';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, className = '', id, ...props }, ref) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label htmlFor={inputId} className="block text-caption font-medium text-secondary">
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={`w-full px-3.5 py-2.5 bg-surface text-primary placeholder:text-muted border border-subtle rounded-lg text-body outline-none transition-all duration-150 ease-out focus:border-accent focus:ring-2 focus:ring-accent/20 disabled:opacity-50 disabled:bg-surface-subtle ${
            error ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20' : ''
          } ${className}`}
          {...props}
        />
        {hint && !error && <p className="text-caption text-muted">{hint}</p>}
        {error && <p className="text-caption text-red-500 font-medium">{error}</p>}
      </div>
    );
  }
);

Input.displayName = 'Input';
