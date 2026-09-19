package dio.budgeting.tool;

import dio.budgeting.dto.request.RecurringRequest;
import dio.budgeting.entity.Category;
import dio.budgeting.service.RecurringTransactionService;
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
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecurringToolsTest {

    static final UUID ME = UUID.randomUUID();

    @Mock RecurringTransactionService recurringService;
    @InjectMocks RecurringTools tools;

    @Test
    void should_exposeOnlyTwoToolsToTheModel() {
        var names = Arrays.stream(ToolCallbacks.from(tools))
                .map(callback -> callback.getToolDefinition().name())
                .toList();

        // Pausar e remover ficam so no site: cada ferramenta a mais custa tokens em toda conversa
        assertThat(names).containsExactlyInAnyOrder("criar_recorrente", "listar_recorrentes");
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
    void should_useUserFromToolContext_when_creatingARule() {
        tools.createRecurring("Aluguel", new BigDecimal("1500.00"), Category.HOUSING, 10, context(ME));

        var captor = ArgumentCaptor.forClass(RecurringRequest.class);
        verify(recurringService).create(eq(ME), captor.capture());
        assertThat(captor.getValue().dayOfMonth()).isEqualTo(10);
        assertThat(captor.getValue().category()).isEqualTo(Category.HOUSING);
        // A Lumi nao escolhe mes inicial nem final: comeca agora e nao tem prazo para acabar
        assertThat(captor.getValue().startMonth()).isNull();
        assertThat(captor.getValue().endMonth()).isNull();
    }

    @Test
    void should_failFast_when_toolContextHasNoUser() {
        assertThatThrownBy(() -> tools.listRecurring(new ToolContext(Map.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sem usuário");
    }

    private ToolContext context(UUID userId) {
        return new ToolContext(Map.of(ToolUser.USER_ID, userId));
    }
}
