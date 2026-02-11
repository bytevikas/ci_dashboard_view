import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, UserInfo } from '../api/client';

type AuthContextType = {
  user: UserInfo | null;
  loading: boolean;
  setToken: (token: string | null) => void;
  logout: () => void;
  refreshUser: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(null);
  // Start false so login shows immediately when there's no token; only true while checking /auth/me
  const [loading, setLoading] = useState(false);

  const refreshUser = useCallback(async () => {
    const token = sessionStorage.getItem('token');
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const { data, status } = await api<UserInfo>('/auth/me', { timeout: 8000 });
      if (status === 401 || !data) {
        sessionStorage.removeItem('token');
        setUser(null);
      } else {
        setUser(data);
      }
    } catch {
      sessionStorage.removeItem('token');
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  const setToken = useCallback((token: string | null) => {
    if (token) sessionStorage.setItem('token', token);
    else sessionStorage.removeItem('token');
    setLoading(true);
    if (token) {
      refreshUser();
    } else {
      setUser(null);
      setLoading(false);
    }
  }, [refreshUser]);

  useEffect(() => {
    const token = sessionStorage.getItem('token');
    if (!token) return;
    setLoading(true);
    refreshUser();
  }, [refreshUser]);

  // Safety: if still loading after 10s, clear and show login (backend down or fetch broken)
  useEffect(() => {
    if (!loading) return;
    const t = setTimeout(() => {
      sessionStorage.removeItem('token');
      setUser(null);
      setLoading(false);
    }, 10000);
    return () => clearTimeout(t);
  }, [loading]);

  const logout = useCallback(() => {
    sessionStorage.removeItem('token');
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, setToken, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
