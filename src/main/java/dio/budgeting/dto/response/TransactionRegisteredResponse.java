package dio.budgeting.dto.response;

/**
 * Resultado de registrar um gasto: a transação e, se existir orçamento para a categoria no mês,
 * a situação dele já recalculada. {@code budget} é nulo quando não há orçamento.
 */
public record TransactionRegisteredResponse(TransactionResponse transaction, BudgetStatusResponse budget) {
}
