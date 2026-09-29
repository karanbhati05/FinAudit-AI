import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { ShieldCheck, ArrowRight } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'AUDITOR' | 'VIEWER'>('AUDITOR');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const { register, loginDemo } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please provide email and password.');
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      await register(email, password, role);
      navigate('/dashboard');
    } catch (err: any) {
      setError(
        err.response?.data?.message || 'Registration failed. Email might already be taken.'
      );
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
          <h1 className="text-title font-semibold text-primary">Create an Account</h1>
          <p className="text-body text-secondary mt-1">
            Get started with autonomous financial compliance auditing
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
              placeholder="name@company.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
            />

            <Input
              label="Password"
              type="password"
              placeholder="Minimum 8 characters"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="new-password"
            />

            <div className="space-y-1.5">
              <label className="block text-caption font-medium text-secondary">
                Assigned Role
              </label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setRole('AUDITOR')}
                  className={`px-3 py-2 rounded-lg border text-caption font-medium transition-colors text-center cursor-pointer ${
                    role === 'AUDITOR'
                      ? 'border-accent bg-accent-subtle text-accent'
                      : 'border-subtle bg-surface-subtle text-secondary hover:text-primary'
                  }`}
                >
                  Auditor (Full Access)
                </button>
                <button
                  type="button"
                  onClick={() => setRole('VIEWER')}
                  className={`px-3 py-2 rounded-lg border text-caption font-medium transition-colors text-center cursor-pointer ${
                    role === 'VIEWER'
                      ? 'border-accent bg-accent-subtle text-accent'
                      : 'border-subtle bg-surface-subtle text-secondary hover:text-primary'
                  }`}
                >
                  Viewer (Read-Only)
                </button>
              </div>
            </div>

            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full mt-2"
              isLoading={isLoading}
            >
              <span>Create Account</span>
              <ArrowRight className="h-4 w-4 ml-2" />
            </Button>
          </form>

          <div className="relative my-6 text-center">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-subtle" />
            </div>
            <span className="relative px-3 bg-surface text-caption text-muted">
              or skip registration
            </span>
          </div>

          <Button
            type="button"
            variant="secondary"
            size="md"
            className="w-full text-caption"
            onClick={async () => {
              try {
                setIsLoading(true);
                setError(null);
                await loginDemo('AUDITOR');
                navigate('/dashboard');
              } catch (err: any) {
                setError(err?.response?.data?.message || 'Demo login failed.');
              } finally {
                setIsLoading(false);
              }
            }}
          >
            Launch Instant Demo Account (Zero Friction)
          </Button>

          <div className="text-center mt-6 pt-6 border-t border-subtle">
            <p className="text-caption text-secondary">
              Already have an account?{' '}
              <Link to="/login" className="text-accent font-medium hover:underline">
                Sign in
              </Link>
            </p>
          </div>
        </Card>
      </div>
    </div>
  );
};
