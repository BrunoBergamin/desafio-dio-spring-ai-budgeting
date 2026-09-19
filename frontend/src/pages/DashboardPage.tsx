import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useBudgets, useSummary, useTransactions } from '../hooks/useFinance';
import { CategoryChart } from '../components/CategoryChart';
import { DailyChart } from '../components/DailyChart';
import { BudgetBar } from '../components/BudgetBar';
import { EmptyState, Skeleton, Stat } from '../ui/primitives';
import { categoryEmoji, currentMonth, money, monthLabel, monthRange, percentDelta, shiftMonth, shortDate } from '../utils/format';

export function DashboardPage() {
  const [month, setMonth] = useState(currentMonth());
  const range = monthRange(month);
  const previous = monthRange(shiftMonth(month, -1));
  const summary = useSummary(range.start, range.end);
  const before = useSummary(previous.start, previous.end);
  const budgets = useBudgets(month);
  // O grafico diario precisa do mes inteiro: uma pagina grande (o teto da API e 500)
  const transactions = useTransactions({ start: range.start, end: range.end, size: 500 });

  const total = summary.data?.total ?? 0;
  const income = summary.data?.income ?? 0;
  const balance = summary.data?.balance ?? 0;
  const delta = before.data ? percentDelta(total, before.data.total) : null;
  const top = summary.data?.categories[0];
  const exceeded = budgets.data?.filter((b) => b.status !== 'OK').length ?? 0;
  const monthTransactions = transactions.data?.content ?? [];
  const expensesOfMonth = monthTransactions.filter((t) => t.type === 'EXPENSE');
  const recent = monthTransactions.slice(0, 6);
  const isCurrent = month === currentMonth();

  return (
    <div className="stack-lg">
      <div className="page-head">
        <div>
          <h1 className="page-title">Painel</h1>
          <p className="muted">Como está o seu mês, em um olhar.</p>
        </div>
        <div className="month-nav" role="group" aria-label="mês">
          <button className="btn ghost icon" onClick={() => setMonth(shiftMonth(month, -1))} aria-label="mês anterior">‹</button>
          <span className="month-label">{monthLabel(month)}</span>
          <button className="btn ghost icon" onClick={() => setMonth(shiftMonth(month, 1))} aria-label="próximo mês" disabled={isCurrent}>›</button>
        </div>
      </div>

      <div className="stats">
        <Stat label="Recebido no mês" value={money(income)} hint={income === 0 ? 'nenhuma receita registrada' : 'salário, freela e rendimentos'} />
        <Stat label="Gasto no mês" value={money(total)}
              hint={delta === null ? 'sem mês anterior para comparar' : <span className={delta > 0 ? 'delta up' : 'delta down'}>{delta > 0 ? '▲' : '▼'} {Math.abs(delta)}% vs. mês anterior</span>} />
        <Stat label="Saldo do mês" value={money(balance)} tone={balance < 0 ? 'danger' : 'ok'}
              hint={balance < 0 ? 'você gastou mais do que entrou' : 'o que sobrou do que entrou'} />
        <Stat label="Maior categoria" value={top ? `${categoryEmoji(top.category)} ${top.categoryLabel}` : '-'} hint={top ? `${money(top.total)} · ${top.percentage}%` : undefined} />
        <Stat label="Orçamentos" value={budgets.data ? `${budgets.data.length - exceeded}/${budgets.data.length} ok` : '-'}
              hint={exceeded ? `${exceeded} em atenção ou estourado` : 'tudo dentro do limite'} tone={exceeded ? 'warning' : 'ok'} />
      </div>

      <div className="grid-two-even">
        <section className="card">
          <div className="card-head"><h3>Por categoria</h3></div>
          {summary.isLoading ? <Skeleton lines={5} height={18} /> : <CategoryChart data={summary.data?.categories ?? []} height={280} />}
        </section>
        <section className="card">
          <div className="card-head"><h3>Por dia</h3></div>
          {transactions.isLoading ? <Skeleton lines={5} height={18} /> : <DailyChart transactions={expensesOfMonth} month={month} />}
        </section>
      </div>

      <div className="grid-two-even">
        <section className="card">
          <div className="card-head">
            <h3>Últimos lançamentos</h3>
            <Link className="btn ghost small" to="/transacoes">ver todos</Link>
          </div>
          {transactions.isLoading ? <Skeleton lines={6} /> : recent.length === 0 ? (
            <EmptyState icon="🧾" title="Nada registrado neste mês" action={<Link className="btn primary small" to="/conversa">falar com a Lumi</Link>} />
          ) : (
            <ul className="list">
              {recent.map((t) => (
                <li key={t.id} className="list-row">
                  <span className="list-icon" aria-hidden>{categoryEmoji(t.category)}</span>
                  <span className="list-main">
                    <span>{t.description}</span>
                    <span className="muted small">{t.categoryLabel} · {shortDate(t.date)}</span>
                  </span>
                  <span className={`list-amount ${t.type === 'INCOME' ? 'income' : ''}`}>
                    {t.type === 'INCOME' ? '+' : ''}{money(t.amount)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
        <section className="card">
          <div className="card-head">
            <h3>Orçamentos do mês</h3>
            <Link className="btn ghost small" to="/orcamentos">gerenciar</Link>
          </div>
          {budgets.isLoading ? <Skeleton lines={4} height={22} /> : (budgets.data?.length ?? 0) === 0 ? (
            <EmptyState icon="🎯" title="Sem limites definidos" hint='Diga à Lumi: "meu limite de mercado é 800".' />
          ) : (
            <div className="stack">{budgets.data!.map((b) => <BudgetBar key={b.id} budget={b} />)}</div>
          )}
        </section>
      </div>
    </div>
  );
}
