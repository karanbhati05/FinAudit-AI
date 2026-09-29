import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './queryClient';
import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/layout/Navbar';
import { Footer } from './components/layout/Footer';
import { RouteLoadingFallback } from './components/ui/RouteLoadingFallback';

// Route-based code splitting (React.lazy)
const LandingPage = React.lazy(() =>
  import('./pages/LandingPage').then((m) => ({ default: m.LandingPage }))
);
const LoginPage = React.lazy(() =>
  import('./pages/LoginPage').then((m) => ({ default: m.LoginPage }))
);
const RegisterPage = React.lazy(() =>
  import('./pages/RegisterPage').then((m) => ({ default: m.RegisterPage }))
);
const UploadPage = React.lazy(() =>
  import('./pages/UploadPage').then((m) => ({ default: m.UploadPage }))
);
const DashboardPage = React.lazy(() =>
  import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage }))
);
const ReportDetailPage = React.lazy(() =>
  import('./pages/ReportDetailPage').then((m) => ({ default: m.ReportDetailPage }))
);
const PublicReportPage = React.lazy(() =>
  import('./pages/PublicReportPage').then((m) => ({ default: m.PublicReportPage }))
);

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          <BrowserRouter>
            <div className="flex flex-col min-h-screen bg-page text-primary transition-colors duration-200">
              <Navbar />
              <main className="flex-1 flex flex-col">
                <Suspense fallback={<RouteLoadingFallback />}>
                  <Routes>
                    <Route path="/" element={<LandingPage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />
                    <Route path="/upload" element={<UploadPage />} />
                    <Route path="/dashboard" element={<DashboardPage />} />
                    <Route path="/reports/:id" element={<ReportDetailPage />} />
                    <Route path="/share/:token" element={<PublicReportPage />} />
                    <Route path="*" element={<Navigate to="/" replace />} />
                  </Routes>
                </Suspense>
              </main>
              <Footer />
            </div>
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

export default App;
