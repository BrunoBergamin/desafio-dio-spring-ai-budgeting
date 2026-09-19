package dio.budgeting.service;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.dto.response.BudgetStatusResponse;
import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.BudgetStatus;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    static final UUID USER = UUID.randomUUID();

    @Mock TransactionService transactionService;
    @Mock BudgetService budgetService;
    @InjectMocks ExpenseService service;

    @Test
    void should_returnBudgetStatus_when_categoryHasBudget() {
        var request = new TransactionRequest("Mercado", new BigDecimal("50"), Category.GROCERIES, LocalDate.of(2026, 9, 10));
        var created = new TransactionResponse(UUID.randomUUID(), "Mercado", new BigDecimal("50"), Category.GROCERIES, "Mercado", TransactionType.EXPENSE, LocalDate.of(2026, 9, 10));
        when(transactionService.create(USER, request)).thenReturn(created);
        when(budgetService.checkAfterExpense(USER, Category.GROCERIES, LocalDate.of(2026, 9, 10)))
                .thenReturn(Optional.of(status(BudgetStatus.EXCEEDED)));

        var result = service.register(USER, request);

        assertThat(result.transaction()).isEqualTo(created);
        assertThat(result.budget().status()).isEqualTo(BudgetStatus.EXCEEDED);
    }

    @Test
    void should_returnNullBudget_when_categoryHasNoBudget() {
        var request = new TransactionRequest("Uber", new BigDecimal("20"), Category.TRANSPORT, null);
        when(transactionService.create(any(), any())).thenReturn(
                new TransactionResponse(UUID.randomUUID(), "Uber", new BigDecimal("20"), Category.TRANSPORT, "Transporte", TransactionType.EXPENSE, LocalDate.now()));
        when(budgetService.checkAfterExpense(any(), any(), any())).thenReturn(Optional.empty());

        assertThat(service.register(USER, request).budget()).isNull();
    }

    @Test
    void should_notLookForBudget_when_theTransactionIsIncome() {
        var request = new TransactionRequest("Salário", new BigDecimal("5200"), Category.SALARY, null);
        when(transactionService.create(USER, request)).thenReturn(new TransactionResponse(
                UUID.randomUUID(), "Salário", new BigDecimal("5200"), Category.SALARY, "Salário",
                TransactionType.INCOME, LocalDate.of(2026, 9, 5)));

        var result = service.register(USER, request);

        assertThat(result.budget()).isNull();
        assertThat(result.transaction().type()).isEqualTo(TransactionType.INCOME);
        verifyNoInteractions(budgetService);
    }

    private BudgetStatusResponse status(BudgetStatus status) {
        return new BudgetStatusResponse(UUID.randomUUID(), Category.GROCERIES, "Mercado", "2026-09",
                new BigDecimal("500"), new BigDecimal("550"), new BigDecimal("-50"), new BigDecimal("110.0"),
                status, status.getLabel(), "estourou");
    }
}
