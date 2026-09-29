import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import * as Sentry from "@sentry/react";
import './index.css';
import App from './App.tsx';

const sentryDsn = import.meta.env.VITE_SENTRY_DSN;
if (sentryDsn) {
  Sentry.init({
    dsn: sentryDsn,
    environment: import.meta.env.MODE || 'production',
    tracesSampleRate: 1.0,
  });
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Sentry.ErrorBoundary fallback={
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white p-6">
        <div className="max-w-md text-center bg-slate-800 border border-slate-700 rounded-xl p-8 shadow-xl">
          <h2 className="text-xl font-bold text-rose-400 mb-2">Application Error Captured</h2>
          <p className="text-sm text-slate-300 mb-4">
            An unexpected error occurred. Observability telemetry has automatically dispatched an alert to our error tracking dashboard.
          </p>
          <a href="/" className="inline-block px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">
            Return to FinAudit AI
          </a>
        </div>
      </div>
    }>
      <App />
    </Sentry.ErrorBoundary>
  </StrictMode>,
);

