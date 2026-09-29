import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../ui/Button';
import { Sun, Moon, ShieldCheck } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { resolvedTheme, toggleTheme } = useTheme();
  const { isAuthenticated, user, logout, loginDemo } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const isActive = (path: string) => location.pathname === path;

  return (
    <header className="sticky top-0 z-40 w-full border-b border-subtle bg-surface/80 backdrop-blur-md transition-colors duration-200">
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        {/* Brand */}
        <Link to="/" className="flex items-center gap-2.5 group">
          <div className="h-8 w-8 rounded-lg bg-accent text-white flex items-center justify-center shadow-sm">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <span className="text-subhead font-semibold tracking-tight text-primary">
            FinAudit<span className="text-accent font-mono text-caption ml-1 font-semibold">AI</span>
          </span>
        </Link>

        {/* Center Nav */}
        <nav className="hidden md:flex items-center gap-6">
          <Link
            to="/"
            className={`text-body transition-colors ${
              isActive('/') ? 'text-primary font-medium' : 'text-secondary hover:text-primary'
            }`}
          >
            Overview
          </Link>
          <Link
            to="/upload"
            className={`text-body transition-colors ${
              isActive('/upload') ? 'text-primary font-medium' : 'text-secondary hover:text-primary'
            }`}
          >
            Upload
          </Link>
          <Link
            to="/dashboard"
            className={`text-body transition-colors ${
              isActive('/dashboard') ? 'text-primary font-medium' : 'text-secondary hover:text-primary'
            }`}
          >
            Dashboard
          </Link>
        </nav>

        {/* Right Actions */}
        <div className="flex items-center gap-3">
          {/* Theme Toggle */}
          <button
            onClick={toggleTheme}
            aria-label="Toggle theme"
            className="h-9 w-9 rounded-lg border border-subtle bg-surface-subtle flex items-center justify-center text-secondary hover:text-primary hover:bg-surface-hover transition-colors cursor-pointer"
          >
            {resolvedTheme === 'dark' ? (
              <Sun className="h-4 w-4" />
            ) : (
              <Moon className="h-4 w-4" />
            )}
          </button>

          {isAuthenticated ? (
            <div className="flex items-center gap-3">
              <span className="hidden sm:inline text-caption text-secondary font-mono">
                {user?.email}
              </span>
              <Button variant="secondary" size="sm" onClick={logout}>
                Sign Out
              </Button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={async () => {
                  await loginDemo('AUDITOR');
                  navigate('/dashboard');
                }}
                className="hidden sm:inline-flex items-center text-caption font-medium px-3 py-1.5 rounded-lg bg-accent/10 text-accent hover:bg-accent/20 transition-colors cursor-pointer"
              >
                <span>View Demo</span>
              </button>
              <Link to="/login">
                <Button variant="ghost" size="sm">
                  Sign In
                </Button>
              </Link>
              <Link to="/register">
                <Button variant="primary" size="sm">
                  Get Started
                </Button>
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
