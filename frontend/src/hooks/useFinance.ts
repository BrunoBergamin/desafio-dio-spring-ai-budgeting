import { useMutation, useQuery } from '@tanstack/react-query';
import { budgetsApi, goalsApi, recurringApi, reportsApi, transactionsApi, type TransactionFilters } from '../api/endpoints';
import type { ExpenseCategory, GoalRequest, RecurringRequest, TransactionRequest } from '../api/types';
import { invalidateFinancial, keys } from '../lib/queryClient';

/** Hooks de dados: cada tela declara o que precisa; o React Query cuida de cache, loading e refetch. */

export function useTransactions(filters: TransactionFilters) {
  return useQuery({
    queryKey: keys.transactions(filters),
    queryFn: () => transactionsApi.list(filters),
    placeholderData: (previous) => previous, // ao trocar de pagina a tabela nao pisca
  });
}

export function useSummary(start?: string, end?: string) {
  return useQuery({ queryKey: keys.summary(start, end), queryFn: () => transactionsApi.summary({ start, end }) });
}

export function useBudgets(month?: string) {
  return useQuery({ queryKey: keys.budgets(month), queryFn: () => budgetsApi.list(month) });
}

export function useAlerts(month?: string) {
  return useQuery({ queryKey: keys.alerts(month), queryFn: () => budgetsApi.alerts(month) });
}

export function useCreateTransaction() {
  return useMutation({ mutationFn: (body: TransactionRequest) => transactionsApi.create(body), onSuccess: invalidateFinancial });
}

export function useUpdateTransaction() {
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: TransactionRequest }) => transactionsApi.update(id, body),
    onSuccess: invalidateFinancial,
  });
}

export function useDeleteTransaction() {
  return useMutation({ mutationFn: (id: string) => transactionsApi.remove(id), onSuccess: invalidateFinancial });
}

export function useRecurring() {
  return useQuery({ queryKey: keys.recurring, queryFn: recurringApi.list });
}

export function useCreateRecurring() {
  return useMutation({ mutationFn: (body: RecurringRequest) => recurringApi.create(body), onSuccess: invalidateFinancial });
}

export function useUpdateRecurring() {
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: RecurringRequest }) => recurringApi.update(id, body),
    onSuccess: invalidateFinancial,
  });
}

export function useDeleteRecurring() {
  return useMutation({ mutationFn: (id: string) => recurringApi.remove(id), onSuccess: invalidateFinancial });
}

export function useMonthlyReport(month?: string) {
  return useQuery({ queryKey: keys.report(month), queryFn: () => reportsApi.monthly(month) });
}

export function useGoals() {
  return useQuery({ queryKey: keys.goals, queryFn: goalsApi.list });
}

export function useCreateGoal() {
  return useMutation({ mutationFn: (body: GoalRequest) => goalsApi.create(body), onSuccess: invalidateFinancial });
}

export function useDepositGoal() {
  return useMutation({
    mutationFn: ({ id, amount }: { id: string; amount: number }) => goalsApi.deposit(id, amount),
    onSuccess: invalidateFinancial,
  });
}

export function useDeleteGoal() {
  return useMutation({ mutationFn: (id: string) => goalsApi.remove(id), onSuccess: invalidateFinancial });
}

export function useCreateBudget() {
  return useMutation({
    mutationFn: ({ category, monthlyLimit, month }: { category: ExpenseCategory; monthlyLimit: number; month?: string }) =>
      budgetsApi.create(category, monthlyLimit, month),
    onSuccess: invalidateFinancial,
  });
}

export function useUpdateBudget() {
  return useMutation({
    mutationFn: ({ id, monthlyLimit }: { id: string; monthlyLimit: number }) => budgetsApi.update(id, monthlyLimit),
    onSuccess: invalidateFinancial,
  });
}

export function useDeleteBudget() {
  return useMutation({ mutationFn: (id: string) => budgetsApi.remove(id), onSuccess: invalidateFinancial });
}
