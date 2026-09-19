package dio.budgeting.tool;

import dio.budgeting.dto.request.GoalRequest;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.service.SavingsGoalService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoalToolsTest {

    static final UUID ME = UUID.randomUUID();

    @Mock SavingsGoalService goalService;
    @InjectMocks GoalTools tools;

    @Test
    void should_exposeTheThreeGoalToolsToTheModel() {
        var names = Arrays.stream(ToolCallbacks.from(tools))
                .map(callback -> callback.getToolDefinition().name())
                .toList();

        assertThat(names).containsExactlyInAnyOrder("criar_meta", "guardar_na_meta", "consultar_metas");
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
    void should_useUserFromToolContext_when_creatingAGoal() {
        tools.createGoal("Viagem", new BigDecimal("6000.00"), "2027-01-31", context(ME));

        var captor = ArgumentCaptor.forClass(GoalRequest.class);
        verify(goalService).create(eq(ME), captor.capture());
        assertThat(captor.getValue().deadline()).isEqualTo(LocalDate.of(2027, 1, 31));
        assertThat(captor.getValue().savedAmount()).isNull();
    }

    @Test
    void should_acceptAGoalWithoutDeadline_when_theModelOmitsIt() {
        tools.createGoal("Reserva", new BigDecimal("10000.00"), "", context(ME));

        var captor = ArgumentCaptor.forClass(GoalRequest.class);
        verify(goalService).create(eq(ME), captor.capture());
        assertThat(captor.getValue().deadline()).isNull();
    }

    @Test
    void should_depositByName_when_thePersonSaysTheyPutMoneyAside() {
        tools.saveIntoGoal("Viagem", new BigDecimal("300.00"), context(ME));

        verify(goalService).depositByName(ME, "Viagem", new BigDecimal("300.00"));
    }

    @Test
    void should_explainError_when_theDeadlineIsNotADate() {
        assertThatThrownBy(() -> tools.createGoal("Viagem", new BigDecimal("6000"), "ano que vem", context(ME)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AAAA-MM-DD");
    }

    @Test
    void should_failFast_when_toolContextHasNoUser() {
        assertThatThrownBy(() -> tools.listGoals(new ToolContext(Map.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sem usuário");
    }

    private ToolContext context(UUID userId) {
        return new ToolContext(Map.of(ToolUser.USER_ID, userId));
    }
}
