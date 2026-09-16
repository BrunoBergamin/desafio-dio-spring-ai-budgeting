import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import type { CategorySummary } from '../api/types';
import { CATEGORY_COLORS, money } from '../utils/format';
import { EmptyState } from '../ui/primitives';

export function CategoryChart({ data, height = 260 }: { data: CategorySummary[]; height?: number }) {
  if (data.length === 0) {
    return <EmptyState icon="📭" title="Nenhum gasto neste período" hint="Fale com a Lumi ou registre um gasto para o gráfico aparecer." />;
  }
  return (
    <ResponsiveContainer width="100%" height={height}>
      <PieChart>
        <Pie data={data} dataKey="total" nameKey="categoryLabel" innerRadius="58%" outerRadius="88%" paddingAngle={2} stroke="none">
          {data.map((entry) => (
            <Cell key={entry.category} fill={CATEGORY_COLORS[entry.category]} />
          ))}
        </Pie>
        <Tooltip
          formatter={(value, _name, item) => [money(Number(value)), String(item?.payload?.categoryLabel ?? '')]}
          contentStyle={{ background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 10, color: 'var(--text)' }}
          itemStyle={{ color: 'var(--text)' }}
        />
        <Legend iconType="circle" formatter={(value) => <span style={{ color: 'var(--text-2)' }}>{value}</span>} />
      </PieChart>
    </ResponsiveContainer>
  );
}
