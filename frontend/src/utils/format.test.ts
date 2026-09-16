import { describe, expect, it } from 'vitest';
import { monthRange, parseMoney, percentDelta, shiftMonth, money, shortDate } from './format';

describe('format', () => {
  it('monta o intervalo do mês respeitando fevereiro e meses de 31 dias', () => {
    expect(monthRange('2026-02')).toEqual({ start: '2026-02-01', end: '2026-02-28' });
    expect(monthRange('2028-02')).toEqual({ start: '2028-02-01', end: '2028-02-29' });
    expect(monthRange('2026-09')).toEqual({ start: '2026-09-01', end: '2026-09-30' });
  });

  it('navega entre meses virando o ano', () => {
    expect(shiftMonth('2026-01', -1)).toBe('2025-12');
    expect(shiftMonth('2026-12', 1)).toBe('2027-01');
  });

  it('aceita valor digitado no formato brasileiro', () => {
    expect(parseMoney('1.234,56')).toBe(1234.56);
    expect(parseMoney('80,5')).toBe(80.5);
    expect(parseMoney('80.50')).toBe(8050); // ponto e separador de milhar no Brasil
  });

  it('calcula a variação contra o mês anterior', () => {
    expect(percentDelta(120, 100)).toBe(20);
    expect(percentDelta(50, 100)).toBe(-50);
    expect(percentDelta(50, 0)).toBeNull();
  });

  it('formata dinheiro e data para pt-BR', () => {
    expect(money(1234.5).replace(/ /g, ' ')).toBe('R$ 1.234,50');
    expect(shortDate('2026-09-16')).toBe('16/09/2026');
  });
});
