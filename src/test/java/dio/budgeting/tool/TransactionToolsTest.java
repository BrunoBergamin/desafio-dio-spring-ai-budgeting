package dio.budgeting.tool;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.entity.Category;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.support.ToolCallbacks;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionToolsTest {

    @Mock
    TransactionService transactionService;

    @InjectMocks
    TransactionTools tools;

    @Test
    void should_exposeAllToolsToTheModel() {
        var names = java.util.Arrays.stream(ToolCallbacks.from(tools))
                .map(callback -> callback.getToolDefinition().name())
                .toList();

        assertThat(names).containsExactlyInAnyOrder(
                "registrar_transacao", "listar_transacoes", "ultimas_transacoes", "resumo_de_gastos");
    }

    @Test
    void should_delegateToService_when_registeringTransaction() {
        tools.registerTransaction("Farmácia", new BigDecimal("42.90"), Category.PHARMA, "2026-09-14");

        var captor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(transactionService).create(captor.capture());
        assertThat(captor.getValue().date()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(captor.getValue().amount()).isEqualByComparingTo("42.90");
    }

    @Test
    void should_useNullDate_when_modelOmitsIt() {
        tools.spendingSummary("", null);

        verify(transactionService).summary(null, null);
    }

    @Test
    void should_explainError_when_dateIsInvalid() {
        assertThatThrownBy(() -> tools.listTransactions(null, "ontem", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AAAA-MM-DD");
    }
}
