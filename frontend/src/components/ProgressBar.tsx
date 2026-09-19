/**
 * Barra de progresso usada pelo orçamento e pelas metas. O tom vem de fora, porque o mesmo
 * percentual significa coisas opostas nos dois casos: 90% do limite é alerta, 90% da meta é ótimo.
 */
export function ProgressBar({ percentage, label, tone = 'ok' }: {
  percentage: number;
  label: string;
  tone?: 'ok' | 'warning' | 'danger';
}) {
  // Mínimo de 2% para a barra não sumir quando o valor é zero, e teto de 100% quando passa do alvo
  const width = Math.min(100, Math.max(2, percentage));
  return (
    <div className={`bar tone-${tone}`} role="progressbar" aria-valuenow={Math.round(percentage)}
         aria-valuemin={0} aria-valuemax={100} aria-label={label}>
      <div className="bar-fill" style={{ width: `${width}%` }} />
    </div>
  );
}
