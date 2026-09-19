import { useState } from 'react';
import type { SavingsGoalResponse } from '../api/types';
import { ProgressBar } from './ProgressBar';
import { money, parseMoney, shortDate } from '../utils/format';

interface Props {
  goal: SavingsGoalResponse;
  onDeposit: (amount: number) => Promise<unknown>;
  onRemove: () => void;
}

/** Uma meta: quanto já foi guardado, quanto falta e um campo para guardar mais. */
export function GoalCard({ goal, onDeposit, onRemove }: Props) {
  const [amount, setAmount] = useState('');
  const [saving, setSaving] = useState(false);

  const tone = goal.status === 'COMPLETED' ? 'ok' : goal.status === 'OVERDUE' ? 'danger' : 'warning';

  const deposit = async () => {
    const value = parseMoney(amount);
    if (!value || value <= 0) return;
    setSaving(true);
    try {
      await onDeposit(value);
      setAmount('');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className={`budget status-${goal.status.toLowerCase()}`}>
      <div className="budget-head">
        <span className="budget-title">🏦 {goal.name}</span>
        <span className="budget-values">
          {money(goal.savedAmount)} <span className="muted">de {money(goal.targetAmount)}</span>
        </span>
      </div>

      <ProgressBar percentage={goal.percentage} tone={tone}
                   label={`${goal.name}: ${goal.percentage}% guardado`} />

      <div className="budget-foot">
        <span className="pill">{goal.statusLabel} · {goal.percentage.toFixed(0)}%</span>
        <span className="muted small">
          {goal.status === 'COMPLETED'
            ? 'alvo alcançado'
            : `faltam ${money(goal.remaining)}${goal.deadline ? ` até ${shortDate(goal.deadline)}` : ''}`}
        </span>
      </div>

      {goal.suggestedMonthly != null && (
        <p className="muted small">Para chegar no prazo: cerca de {money(goal.suggestedMonthly)} por mês.</p>
      )}

      <div className="row-inline">
        <input className="input-sm" inputMode="decimal" value={amount} placeholder="guardar R$"
               aria-label={`guardar dinheiro na meta ${goal.name}`}
               onChange={(e) => setAmount(e.target.value)}
               onKeyDown={(e) => { if (e.key === 'Enter') deposit(); }} />
        <button className="btn primary small" onClick={deposit} disabled={saving || !amount}>guardar</button>
        <button className="btn ghost small" onClick={onRemove} aria-label={`remover a meta ${goal.name}`}>remover</button>
      </div>
    </div>
  );
}
