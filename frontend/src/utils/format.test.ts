import { describe, expect, it } from 'vitest';
import { CATEGORIES, CATEGORY_COLORS, categoriesOf, monthRange, parseMoney, percentDelta, shiftMonth, money, shortDate } from './format';

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
    // O Intl separa "R$" do valor com um espaco sem quebra; o \s cobre os dois tipos de espaco
    expect(money(1234.5).replace(/\s/g, ' ')).toBe('R$ 1.234,50');
    expect(shortDate('2026-09-16')).toBe('16/09/2026');
  });

  it('separa as categorias de gasto das de receita', () => {
    const income = categoriesOf('INCOME').map((c) => c.value);
    const expense = categoriesOf('EXPENSE').map((c) => c.value);

    expect(income).toEqual(['SALARY', 'FREELANCE', 'INVESTMENTS', 'OTHER_INCOME']);
    expect(expense).toContain('GROCERIES');
    expect(expense).not.toContain('SALARY');
    // Toda categoria precisa de cor, senao o grafico e as etiquetas ficam sem nada
    for (const c of CATEGORIES) expect(CATEGORY_COLORS[c.value]).toBeDefined();
  });
});
