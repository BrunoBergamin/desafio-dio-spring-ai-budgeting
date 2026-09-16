import type { Category } from '../api/types';

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export const money = (value: number) => brl.format(value);

export const shortDate = (iso: string) => {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

/** Data local em ISO (AAAA-MM-DD), sem o deslocamento de fuso do toISOString. */
export const today = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

export const currentMonth = () => today().slice(0, 7);

/** "2026-09" -> { start: "2026-09-01", end: "2026-09-30" } */
export const monthRange = (month: string) => {
  const [y, m] = month.split('-').map(Number);
  const last = new Date(y, m, 0).getDate();
  return { start: `${month}-01`, end: `${month}-${String(last).padStart(2, '0')}` };
};

export const shiftMonth = (month: string, delta: number) => {
  const [y, m] = month.split('-').map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
};

export const monthLabel = (month: string) => {
  const [y, m] = month.split('-').map(Number);
  const label = new Date(y, m - 1, 1).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  return label.charAt(0).toUpperCase() + label.slice(1); // "Setembro de 2026"
};

export const timeNow = () => new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

export const percentDelta = (current: number, previous: number) =>
  previous === 0 ? null : Math.round(((current - previous) / previous) * 100);

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

/** Aceita o que o Whisper aceita, inclusive os .ogg/.opus das notas de voz do WhatsApp. */
export const AUDIO_ACCEPT = 'audio/*,.ogg,.opus,.oga,.m4a,.mp3,.wav,.webm,.mp4,.flac';

export const parseMoney = (value: string) => Number(value.replace(/\./g, '').replace(',', '.'));
