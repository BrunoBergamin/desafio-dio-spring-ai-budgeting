import { useState, type FormEvent } from 'react';
import type { Category, RecurringResponse, TransactionType } from '../api/types';
import { errorMessage } from '../api/client';
import { useCreateRecurring, useDeleteRecurring, useRecurring, useUpdateRecurring } from '../hooks/useFinance';
import { Confirm, EmptyState, Skeleton } from '../ui/primitives';
import { useToast } from '../ui/Toast';
import { categoriesOf, categoryEmoji, money, shortDate } from '../utils/format';

/** Contas que se repetem todo mês: a pessoa cadastra uma vez e o lançamento entra sozinho no dia. */
export function RecurringPage() {
  const { notify } = useToast();
  const [removing, setRemoving] = useState<RecurringResponse | null>(null);
  const [form, setForm] = useState({
    description: '',
    amount: '',
    category: 'HOUSING' as Category,
    dayOfMonth: '10',
    type: 'EXPENSE' as TransactionType,
  });

  const rules = useRecurring();
  const create = useCreateRecurring();
  const update = useUpdateRecurring();
  const remove = useDeleteRecurring();

  const changeType = (type: TransactionType) =>
    setForm({ ...form, type, category: categoriesOf(type)[0].value });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await create.mutateAsync({
        description: form.description,
        amount: Number(form.amount.replace(/\./g, '').replace(',', '.')),
        category: form.category,
        dayOfMonth: Number(form.dayOfMonth),
      });
      notify('success', `${form.description} vai entrar sozinho todo dia ${form.dayOfMonth}.`);
      setForm({ ...form, description: '', amount: '' });
    } catch (err) {
      notify('error', errorMessage(err));
    }
  };

  const togglePause = async (rule: RecurringResponse) => {
    try {
      await update.mutateAsync({
        id: rule.id,
        body: {
          description: rule.description,
          amount: rule.amount,
          category: rule.category,
          dayOfMonth: rule.dayOfMonth,
          active: !rule.active,
        },
      });
      notify('success', rule.active ? `${rule.description} pausada.` : `${rule.description} retomada.`);
    } catch (err) {
      notify('error', errorMessage(err));
    }
  };

  const confirmRemove = async () => {
    if (!removing) return;
    try {
      await remove.mutateAsync(removing.id);
      notify('success', 'Conta removida. Os lançamentos já feitos continuam no histórico.');
    } catch (err) {
      notify('error', errorMessage(err));
    } finally {
      setRemoving(null);
    }
  };

  const active = (rules.data ?? []).filter((r) => r.active).length;

  return (
    <div className="grid-two">
      <section className="card">
        <div className="card-head"><h2>Nova conta fixa</h2></div>
        <p className="muted small">
          Aluguel, streaming, academia, salário: cadastre uma vez e o lançamento entra sozinho todo mês,
          no dia escolhido. Em mês curto, o dia 31 cai no último dia.
        </p>
        <form className="form" onSubmit={submit}>
          <div className="segmented" role="group" aria-label="tipo da conta">
            <button type="button" className={form.type === 'EXPENSE' ? 'active' : ''}
                    aria-pressed={form.type === 'EXPENSE'} onClick={() => changeType('EXPENSE')}>Gasto</button>
            <button type="button" className={form.type === 'INCOME' ? 'active' : ''}
                    aria-pressed={form.type === 'INCOME'} onClick={() => changeType('INCOME')}>Receita</button>
          </div>
          <label>
            Descrição
            <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}
                   required maxLength={120} placeholder="ex.: Aluguel" />
          </label>
          <div className="row">
            <label>
              Valor (R$)
              <input inputMode="decimal" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })}
                     required placeholder="0,00" />
            </label>
            <label>
              Dia do mês
              <input type="number" min={1} max={31} value={form.dayOfMonth}
                     onChange={(e) => setForm({ ...form, dayOfMonth: e.target.value })} required />
            </label>
          </div>
          <label>
            Categoria
            <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value as Category })}>
              {categoriesOf(form.type).map((c) => <option key={c.value} value={c.value}>{c.emoji} {c.label}</option>)}
            </select>
          </label>
          <button className="btn primary" disabled={create.isPending}>
            {create.isPending ? 'Salvando…' : 'Salvar conta fixa'}
          </button>
        </form>
      </section>

      <section className="card">
        <div className="card-head">
          <h2>Contas do mês</h2>
          <span className="muted small">{active} ativa{active === 1 ? '' : 's'}</span>
        </div>
        {rules.isLoading ? <Skeleton lines={5} /> : (rules.data?.length ?? 0) === 0 ? (
          <EmptyState icon="🔁" title="Nenhuma conta fixa"
                      hint='Cadastre ao lado ou diga à Lumi: "todo dia 10 pago 1500 de aluguel".' />
        ) : (
          <ul className="list">
            {rules.data!.map((r) => (
              <li key={r.id} className="list-row">
                <span className="list-icon" aria-hidden>{categoryEmoji(r.category)}</span>
                <span className="list-main">
                  <span>
                    {r.description}{' '}
                    <span className={`pill ${r.active ? 'ok' : ''}`}>{r.active ? 'ativa' : 'pausada'}</span>
                  </span>
                  <span className="muted small">
                    {r.categoryLabel} · todo dia {r.dayOfMonth}
                    {r.nextOccurrence ? ` · próxima em ${shortDate(r.nextOccurrence)}` : ''}
                  </span>
                </span>
                <span className={`list-amount ${r.type === 'INCOME' ? 'income' : ''}`}>
                  {r.type === 'INCOME' ? '+' : ''}{money(r.amount)}
                </span>
                <span className="nowrap">
                  <button className="btn ghost small" onClick={() => togglePause(r)} disabled={update.isPending}
                          aria-label={`${r.active ? 'pausar' : 'retomar'} ${r.description}`}>
                    {r.active ? '⏸' : '▶'}
                  </button>
                  <button className="btn ghost small" onClick={() => setRemoving(r)}
                          aria-label={`remover ${r.description}`}>✕</button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <Confirm open={!!removing} title="Remover esta conta fixa?" danger
               text={removing ? `${removing.description} · ${money(removing.amount)} · todo dia ${removing.dayOfMonth}. Os lançamentos já feitos continuam no histórico.` : ''}
               onCancel={() => setRemoving(null)} onConfirm={confirmRemove} />
    </div>
  );
}
