package dio.budgeting.tool;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.entity.Category;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.service.ExpenseService;
import dio.budgeting.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionToolsTest {

    static final UUID ME = UUID.randomUUID();
    static final UUID SOMEONE_ELSE = UUID.randomUUID();

    @Mock TransactionService transactionService;
    @Mock ExpenseService expenseService;
    @InjectMocks TransactionTools tools;

    @Test
    void should_exposeAllToolsToTheModel() {
        var names = Arrays.stream(ToolCallbacks.from(tools))
                .map(callback -> callback.getToolDefinition().name())
                .toList();

        assertThat(names).containsExactlyInAnyOrder(
                "registrar_transacao", "listar_transacoes", "ultimas_transacoes", "resumo_de_gastos");
    }

    @Test
    void should_notExposeUserIdInTheToolSchema() {
        for (ToolCallback callback : ToolCallbacks.from(tools)) {
            assertThat(callback.getToolDefinition().inputSchema())
                    .doesNotContain("userId")
                    .doesNotContain("toolContext");
        }
    }

    @Test
    void should_useUserFromToolContext_when_registeringTransaction() {
        tools.registerTransaction("Farmácia", new BigDecimal("42.90"), Category.PHARMA, "2026-09-14", context(ME));

        var captor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(expenseService).register(eq(ME), captor.capture());
        assertThat(captor.getValue().date()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(captor.getValue().amount()).isEqualByComparingTo("42.90");
    }

    @Test
    void should_ignoreUserIdSentByTheModel_when_callingTheTool() {
        var callback = Arrays.stream(ToolCallbacks.from(tools))
                .filter(c -> c.getToolDefinition().name().equals("registrar_transacao"))
                .findFirst().orElseThrow();

        // O modelo "inventa" um userId nos argumentos: ele e ignorado, vale o do contexto
        callback.call("""
                {"description":"Mercado","amount":80.50,"category":"GROCERIES","userId":"%s"}
                """.formatted(SOMEONE_ELSE), context(ME));

        verify(expenseService).register(eq(ME), any());
    }

    @Test
    void should_failFast_when_toolContextHasNoUser() {
        assertThatThrownBy(() -> tools.recentTransactions(new ToolContext(Map.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sem usuário");
    }

    @Test
    void should_useNullDate_when_modelOmitsIt() {
        tools.spendingSummary("", null, context(ME));

        verify(transactionService).summary(ME, null, null);
    }

    @Test
    void should_explainError_when_dateIsInvalid() {
        assertThatThrownBy(() -> tools.listTransactions(null, "ontem", null, context(ME)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AAAA-MM-DD");
    }

    private ToolContext context(UUID userId) {
        return new ToolContext(Map.of(ToolUser.USER_ID, userId));
    }
}
