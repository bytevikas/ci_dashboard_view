import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { api, UserInfo } from '../api/client';

type AuthContextType = {
  user: UserInfo | null;
  loading: boolean;
  setToken: (token: string | null) => void;
  logout: () => void;
  refreshUser: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [loading, setLoading] = useState(!!sessionStorage.getItem('token'));

  const refreshUser = useCallback(async () => {
    const token = sessionStorage.getItem('token');
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const { data, status } = await api<UserInfo>('/auth/me', { timeout: 5000 });
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

  // Stop loading after 6s if /auth/me never completes (e.g. backend down)
  useEffect(() => {
    if (!loading) return;
    const t = setTimeout(() => {
      sessionStorage.removeItem('token');
      setUser(null);
      setLoading(false);
    }, 6000);
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
