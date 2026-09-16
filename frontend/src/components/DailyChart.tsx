import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { TransactionResponse } from '../api/types';
import { money } from '../utils/format';

/** Gastos por dia do mes, somados no cliente a partir da lista de transacoes. */
export function DailyChart({ transactions, month }: { transactions: TransactionResponse[]; month: string }) {
  const [y, m] = month.split('-').map(Number);
  const days = new Date(y, m, 0).getDate();
  const totals = new Array(days).fill(0);
  for (const t of transactions) {
    if (t.date.startsWith(month)) totals[Number(t.date.slice(8, 10)) - 1] += t.amount;
  }
  const data = totals.map((total, i) => ({ day: String(i + 1).padStart(2, '0'), total }));

  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid vertical={false} stroke="var(--border)" />
        <XAxis dataKey="day" tick={{ fill: 'var(--text-2)', fontSize: 11 }} interval={4} axisLine={false} tickLine={false} />
        <YAxis tick={{ fill: 'var(--text-2)', fontSize: 11 }} axisLine={false} tickLine={false} width={48}
               tickFormatter={(v) => (v >= 1000 ? `${Math.round(v / 1000)}k` : String(v))} />
        <Tooltip
          cursor={{ fill: 'rgba(124, 92, 255, 0.12)' }}
          formatter={(value) => [money(Number(value)), 'gasto no dia']}
          labelFormatter={(label) => `dia ${label}`}
          contentStyle={{ background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 10, color: 'var(--text)' }}
        />
        <Bar dataKey="total" fill="var(--accent)" radius={[6, 6, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
