import { useState, type FormEvent } from 'react';
import type { SavingsGoalResponse } from '../api/types';
import { errorMessage } from '../api/client';
import { useCreateGoal, useDeleteGoal, useDepositGoal, useGoals } from '../hooks/useFinance';
import { GoalCard } from '../components/GoalCard';
import { Confirm, EmptyState, Skeleton } from '../ui/primitives';
import { useToast } from '../ui/Toast';
import { money, parseMoney } from '../utils/format';

/** Metas de economia: guardar dinheiro com alvo e prazo. Guardar não é gasto, então não entra no resumo. */
export function GoalsPage() {
  const { notify } = useToast();
  const [removing, setRemoving] = useState<SavingsGoalResponse | null>(null);
  const [form, setForm] = useState({ name: '', targetAmount: '', deadline: '' });

  const goals = useGoals();
  const create = useCreateGoal();
  const deposit = useDepositGoal();
  const remove = useDeleteGoal();

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await create.mutateAsync({
        name: form.name,
        targetAmount: parseMoney(form.targetAmount),
        deadline: form.deadline || undefined,
      });
      notify('success', `Meta ${form.name} criada.`);
      setForm({ name: '', targetAmount: '', deadline: '' });
    } catch (err) {
      notify('error', errorMessage(err));
    }
  };

  const onDeposit = (goal: SavingsGoalResponse) => async (amount: number) => {
    try {
      const updated = await deposit.mutateAsync({ id: goal.id, amount });
      notify('success', updated.status === 'COMPLETED'
        ? `Meta ${goal.name} concluída!`
        : `${money(amount)} guardados. Faltam ${money(updated.remaining)}.`);
    } catch (err) {
      notify('error', errorMessage(err));
    }
  };

  const confirmRemove = async () => {
    if (!removing) return;
    try {
      await remove.mutateAsync(removing.id);
      notify('success', 'Meta removida.');
    } catch (err) {
      notify('error', errorMessage(err));
    } finally {
      setRemoving(null);
    }
  };

  const saved = (goals.data ?? []).reduce((sum, g) => sum + g.savedAmount, 0);

  return (
    <div className="grid-two">
      <section className="card">
        <div className="card-head"><h2>Nova meta</h2></div>
        <p className="muted small">
          Guardar dinheiro para uma meta não é gasto: não entra no total do mês nem consome orçamento.
          Com prazo, a Lumi calcula quanto guardar por mês.
        </p>
        <form className="form" onSubmit={submit}>
          <label>
            Nome
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })}
                   required maxLength={80} placeholder="ex.: Viagem" />
          </label>
          <div className="row">
            <label>
              Quanto juntar (R$)
              <input inputMode="decimal" value={form.targetAmount} required placeholder="0,00"
                     onChange={(e) => setForm({ ...form, targetAmount: e.target.value })} />
            </label>
            <label>
              Prazo (opcional)
              <input type="date" value={form.deadline}
                     onChange={(e) => setForm({ ...form, deadline: e.target.value })} />
            </label>
          </div>
          <button className="btn primary" disabled={create.isPending}>
            {create.isPending ? 'Salvando…' : 'Criar meta'}
          </button>
        </form>
      </section>

      <section className="card">
        <div className="card-head">
          <h2>Minhas metas</h2>
          <span className="total">{money(saved)} guardados</span>
        </div>
        {goals.isLoading ? <Skeleton lines={5} height={22} /> : (goals.data?.length ?? 0) === 0 ? (
          <EmptyState icon="🏦" title="Nenhuma meta ainda"
                      hint='Crie ao lado ou diga à Lumi: "quero juntar 6 mil para uma viagem até janeiro".' />
        ) : (
          <div className="stack">
            {goals.data!.map((g) => (
              <GoalCard key={g.id} goal={g} onDeposit={onDeposit(g)} onRemove={() => setRemoving(g)} />
            ))}
          </div>
        )}
      </section>

      <Confirm open={!!removing} title="Remover esta meta?" danger
               text={removing ? `${removing.name} · ${money(removing.savedAmount)} de ${money(removing.targetAmount)} guardados.` : ''}
               onCancel={() => setRemoving(null)} onConfirm={confirmRemove} />
    </div>
  );
}
