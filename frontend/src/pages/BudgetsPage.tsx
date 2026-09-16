import { useState, type FormEvent } from 'react';
import type { BudgetStatusResponse, Category } from '../api/types';
import { errorMessage } from '../api/client';
import { useBudgets, useCreateBudget, useDeleteBudget, useUpdateBudget } from '../hooks/useFinance';
import { BudgetBar } from '../components/BudgetBar';
import { Confirm, EmptyState, Skeleton } from '../ui/primitives';
import { useToast } from '../ui/Toast';
import { CATEGORIES, currentMonth, money, monthLabel, parseMoney, shiftMonth } from '../utils/format';

export function BudgetsPage() {
  const { notify } = useToast();
  const [month, setMonth] = useState(currentMonth());
  const [category, setCategory] = useState<Category>('GROCERIES');
  const [limit, setLimit] = useState('');
  const [removing, setRemoving] = useState<BudgetStatusResponse | null>(null);
  const budgets = useBudgets(month);
  const create = useCreateBudget();
  const update = useUpdateBudget();
  const remove = useDeleteBudget();

  const used = new Set((budgets.data ?? []).map((b) => b.category));
  const totalLimit = (budgets.data ?? []).reduce((s, b) => s + b.monthlyLimit, 0);
  const totalSpent = (budgets.data ?? []).reduce((s, b) => s + b.spent, 0);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await create.mutateAsync({ category, monthlyLimit: parseMoney(limit), month });
      notify('success', `Limite de ${CATEGORIES.find((c) => c.value === category)?.label} definido.`);
      setLimit('');
    } catch (err) {
      notify('error', errorMessage(err));
    }
  };

  const onUpdate = (b: BudgetStatusResponse) => async (monthlyLimit: number) => {
    try {
      await update.mutateAsync({ id: b.id, monthlyLimit });
      notify('success', 'Limite atualizado.');
    } catch (err) {
      notify('error', errorMessage(err));
      throw err;
    }
  };

  const confirmRemove = async () => {
    if (!removing) return;
    try {
      await remove.mutateAsync(removing.id);
      notify('success', 'Orçamento removido.');
    } catch (err) {
      notify('error', errorMessage(err));
    } finally {
      setRemoving(null);
    }
  };

  return (
    <div className="grid-two">
      <section className="card">
        <div className="card-head"><h2>Definir limite</h2></div>
        <p className="muted small">Também dá para falar com a Lumi: “meu limite de mercado é 800 por mês”.</p>
        <form className="form" onSubmit={submit}>
          <label>
            Categoria
            <select value={category} onChange={(e) => setCategory(e.target.value as Category)}>
              {CATEGORIES.map((c) => (
                <option key={c.value} value={c.value} disabled={used.has(c.value)}>
                  {c.emoji} {c.label}{used.has(c.value) ? ' (já definido)' : ''}
                </option>
              ))}
            </select>
          </label>
          <div className="row">
            <label>
              Limite mensal (R$)
              <input inputMode="decimal" value={limit} onChange={(e) => setLimit(e.target.value)} required placeholder="0,00" />
            </label>
            <label>
              Mês
              <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} required />
            </label>
          </div>
          <button className="btn primary" disabled={create.isPending}>{create.isPending ? 'Salvando…' : 'Salvar limite'}</button>
        </form>
      </section>

      <section className="card">
        <div className="card-head">
          <div className="month-nav">
            <button className="btn ghost icon" onClick={() => setMonth(shiftMonth(month, -1))} aria-label="mês anterior">‹</button>
            <h2 className="month-label">{monthLabel(month)}</h2>
            <button className="btn ghost icon" onClick={() => setMonth(shiftMonth(month, 1))} aria-label="próximo mês">›</button>
          </div>
          {budgets.data && budgets.data.length > 0 && (
            <span className="muted small">{money(totalSpent)} de {money(totalLimit)} no total</span>
          )}
        </div>
        {budgets.isLoading ? <Skeleton lines={4} height={22} /> : (budgets.data?.length ?? 0) === 0 ? (
          <EmptyState icon="🎯" title="Nenhum limite para este mês" hint="Defina ao lado, ou peça para a Lumi." />
        ) : (
          <div className="stack">
            {budgets.data!.map((b) => <BudgetBar key={b.id} budget={b} onUpdate={onUpdate(b)} onRemove={() => setRemoving(b)} />)}
          </div>
        )}
      </section>

      <Confirm open={!!removing} title="Remover este orçamento?" danger text={removing ? `${removing.categoryLabel} · ${money(removing.monthlyLimit)}` : ''}
               onCancel={() => setRemoving(null)} onConfirm={confirmRemove} />
    </div>
  );
}
