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
  { value: 'RESTAURANT', label: 'Restaurantes e delivery', emoji: '🍕' },
  { value: 'PHARMA', label: 'Farmácia e saúde', emoji: '💊' },
  { value: 'HOUSING', label: 'Moradia e contas de casa', emoji: '🏠' },
  { value: 'TRANSPORT', label: 'Transporte', emoji: '🚌' },
  { value: 'AUTO', label: 'Carro e combustível', emoji: '🚗' },
  { value: 'SUBSCRIPTIONS', label: 'Assinaturas e streaming', emoji: '📺' },
  { value: 'CLOTHING', label: 'Roupas e acessórios', emoji: '👕' },
  { value: 'PERSONAL_CARE', label: 'Beleza e cuidados pessoais', emoji: '💇' },
  { value: 'LEISURE', label: 'Lazer', emoji: '🎬' },
  { value: 'EDUCATION', label: 'Educação', emoji: '📚' },
  { value: 'PETS', label: 'Pets', emoji: '🐶' },
  { value: 'TRAVEL', label: 'Viagem', emoji: '✈️' },
  { value: 'GIFTS', label: 'Presentes e doações', emoji: '🎁' },
  { value: 'TAXES', label: 'Impostos e taxas', emoji: '🏛️' },
  { value: 'OTHER', label: 'Outros', emoji: '🧾' },
];

export const categoryEmoji = (category: Category) =>
  CATEGORIES.find((c) => c.value === category)?.emoji ?? '🧾';

/** Cores fixas por categoria, para o grafico e as etiquetas ficarem consistentes. */
export const CATEGORY_COLORS: Record<Category, string> = {
  GROCERIES: '#2dd4bf',
  RESTAURANT: '#fb923c',
  PHARMA: '#f472b6',
  HOUSING: '#a78bfa',
  TRANSPORT: '#60a5fa',
  AUTO: '#fbbf24',
  SUBSCRIPTIONS: '#e879f9',
  CLOTHING: '#f9a8d4',
  PERSONAL_CARE: '#fda4af',
  LEISURE: '#34d399',
  EDUCATION: '#c084fc',
  PETS: '#fcd34d',
  TRAVEL: '#38bdf8',
  GIFTS: '#f87171',
  TAXES: '#9ca3af',
  OTHER: '#94a3b8',
};
