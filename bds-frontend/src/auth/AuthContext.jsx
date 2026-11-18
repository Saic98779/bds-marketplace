import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, session } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [state, setState] = useState(() => session.get());

  useEffect(() => {
    const sync = () => setState(session.get());
    window.addEventListener('bds-session', sync);
    return () => window.removeEventListener('bds-session', sync);
  }, []);

  const establish = useCallback((tokens) => {
    session.set({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken, user: tokens.user });
  }, []);

  const refreshUser = useCallback(async () => {
    const current = session.get();
    if (!current) return null;
    const user = await api.get('/auth/me');
    session.set({ ...current, user });
    return user;
  }, []);

  const reissue = useCallback(async () => {
    const current = session.get();
    if (!current?.refreshToken) return;
    const { data } = await api.post('/auth/refresh', { refreshToken: current.refreshToken });
    session.set({ accessToken: data.accessToken, refreshToken: data.refreshToken, user: data.user });
  }, []);

  const logout = useCallback(async () => {
    const current = session.get();
    if (current?.refreshToken) {
      try {
        await api.post('/auth/logout', { refreshToken: current.refreshToken });
      } catch {
        /* ignore */
      }
    }
    session.set(null);
  }, []);

  const value = useMemo(() => {
    const user = state?.user || null;
    const permissions = new Set(user?.permissions || []);
    return {
      user,
      isAuthenticated: !!user,
      role: user?.role,
      can: (p) => permissions.has(p),
      active: user?.status === 'ACTIVE',
      establish,
      refreshUser,
      reissue,
      logout
    };
  }, [state, establish, refreshUser, reissue, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}

export function homeFor(role) {
  if (role === 'ADMIN') return '/admin/dashboard';
  if (role === 'SELLER') return '/seller/dashboard';
  if (role === 'BUYER') return '/buyer/dashboard';
  return '/';
}

export function portalPrefix(role) {
  return role === 'ADMIN' ? '/admin' : role === 'SELLER' ? '/seller' : '/buyer';
}

export function resolveLink(link, role) {
  if (!link) return null;
  if (/^\/(seller|buyer|admin)\//.test(link)) return link;
  return `${portalPrefix(role)}${link}`;
}
