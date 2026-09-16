import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { transactionsApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { Category, TransactionResponse } from '../api/types';
import { CATEGORIES, categoryEmoji, money, shortDate, today } from '../utils/format';

export function TransactionsPage() {
  const [items, setItems] = useState<TransactionResponse[]>([]);
  const [filter, setFilter] = useState<Category | ''>('');
  const [error, setError] = useState<string | null>(null);
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState<Category>('GROCERIES');
  const [date, setDate] = useState(today());
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    transactionsApi
      .list(filter ? { category: filter } : undefined)
      .then(setItems)
      .catch((e) => setError(errorMessage(e)));
  }, [filter]);

  useEffect(load, [load]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await transactionsApi.create({ description, amount: Number(amount.replace(',', '.')), category, date });
      setDescription('');
      setAmount('');
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: string) => {
    if (!confirm('Remover este gasto?')) return;
    await transactionsApi.remove(id).catch((e) => setError(errorMessage(e)));
    load();
  };

  const total = items.reduce((sum, t) => sum + t.amount, 0);

  return (
    <div className="grid-two">
      <section className="card">
        <div className="card-head"><h2>Novo gasto</h2></div>
        <form className="form" onSubmit={submit}>
          <label>
            Descrição
            <input value={description} onChange={(e) => setDescription(e.target.value)} required maxLength={120} placeholder="ex.: almoço com a equipe" />
          </label>
          <div className="row">
            <label>
              Valor (R$)
              <input inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} required placeholder="0,00" />
            </label>
            <label>
              Data
              <input type="date" value={date} max={today()} onChange={(e) => setDate(e.target.value)} required />
            </label>
          </div>
          <label>
            Categoria
            <select value={category} onChange={(e) => setCategory(e.target.value as Category)}>
              {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.emoji} {c.label}</option>)}
            </select>
          </label>
          {error && <p className="error" role="alert">{error}</p>}
          <button className="btn primary" disabled={busy}>{busy ? 'Salvando…' : 'Salvar gasto'}</button>
        </form>
      </section>

      <section className="card">
        <div className="card-head">
          <h2>Gastos</h2>
          <div className="row-inline">
            <select value={filter} onChange={(e) => setFilter(e.target.value as Category | '')} aria-label="filtrar por categoria">
              <option value="">todas as categorias</option>
              {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
            </select>
            <span className="total">{money(total)}</span>
          </div>
        </div>
        {items.length === 0 ? (
          <p className="muted">Nada por aqui ainda.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Data</th><th>Descrição</th><th>Categoria</th><th className="num">Valor</th><th /></tr>
              </thead>
              <tbody>
                {items.map((t) => (
                  <tr key={t.id}>
                    <td>{shortDate(t.date)}</td>
                    <td>{t.description}</td>
                    <td><span aria-hidden>{categoryEmoji(t.category)}</span> {t.categoryLabel}</td>
                    <td className="num">{money(t.amount)}</td>
                    <td className="num">
                      <button className="btn ghost small" onClick={() => remove(t.id)} aria-label={`remover ${t.description}`}>✕</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
