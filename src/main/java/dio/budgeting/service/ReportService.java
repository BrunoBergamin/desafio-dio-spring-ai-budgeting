package dio.budgeting.service;

import dio.budgeting.dto.response.MonthlyReportResponse;
import dio.budgeting.entity.BudgetStatus;
import dio.budgeting.entity.TransactionType;
import dio.budgeting.mapper.TransactionMapper;
import dio.budgeting.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Junta num objeto so tudo o que conta a historia do mes: entrou, saiu, sobrou, onde foi parar,
 * como ficaram os orcamentos e as metas. Nao guarda nada: le dos outros services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int TOP_CATEGORIES = 3;
    private static final int TOP_EXPENSES = 5;

    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final SavingsGoalService goalService;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MonthlyReportResponse monthly(UUID userId, String month) {
        var start = MonthParser.parse(month, clock);
        var end = start.plusMonths(1).minusDays(1);
        var previousStart = start.minusMonths(1);

        var summary = transactionService.summary(userId, start, end);
        var previous = transactionService.summary(userId, previousStart, start.minusDays(1));

        var topExpenses = transactionRepository
                .search(userId, TransactionType.EXPENSE, null, start, end,
                        PageRequest.of(0, TOP_EXPENSES, Sort.by(Sort.Order.desc("amount"))))
                .map(transactionMapper::toResponse)
                .getContent();

        var budgets = budgetService.list(userId, MonthParser.format(start));
        var inAlert = (int) budgets.stream().filter(b -> b.status() != BudgetStatus.OK).count();

        return new MonthlyReportResponse(
                MonthParser.format(start), start, end,
                summary.income(), summary.total(), summary.balance(), summary.quantity(),
                previous.total(), delta(summary.total(), previous.total()),
                summary.categories().stream().limit(TOP_CATEGORIES).toList(),
                topExpenses,
                budgets, inAlert,
                goalService.list(userId));
    }

    /** Quanto os gastos subiram ou caíram contra o mes anterior. Nulo quando nao havia gasto para comparar. */
    private BigDecimal delta(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    /** Periodo do mes, para o controller montar o nome do arquivo CSV. */
    LocalDate monthStart(String month) {
        return MonthParser.parse(month, clock);
    }
}
