package dio.budgeting.service;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.dto.response.TransactionRegisteredResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registra um gasto e ja devolve a situacao do orcamento da categoria.
 * Orquestra os dois services sem acoplar um ao outro; usado pelo REST e pela ferramenta da Lumi.
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final TransactionService transactionService;
    private final BudgetService budgetService;

    @Transactional
    public TransactionRegisteredResponse register(UUID userId, TransactionRequest request) {
        var transaction = transactionService.create(userId, request);
        var budget = budgetService.checkAfterExpense(userId, transaction.category(), transaction.date());
        return new TransactionRegisteredResponse(transaction, budget.orElse(null));
    }
}
