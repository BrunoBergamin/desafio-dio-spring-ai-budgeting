import { api } from './client';
import type {
  AssistantResponse, AuthResponse, BudgetStatusResponse, Category, ExpenseCategory, PageResponse,
  GoalRequest, MonthlyReport, RecurringRequest, RecurringResponse, SavingsGoalResponse, SpendingSummary,
  TransactionRequest, TransactionResponse,
  TransactionType, UserResponse, WhatsAppConnection,
} from './types';

export interface TransactionFilters {
  type?: TransactionType;
  category?: Category;
  start?: string;
  end?: string;
  page?: number;
  size?: number;
}

export const authApi = {
  register: (name: string, email: string, password: string) =>
    api.post<AuthResponse>('/auth/register', { name, email, password }).then((r) => r.data),
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data),
  me: () => api.get<UserResponse>('/auth/me').then((r) => r.data),
  /** Entra na conta de demonstracao sem senha (404 quando o modo demo esta desligado). */
  demo: () => api.post<AuthResponse>('/auth/demo').then((r) => r.data),
  linkPhone: (phone: string) => api.put<UserResponse>('/auth/me/phone', { phone }).then((r) => r.data),
};

export const whatsappApi = {
  status: () => api.get<WhatsAppConnection>('/whatsapp/status').then((r) => r.data),
  connect: () => api.post<WhatsAppConnection>('/whatsapp/connect').then((r) => r.data),
};

export const transactionsApi = {
  /** Paginado (50 por pagina por padrao, no maximo 500), dos mais recentes para os mais antigos. */
  list: (params?: TransactionFilters) =>
    api.get<PageResponse<TransactionResponse>>('/transactions', { params }).then((r) => r.data),
  summary: (params?: { start?: string; end?: string }) =>
    api.get<SpendingSummary>('/transactions/summary', { params }).then((r) => r.data),
  create: (body: TransactionRequest) =>
    api.post<TransactionResponse>('/transactions', body).then((r) => r.data),
  update: (id: string, body: TransactionRequest) =>
    api.put<TransactionResponse>(`/transactions/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/transactions/${id}`),

  /** Baixa o CSV do periodo. Vem como blob para o navegador salvar como arquivo. */
  exportCsv: (params?: TransactionFilters) =>
    api.get<Blob>('/transactions/export.csv', { params, responseType: 'blob' }).then((r) => r.data),
};

export const recurringApi = {
  list: () => api.get<RecurringResponse[]>('/recurring').then((r) => r.data),
  create: (body: RecurringRequest) => api.post<RecurringResponse>('/recurring', body).then((r) => r.data),
  update: (id: string, body: RecurringRequest) =>
    api.put<RecurringResponse>(`/recurring/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/recurring/${id}`),
};

export const budgetsApi = {
  list: (month?: string) =>
    api.get<BudgetStatusResponse[]>('/budgets', { params: month ? { month } : undefined }).then((r) => r.data),
  alerts: (month?: string) =>
    api.get<BudgetStatusResponse[]>('/budgets/alerts', { params: month ? { month } : undefined }).then((r) => r.data),
  create: (category: ExpenseCategory, monthlyLimit: number, month?: string) =>
    api.post<BudgetStatusResponse>('/budgets', { category, monthlyLimit, month }).then((r) => r.data),
  update: (id: string, monthlyLimit: number) =>
    api.put<BudgetStatusResponse>(`/budgets/${id}`, { monthlyLimit }).then((r) => r.data),
  remove: (id: string) => api.delete(`/budgets/${id}`),
};

export const goalsApi = {
  list: () => api.get<SavingsGoalResponse[]>('/goals').then((r) => r.data),
  create: (body: GoalRequest) => api.post<SavingsGoalResponse>('/goals', body).then((r) => r.data),
  update: (id: string, body: GoalRequest) => api.put<SavingsGoalResponse>(`/goals/${id}`, body).then((r) => r.data),
  deposit: (id: string, amount: number) =>
    api.post<SavingsGoalResponse>(`/goals/${id}/deposits`, { amount }).then((r) => r.data),
  remove: (id: string) => api.delete(`/goals/${id}`),
};

export const reportsApi = {
  monthly: (month?: string) =>
    api.get<MonthlyReport>('/reports/monthly', { params: month ? { month } : undefined }).then((r) => r.data),
};

export const assistantApi = {
  chat: (message: string, conversationId: string) =>
    api.post<AssistantResponse>('/assistant/chat', { message, conversationId }).then((r) => r.data),

  /** Audio -> transcricao + resposta em texto. Nao setar Content-Type: o navegador poe o boundary. */
  voiceToText: (blob: Blob, filename: string, conversationId: string) => {
    const form = new FormData();
    form.append('file', blob, filename);
    return api
      .post<AssistantResponse>('/assistant/voice/text', form, { params: { conversationId } })
      .then((r) => r.data);
  },

  /** Audio -> MP3 com a resposta falada. Responde 503 no perfil groq (sem text-to-speech). */
  voiceToVoice: (blob: Blob, filename: string, conversationId: string) => {
    const form = new FormData();
    form.append('file', blob, filename);
    return api
      .post<Blob>('/assistant/voice', form, { params: { conversationId }, responseType: 'blob' })
      .then((r) => r.data);
  },

  forget: (conversationId: string) => api.delete('/assistant/conversation', { params: { conversationId } }),
};
