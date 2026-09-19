import { useMemo, useState, type FormEvent } from 'react';
import type { Category, TransactionResponse, TransactionType } from '../api/types';
import { errorMessage } from '../api/client';
import { useCreateTransaction, useDeleteTransaction, useTransactions, useUpdateTransaction } from '../hooks/useFinance';
import { Confirm, EmptyState, Skeleton } from '../ui/primitives';
import { useToast } from '../ui/Toast';
import { CATEGORIES, categoriesOf, categoryEmoji, currentMonth, money, monthLabel, monthRange, parseMoney, shiftMonth, shortDate, today } from '../utils/format';

type Sort = 'date' | 'amount';
const PAGE_SIZE = 50;

export function TransactionsPage() {
  const { notify } = useToast();
  const [month, setMonth] = useState(currentMonth());
  const [allTime, setAllTime] = useState(false);
  const [filter, setFilter] = useState<Category | ''>('');
  const [kind, setKind] = useState<TransactionType | ''>('');
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<Sort>('date');
  const [editing, setEditing] = useState<TransactionResponse | null>(null);
  const [removing, setRemoving] = useState<TransactionResponse | null>(null);

  const [page, setPage] = useState(0);
  // Trocar de mês, de filtro ou marcar "tudo" muda a lista inteira: a leitura recomeça da primeira página
  const goToMonth = (value: string) => { setMonth(value); setPage(0); };
  const changeFilter = (value: Category | '') => { setFilter(value); setPage(0); };
  const changeKind = (value: TransactionType | '') => { setKind(value); setFilter(''); setPage(0); };
  const toggleAllTime = (value: boolean) => { setAllTime(value); setPage(0); };
  const range = allTime ? {} : monthRange(month);
  const query = useTransactions({ type: kind || undefined, category: filter || undefined, ...range, page, size: PAGE_SIZE });
  const create = useCreateTransaction();
  const update = useUpdateTransaction();
  const remove = useDeleteTransaction();

  const [form, setForm] = useState({ description: '', amount: '', category: 'GROCERIES' as Category, date: today(), type: 'EXPENSE' as TransactionType });

  /** Trocar entre gasto e receita troca a lista de categorias: a primeira do grupo vira a escolhida. */
  const changeFormType = (type: TransactionType) =>
    setForm({ ...form, type, category: categoriesOf(type)[0].value });

  const items = useMemo(() => {
    const term = search.trim().toLowerCase();
    const list = (query.data?.content ?? []).filter((t) => !term || t.description.toLowerCase().includes(term) || t.categoryLabel.toLowerCase().includes(term));
    return sort === 'amount' ? [...list].sort((a, b) => b.amount - a.amount) : list;
  }, [query.data, search, sort]);
  // Saldo do que esta na tela: receita entra somando, gasto subtraindo
  const total = items.reduce((sum, t) => sum + (t.type === 'INCOME' ? t.amount : -t.amount), 0);
  const totalPages = query.data?.totalPages ?? 0;
  const totalElements = query.data?.totalElements ?? 0;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const body = { description: form.description, amount: parseMoney(form.amount), category: form.category, date: form.date };
      if (editing) {
        await update.mutateAsync({ id: editing.id, body });
        notify('success', 'Lançamento atualizado.');
        setEditing(null);
      } else {
        await create.mutateAsync(body);
        notify('success', `${money(body.amount)} registrado em ${CATEGORIES.find((c) => c.value === body.category)?.label}.`);
      }
      setForm({ description: '', amount: '', category: form.category, date: today(), type: form.type });
    } catch (err) {
      notify('error', errorMessage(err));
    }
  };

  const startEdit = (t: TransactionResponse) => {
    setEditing(t);
    setForm({ description: t.description, amount: String(t.amount).replace('.', ','), category: t.category, date: t.date, type: t.type });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const confirmRemove = async () => {
    if (!removing) return;
    try {
      await remove.mutateAsync(removing.id);
      notify('success', 'Lançamento removido.');
    } catch (err) {
      notify('error', errorMessage(err));
    } finally {
      setRemoving(null);
    }
  };

  return (
    <div className="grid-two">
      <section className="card">
        <div className="card-head">
          <h2>{editing ? 'Editar lançamento' : form.type === 'INCOME' ? 'Nova receita' : 'Novo gasto'}</h2>
          {editing && <button className="btn ghost small" onClick={() => { setEditing(null); setForm({ description: '', amount: '', category: 'GROCERIES', date: today(), type: 'EXPENSE' }); }}>cancelar</button>}
        </div>
        <form className="form" onSubmit={submit}>
          <div className="segmented" role="group" aria-label="tipo do lançamento">
            <button type="button" className={form.type === 'EXPENSE' ? 'active' : ''}
                    aria-pressed={form.type === 'EXPENSE'} onClick={() => changeFormType('EXPENSE')}>Gasto</button>
            <button type="button" className={form.type === 'INCOME' ? 'active' : ''}
                    aria-pressed={form.type === 'INCOME'} onClick={() => changeFormType('INCOME')}>Receita</button>
          </div>
          <label>
            Descrição
            <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required maxLength={120} placeholder="ex.: almoço com a equipe" />
          </label>
          <div className="row">
            <label>
              Valor (R$)
              <input inputMode="decimal" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required placeholder="0,00" />
            </label>
            <label>
              Data
              <input type="date" value={form.date} max={today()} onChange={(e) => setForm({ ...form, date: e.target.value })} required />
            </label>
          </div>
          <label>
            Categoria
            <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value as Category })}>
              {categoriesOf(form.type).map((c) => <option key={c.value} value={c.value}>{c.emoji} {c.label}</option>)}
            </select>
          </label>
          <button className="btn primary" disabled={create.isPending || update.isPending}>
            {create.isPending || update.isPending ? 'Salvando…' : editing ? 'Salvar alterações' : form.type === 'INCOME' ? 'Salvar receita' : 'Salvar gasto'}
          </button>
        </form>
      </section>

      <section className="card">
        <div className="card-head">
          <h2>Lançamentos</h2>
          <span className={`total ${total < 0 ? 'negative' : ''}`}
                title={totalPages > 1 ? 'saldo só desta página' : 'receitas menos gastos do que está listado'}>
            {money(total)}{totalPages > 1 && <small className="muted"> nesta página</small>}
          </span>
        </div>
        <div className="toolbar">
          <div className="month-nav">
            <button className="btn ghost icon" onClick={() => goToMonth(shiftMonth(month, -1))} aria-label="mês anterior" disabled={allTime}>‹</button>
            <span className="month-label">{allTime ? 'todo o período' : monthLabel(month)}</span>
            <button className="btn ghost icon" onClick={() => goToMonth(shiftMonth(month, 1))} aria-label="próximo mês" disabled={allTime || month === currentMonth()}>›</button>
          </div>
          <label className="check"><input type="checkbox" checked={allTime} onChange={(e) => toggleAllTime(e.target.checked)} /> tudo</label>
          <select value={kind} onChange={(e) => changeKind(e.target.value as TransactionType | '')} aria-label="filtrar por tipo">
            <option value="">gastos e receitas</option>
            <option value="EXPENSE">só gastos</option>
            <option value="INCOME">só receitas</option>
          </select>
          <select value={filter} onChange={(e) => changeFilter(e.target.value as Category | '')} aria-label="filtrar por categoria">
            <option value="">todas as categorias</option>
            {(kind ? categoriesOf(kind) : CATEGORIES).map((c) => <option key={c.value} value={c.value}>{c.emoji} {c.label}</option>)}
          </select>
          <input className="search" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="buscar…" aria-label="buscar" />
          <select value={sort} onChange={(e) => setSort(e.target.value as Sort)} aria-label="ordenar">
            <option value="date">mais recentes</option>
            <option value="amount">maior valor</option>
          </select>
        </div>

        {query.isLoading ? <Skeleton lines={6} /> : items.length === 0 ? (
          <EmptyState icon="🧾" title="Nada por aqui" hint={search ? 'Nenhum lançamento bate com a busca.' : 'Registre o primeiro lançamento ao lado ou fale com a Lumi.'} />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Data</th><th>Descrição</th><th>Categoria</th><th className="num">Valor</th><th className="num">Ações</th></tr>
              </thead>
              <tbody>
                {items.map((t) => (
                  <tr key={t.id} className={editing?.id === t.id ? 'editing' : ''}>
                    <td className="nowrap">{shortDate(t.date)}</td>
                    <td>
                      {t.description}
                      {t.recurringId && <span className="pill inline" title="lançamento de uma conta fixa">🔁</span>}
                    </td>
                    <td className="nowrap"><span aria-hidden>{categoryEmoji(t.category)}</span> {t.categoryLabel}</td>
                    <td className={`num ${t.type === 'INCOME' ? 'income' : ''}`}>{t.type === 'INCOME' ? '+' : ''}{money(t.amount)}</td>
                    <td className="num nowrap">
                      <button className="btn ghost small" onClick={() => startEdit(t)} aria-label={`editar ${t.description}`}>✎</button>
                      <button className="btn ghost small" onClick={() => setRemoving(t)} aria-label={`remover ${t.description}`}>✕</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {totalPages > 1 && (
              <nav className="pager" aria-label="paginação">
                <button className="btn ghost small" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0 || query.isFetching}>‹ anterior</button>
                <span className="muted small">página {page + 1} de {totalPages} · {totalElements} lançamentos</span>
                <button className="btn ghost small" onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1 || query.isFetching}>próxima ›</button>
              </nav>
            )}
          </div>
        )}
      </section>

      <Confirm open={!!removing} title="Remover este lançamento?" danger
               text={removing ? `${removing.description} · ${money(removing.amount)} · ${shortDate(removing.date)}` : ''}
               onCancel={() => setRemoving(null)} onConfirm={confirmRemove} />
    </div>
  );
}
