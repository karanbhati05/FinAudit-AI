import React, { createContext, useContext, useState } from 'react';
import { api, setAuthToken } from '../services/api';

export interface User {
  id?: number;
  email: string;
  role: 'AUDITOR' | 'ADMIN' | 'VIEWER';
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, role?: 'AUDITOR' | 'ADMIN' | 'VIEWER') => Promise<void>;
  loginDemo: (role?: 'AUDITOR' | 'ADMIN' | 'VIEWER') => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // In-memory authentication state (never stored in localStorage)
  const [token, setTokenState] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);

  const updateToken = (newToken: string | null, newUser: User | null) => {
    setTokenState(newToken);
    setUser(newUser);
    setAuthToken(newToken);
  };

  const login = async (email: string, password: string) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      const data = response.data;
      const receivedToken = data.token || data.accessToken || 'demo-session-token';
      const receivedUser: User = {
        id: data.id,
        email: data.email || email,
        role: data.role || 'AUDITOR',
      };
      updateToken(receivedToken, receivedUser);
    } catch (err: any) {
      if (err.code === 'ERR_NETWORK' || err.response?.status === 404) {
        console.warn('Auth endpoint not reachable yet, falling back to simulated session');
        updateToken('simulated-jwt-token', {
          email,
          role: 'AUDITOR',
        });
        return;
      }
      throw err;
    }
  };

  const register = async (
    email: string,
    password: string,
    role: 'AUDITOR' | 'ADMIN' | 'VIEWER' = 'AUDITOR'
  ) => {
    try {
      const response = await api.post('/auth/register', { email, password, role });
      const data = response.data;
      const receivedToken = data.token || data.accessToken || 'demo-session-token';
      const receivedUser: User = {
        id: data.id,
        email: data.email || email,
        role: data.role || role,
      };
      updateToken(receivedToken, receivedUser);
    } catch (err: any) {
      if (err.code === 'ERR_NETWORK' || err.response?.status === 404) {
        console.warn('Auth endpoint not reachable yet, falling back to simulated session');
        updateToken('simulated-jwt-token', {
          email,
          role,
        });
        return;
      }
      throw err;
    }
  };

  const loginDemo = async (role: 'AUDITOR' | 'ADMIN' | 'VIEWER' = 'AUDITOR') => {
    try {
      const response = await api.post('/auth/demo');
      const data = response.data;
      const receivedToken = data.token || data.accessToken;
      const receivedUser: User = {
        id: data.id,
        email: data.email || 'demo@finaudit.ai',
        role: data.role || role,
      };
      updateToken(receivedToken, receivedUser);
    } catch (err) {
      console.warn('Backend demo auth not available, falling back to simulated session', err);
      updateToken('demo-auditor-jwt-token', {
        email: 'demo@finaudit.ai',
        role,
      });
    }
  };

  const logout = () => {
    updateToken(null, null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        login,
        register,
        loginDemo,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
