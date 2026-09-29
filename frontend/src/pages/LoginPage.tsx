import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { ShieldCheck, ArrowRight } from 'lucide-react';
import { prefetchDashboardData } from '../services/dashboardService';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const { login, loginDemo } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please provide both email and password.');
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      await login(email, password);
      // Prefetch dashboard route data immediately upon login success
      prefetchDashboardData();
      navigate('/dashboard');
    } catch (err: any) {
      setError(
        err.response?.data?.message || 'Authentication failed. Please verify credentials.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleDemoSignIn = async () => {
    try {
      setIsLoading(true);
      setError(null);
      await loginDemo('AUDITOR');
      // Prefetch dashboard route data immediately upon demo login success
      prefetchDashboardData();
      navigate('/dashboard');
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Demo login failed.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center px-6 py-16">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex h-10 w-10 rounded-xl bg-accent text-white items-center justify-center shadow-sm mb-4">
            <ShieldCheck className="h-6 w-6" />
          </div>
          <h1 className="text-title font-semibold text-primary">Sign in to FinAudit</h1>
          <p className="text-body text-secondary mt-1">
            Access your autonomous compliance audit workspace
          </p>
        </div>

        <Card padding="lg" className="border-subtle bg-surface shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="p-3.5 rounded-lg bg-red-500/10 border border-red-500/20 text-caption text-red-600 dark:text-red-400">
                {error}
              </div>
            )}

            <Input
              label="Work Email"
              type="email"
              placeholder="auditor@organization.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
            />

            <Input
              label="Password"
              type="password"
              placeholder="••••••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />

            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full mt-2"
              isLoading={isLoading}
            >
              <span>Sign In</span>
              <ArrowRight className="h-4 w-4 ml-2" />
            </Button>
          </form>

          <div className="relative my-6 text-center">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-subtle" />
            </div>
            <span className="relative px-3 bg-surface text-caption text-muted">
              portfolio demonstration
            </span>
          </div>

          <Button
            type="button"
            variant="secondary"
            size="md"
            className="w-full text-caption"
            onClick={handleDemoSignIn}
          >
            Sign in as Demo Auditor (1-Click)
          </Button>

          <div className="text-center mt-6 pt-6 border-t border-subtle">
            <p className="text-caption text-secondary">
              Don't have an account?{' '}
              <Link to="/register" className="text-accent font-medium hover:underline">
                Create one now
              </Link>
            </p>
          </div>
        </Card>
      </div>
    </div>
  );
};
