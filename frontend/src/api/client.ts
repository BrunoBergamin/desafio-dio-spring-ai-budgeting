import axios, { AxiosError } from 'axios';
import type { ProblemDetail } from './types';

export const TOKEN_KEY = 'lumi.token';

export const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
  const token = readToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ProblemDetail>) => {
    // Token vencido ou invalido: volta para o login (exceto na propria tela de login)
    if (error.response?.status === 401 && !error.config?.url?.startsWith('/auth/')) {
      clearToken();
      if (window.location.pathname !== '/login') window.location.assign('/login');
    }
    return Promise.reject(error);
  },
);

export function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function saveToken(token: string) {
  try {
    localStorage.setItem(TOKEN_KEY, token);
  } catch {
    /* modo privado: segue sem persistir */
  }
}

export function clearToken() {
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* ignore */
  }
}

/** Extrai uma mensagem legivel de um erro da API (ProblemDetail) ou de rede. */
export function errorMessage(error: unknown, fallback = 'Algo deu errado. Tente de novo.'): string {
  if (axios.isAxiosError<ProblemDetail>(error)) {
    const data = error.response?.data;
    if (data?.errors) return Object.values(data.errors).join('; ');
    if (data?.detail) return data.detail;
    if (data?.title) return data.title;
    if (!error.response) return 'Não foi possível falar com o servidor. Ele está rodando?';
  }
  return fallback;
}
