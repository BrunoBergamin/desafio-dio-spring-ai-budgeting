import type { Category } from '../api/types';

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export const money = (value: number) => brl.format(value);

export const shortDate = (iso: string) => {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

export const today = () => new Date().toISOString().slice(0, 10);

export const currentMonth = () => today().slice(0, 7);

export const CATEGORIES: { value: Category; label: string; emoji: string }[] = [
  { value: 'GROCERIES', label: 'Mercado', emoji: '🛒' },
  { value: 'PHARMA', label: 'Farmácia e saúde', emoji: '💊' },
  { value: 'AUTO', label: 'Carro e combustível', emoji: '🚗' },
  { value: 'RESTAURANT', label: 'Restaurantes e delivery', emoji: '🍕' },
  { value: 'TRANSPORT', label: 'Transporte', emoji: '🚌' },
  { value: 'HOUSING', label: 'Moradia e contas', emoji: '🏠' },
  { value: 'LEISURE', label: 'Lazer', emoji: '🎬' },
  { value: 'EDUCATION', label: 'Educação', emoji: '📚' },
  { value: 'OTHER', label: 'Outros', emoji: '🧾' },
];

export const categoryEmoji = (category: Category) =>
  CATEGORIES.find((c) => c.value === category)?.emoji ?? '🧾';

/** Cores fixas por categoria, para o grafico e as etiquetas ficarem consistentes. */
export const CATEGORY_COLORS: Record<Category, string> = {
  GROCERIES: '#2dd4bf',
  PHARMA: '#f472b6',
  AUTO: '#fbbf24',
  RESTAURANT: '#fb923c',
  TRANSPORT: '#60a5fa',
  HOUSING: '#a78bfa',
  LEISURE: '#34d399',
  EDUCATION: '#c084fc',
  OTHER: '#94a3b8',
};
