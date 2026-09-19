import axios, { AxiosError } from 'axios';
import type { ProblemDetail } from './types';

/**
 * O token nao passa mais pelo JavaScript: ele chega num cookie HttpOnly que o navegador guarda e
 * reenvia sozinho. Por isso `withCredentials` e nenhuma funcao de ler ou salvar token por aqui.
 * Como o frontend e servido pela propria API (e o Vite faz proxy de /api em desenvolvimento),
 * tudo e mesma origem e o cookie viaja normalmente.
 */
export const api = axios.create({ baseURL: '/api', withCredentials: true });

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ProblemDetail>) => {
    // Token vencido ou invalido: volta para o login. As rotas /auth/ ficam de fora porque o 401 do
    // /auth/me na abertura do site e esperado: e ele que decide entre modo demo e tela de login.
    if (error.response?.status === 401 && !error.config?.url?.startsWith('/auth/')) {
      if (window.location.pathname !== '/login') window.location.assign('/login');
    }
    return Promise.reject(error);
  },
);

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
