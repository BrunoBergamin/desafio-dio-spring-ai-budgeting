package dio.budgeting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fechamento do mes: o que entrou, o que saiu, o que sobrou e onde o dinheiro foi parar.
 * Serve ao card do painel e a ferramenta que a Lumi usa para narrar o mes.
 *
 * @param expensesDeltaPercentage variacao dos gastos contra o mes anterior; nulo quando nao havia gasto antes
 */
public record MonthlyReportResponse(
        @Schema(example = "2026-09") String month,
        LocalDate start,
        LocalDate end,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance,
        long quantity,
        BigDecimal previousExpenses,
        @Schema(example = "-12.5") BigDecimal expensesDeltaPercentage,
        @Schema(description = "As três categorias em que mais se gastou") List<CategorySummaryResponse> topCategories,
        @Schema(description = "Os cinco maiores gastos do mês") List<TransactionResponse> topExpenses,
        List<BudgetStatusResponse> budgets,
        @Schema(description = "Quantos orçamentos estão em atenção ou estourados") int budgetsInAlert,
        List<SavingsGoalResponse> goals) {
}
