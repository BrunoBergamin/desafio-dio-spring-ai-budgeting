import { useState } from 'react';
import type { BudgetStatusResponse } from '../api/types';
import { categoryEmoji, money, parseMoney } from '../utils/format';

interface Props {
  budget: BudgetStatusResponse;
  onRemove?: () => void;
  onUpdate?: (monthlyLimit: number) => Promise<unknown>;
}

export function BudgetBar({ budget, onRemove, onUpdate }: Props) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(String(budget.monthlyLimit).replace('.', ','));
  const width = Math.min(100, Math.max(2, budget.usedPercentage));

  const save = async () => {
    if (!onUpdate) return;
    await onUpdate(parseMoney(value));
    setEditing(false);
  };

  return (
    <div className={`budget status-${budget.status.toLowerCase()}`}>
      <div className="budget-head">
        <span className="budget-title">
          <span aria-hidden>{categoryEmoji(budget.category)}</span> {budget.categoryLabel}
        </span>
        {editing ? (
          <span className="row-inline">
            <input className="input-sm" inputMode="decimal" value={value} onChange={(e) => setValue(e.target.value)} aria-label="novo limite" autoFocus
                   onKeyDown={(e) => { if (e.key === 'Enter') save(); if (e.key === 'Escape') setEditing(false); }} />
            <button className="btn primary small" onClick={save}>salvar</button>
            <button className="btn ghost small" onClick={() => setEditing(false)}>cancelar</button>
          </span>
        ) : (
          <span className="budget-values">
            {money(budget.spent)} <span className="muted">de {money(budget.monthlyLimit)}</span>
          </span>
        )}
      </div>
      <div className="bar" role="progressbar" aria-valuenow={budget.usedPercentage} aria-valuemin={0} aria-valuemax={100} aria-label={`${budget.categoryLabel}: ${budget.usedPercentage}% usado`}>
        <div className="bar-fill" style={{ width: `${width}%` }} />
      </div>
      <div className="budget-foot">
        <span className="pill">{budget.statusLabel} · {budget.usedPercentage.toFixed(0)}%</span>
        <span className="row-inline">
          <span className="muted small">{budget.remaining >= 0 ? `restam ${money(budget.remaining)}` : `passou ${money(-budget.remaining)}`}</span>
          {onUpdate && !editing && <button className="btn ghost small" onClick={() => setEditing(true)}>editar</button>}
          {onRemove && <button className="btn ghost small" onClick={onRemove} aria-label={`remover orçamento de ${budget.categoryLabel}`}>remover</button>}
        </span>
      </div>
    </div>
  );
}
