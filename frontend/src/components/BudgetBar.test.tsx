import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BudgetBar } from './BudgetBar';
import type { BudgetStatusResponse } from '../api/types';

const budget: BudgetStatusResponse = {
  id: '1', category: 'GROCERIES', categoryLabel: 'Mercado', month: '2026-09',
  monthlyLimit: 500, spent: 425, remaining: 75, usedPercentage: 85,
  status: 'WARNING', statusLabel: 'Atenção: perto do limite', message: '',
};

describe('BudgetBar', () => {
  it('mostra gasto, limite, status e quanto resta', () => {
    render(<BudgetBar budget={budget} />);
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '85');
    expect(screen.getByText(/Atenção: perto do limite · 85%/)).toBeInTheDocument();
    expect(screen.getByText(/restam/)).toHaveTextContent('75,00');
  });

  it('edita o limite inline e envia o valor convertido', async () => {
    const onUpdate = vi.fn().mockResolvedValue(undefined);
    render(<BudgetBar budget={budget} onUpdate={onUpdate} />);

    await userEvent.click(screen.getByRole('button', { name: 'editar' }));
    const input = screen.getByRole('textbox', { name: 'novo limite' });
    await userEvent.clear(input);
    await userEvent.type(input, '1.200,00{Enter}');

    expect(onUpdate).toHaveBeenCalledWith(1200);
  });
});
