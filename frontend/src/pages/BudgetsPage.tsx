import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { budgetsApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { BudgetStatusResponse, Category } from '../api/types';
import { BudgetBar } from '../components/BudgetBar';
import { CATEGORIES, currentMonth } from '../utils/format';

export function BudgetsPage() {
  const [month, setMonth] = useState(currentMonth());
  const [items, setItems] = useState<BudgetStatusResponse[]>([]);
  const [category, setCategory] = useState<Category>('GROCERIES');
  const [limit, setLimit] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    budgetsApi.list(month).then(setItems).catch((e) => setError(errorMessage(e)));
  }, [month]);

  useEffect(load, [load]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await budgetsApi.create(category, Number(limit.replace(',', '.')), month);
      setLimit('');
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: string) => {
    if (!confirm('Remover este orçamento?')) return;
    await budgetsApi.remove(id).catch((e) => setError(errorMessage(e)));
    load();
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
              {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.emoji} {c.label}</option>)}
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
          {error && <p className="error" role="alert">{error}</p>}
          <button className="btn primary" disabled={busy}>{busy ? 'Salvando…' : 'Salvar limite'}</button>
        </form>
      </section>

      <section className="card">
        <div className="card-head"><h2>Orçamentos de {month}</h2></div>
        {items.length === 0 ? (
          <p className="muted">Nenhum limite definido para este mês.</p>
        ) : (
          <div className="stack">
            {items.map((b) => <BudgetBar key={b.id} budget={b} onRemove={() => remove(b.id)} />)}
          </div>
        )}
      </section>
    </div>
  );
}
