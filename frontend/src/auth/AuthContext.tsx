import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../api/endpoints';
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
/** Só marca que a sessão veio do modo demo, para a interface avisar. Não é dado sensível. */
const DEMO_KEY = 'lumi.demo';

/**
 * Quem está logado é decidido pelo servidor: o token vive num cookie HttpOnly que o JavaScript não lê.
 * Então a abertura do site pergunta "quem sou eu?" em /auth/me e, se ninguém responder, tenta a conta
 * de demonstração. Sem o cookie e sem demo, cai na tela de login.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [demo, setDemo] = useState(() => {
    try { return localStorage.getItem(DEMO_KEY) === '1'; } catch { return false; }
  });

  const accept = useCallback((auth: AuthResponse, isDemo: boolean) => {
    setUser(auth.user);
    setDemo(isDemo);
    try { localStorage.setItem(DEMO_KEY, isDemo ? '1' : '0'); } catch { /* ignore */ }
  }, []);

  useEffect(() => {
    let cancelled = false;
    authApi.me()
      .then((me) => { if (!cancelled) setUser(me); })
      // Sem sessão: tenta a conta de demonstração. Com o modo demo desligado (404), fica no login.
      .catch(() => authApi.demo()
        .then((auth) => { if (!cancelled) accept(auth, true); })
        .catch(() => undefined))
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [accept]);

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      demo,
      login: (email, password) => authApi.login(email, password).then((a) => accept(a, false)),
      register: (name, email, password) => authApi.register(name, email, password).then((a) => accept(a, false)),
      logout: () => {
        // Só o servidor apaga o cookie, porque o JavaScript não enxerga esse cookie
        authApi.logout().catch(() => undefined).finally(() => {
          setUser(null);
          setDemo(false);
          try { localStorage.setItem(DEMO_KEY, '0'); } catch { /* ignore */ }
        });
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
