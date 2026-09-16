package dio.budgeting.dto.response;

import dio.budgeting.entity.BudgetStatus;
import dio.budgeting.entity.Category;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Orçamento de uma categoria em um mês, com quanto já foi gasto.
 *
 * @param message frase pronta para a Lumi repetir (ex.: "você já usou 82% do limite de Mercado")
 */
public record BudgetStatusResponse(UUID id,
                                   Category category,
                                   String categoryLabel,
                                   String month,
                                   BigDecimal monthlyLimit,
                                   BigDecimal spent,
                                   BigDecimal remaining,
                                   BigDecimal usedPercentage,
                                   BudgetStatus status,
                                   String statusLabel,
                                   String message) {
}
