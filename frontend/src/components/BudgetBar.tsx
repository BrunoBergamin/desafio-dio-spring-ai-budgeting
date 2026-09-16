import type { BudgetStatusResponse } from '../api/types';
import { categoryEmoji, money } from '../utils/format';

export function BudgetBar({ budget, onRemove }: { budget: BudgetStatusResponse; onRemove?: () => void }) {
  const width = Math.min(100, Math.max(2, budget.usedPercentage));
  return (
    <div className={`budget status-${budget.status.toLowerCase()}`}>
      <div className="budget-head">
        <span className="budget-title">
          <span aria-hidden>{categoryEmoji(budget.category)}</span> {budget.categoryLabel}
        </span>
        <span className="budget-values">
          {money(budget.spent)} <span className="muted">de {money(budget.monthlyLimit)}</span>
        </span>
      </div>
      <div className="bar" role="progressbar" aria-valuenow={budget.usedPercentage} aria-valuemin={0} aria-valuemax={100}>
        <div className="bar-fill" style={{ width: `${width}%` }} />
      </div>
      <div className="budget-foot">
        <span className="pill">{budget.statusLabel} · {budget.usedPercentage.toFixed(0)}%</span>
        {onRemove && (
          <button className="btn ghost small" onClick={onRemove} aria-label={`remover orçamento de ${budget.categoryLabel}`}>
            remover
          </button>
        )}
      </div>
    </div>
  );
}
