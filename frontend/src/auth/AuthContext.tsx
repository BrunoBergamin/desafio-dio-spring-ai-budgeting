import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../api/endpoints';
import { clearToken, readToken, saveToken } from '../api/client';
import type { AuthResponse, UserResponse } from '../api/types';

interface AuthState {
  user: UserResponse | null;
  loading: boolean;
  demo: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);
const DEMO_KEY = 'lumi.demo';

/** Le o "exp" do JWT sem biblioteca: e so o payload em base64url. */
function isExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [demo, setDemo] = useState(() => {
    try { return localStorage.getItem(DEMO_KEY) === '1'; } catch { return false; }
  });

  const accept = useCallback((auth: AuthResponse, isDemo: boolean) => {
    saveToken(auth.token);
    setUser(auth.user);
    setDemo(isDemo);
    try { localStorage.setItem(DEMO_KEY, isDemo ? '1' : '0'); } catch { /* ignore */ }
  }, []);

  useEffect(() => {
    const token = readToken();
    if (token && !isExpired(token)) {
      authApi.me().then(setUser).catch(() => clearToken()).finally(() => setLoading(false));
      return;
    }
    clearToken();
    // Sem token: tenta a conta de demonstracao. Se o servidor estiver com o modo demo desligado (404),
    // cai na tela de login normal.
    authApi.demo()
      .then((auth) => accept(auth, true))
      .catch(() => undefined)
      .finally(() => setLoading(false));
  }, [accept]);

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      demo,
      login: (email, password) => authApi.login(email, password).then((a) => accept(a, false)),
      register: (name, email, password) => authApi.register(name, email, password).then((a) => accept(a, false)),
      logout: () => {
        clearToken();
        setUser(null);
        setDemo(false);
        try { localStorage.setItem(DEMO_KEY, '0'); } catch { /* ignore */ }
      },
    }),
    [user, loading, demo, accept],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth precisa estar dentro de AuthProvider');
  return ctx;
}
