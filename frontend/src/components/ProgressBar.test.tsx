import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ProgressBar } from './ProgressBar';

describe('ProgressBar', () => {
  it('nunca some quando está em zero nem passa de 100 quando ultrapassa o alvo', () => {
    const { rerender } = render(<ProgressBar percentage={0} label="Viagem: 0% guardado" />);
    const zero = screen.getByRole('progressbar');
    expect(zero).toHaveAttribute('aria-valuenow', '0');
    expect((zero.firstElementChild as HTMLElement).style.width).toBe('2%');

    rerender(<ProgressBar percentage={116.7} label="Viagem: 116.7% guardado" />);
    const cheia = screen.getByRole('progressbar');
    expect(cheia).toHaveAttribute('aria-valuenow', '117');
    expect((cheia.firstElementChild as HTMLElement).style.width).toBe('100%');
  });

  it('usa o tom recebido, porque o mesmo percentual significa coisas opostas em meta e orçamento', () => {
    const { rerender } = render(<ProgressBar percentage={90} label="Viagem" tone="ok" />);
    expect(screen.getByRole('progressbar')).toHaveClass('tone-ok');

    rerender(<ProgressBar percentage={90} label="Mercado" tone="warning" />);
    expect(screen.getByRole('progressbar')).toHaveClass('tone-warning');
  });
});
