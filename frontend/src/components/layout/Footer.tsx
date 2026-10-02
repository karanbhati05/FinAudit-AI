import React from 'react';
import { ShieldCheck } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="border-t border-subtle bg-surface/50 transition-colors duration-200 py-12 mt-auto">
      <div className="max-w-6xl mx-auto px-6 flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="flex items-center gap-2 text-secondary">
          <ShieldCheck className="h-4 w-4 text-accent" />
          <span className="text-body font-medium text-primary">FinAudit-AI</span>
          <span className="text-caption text-muted ml-2">
            Autonomous Financial Compliance & Semantic Audit System
          </span>
        </div>
        <div className="text-caption text-muted flex items-center gap-6">
          <span>Spring AI + Gemini 3.5 Flash Lite + pgvector</span>
          <span>&copy; {new Date().getFullYear()} FinAudit Labs</span>
        </div>
      </div>
    </footer>
  );
};
