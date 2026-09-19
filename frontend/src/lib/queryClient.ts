import { QueryClient } from '@tanstack/react-query';

/** Cache de dados: uma busca por chave, invalidada quando algo muda (gasto novo, orcamento, resposta da Lumi). */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export const keys = {
  me: ['me'] as const,
  transactions: (filters: object) => ['transactions', filters] as const,
  summary: (start?: string, end?: string) => ['summary', start ?? '', end ?? ''] as const,
  budgets: (month?: string) => ['budgets', month ?? ''] as const,
  alerts: (month?: string) => ['alerts', month ?? ''] as const,
  recurring: ['recurring'] as const,
  whatsapp: ['whatsapp'] as const,
};

/** Depois de qualquer mudanca financeira, tudo que mostra numero e recarregado. */
export function invalidateFinancial() {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['transactions'] }),
    queryClient.invalidateQueries({ queryKey: ['summary'] }),
    queryClient.invalidateQueries({ queryKey: ['budgets'] }),
    queryClient.invalidateQueries({ queryKey: ['alerts'] }),
    // Criar uma conta fixa pode ja gerar o lancamento do mes, entao a lista tambem muda
    queryClient.invalidateQueries({ queryKey: ['recurring'] }),
  ]);
}
