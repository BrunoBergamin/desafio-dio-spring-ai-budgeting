package dio.budgeting.tool;

import dio.budgeting.dto.request.BudgetRequest;
import dio.budgeting.entity.Category;
import dio.budgeting.service.BudgetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BudgetToolsTest {

    static final UUID ME = UUID.randomUUID();

    @Mock BudgetService budgetService;
    @InjectMocks BudgetTools tools;

    @Test
    void should_exposeBudgetToolsToTheModel() {
        var names = Arrays.stream(ToolCallbacks.from(tools)).map(c -> c.getToolDefinition().name()).toList();

        assertThat(names).containsExactlyInAnyOrder(
                "definir_orcamento", "consultar_orcamentos", "status_do_orcamento", "alertas_de_orcamento");
    }

    @Test
    void should_upsertForTheContextUser_when_definingBudget() {
        tools.defineBudget(Category.GROCERIES, new BigDecimal("800"), " 2026-09 ", context());

        var captor = ArgumentCaptor.forClass(BudgetRequest.class);
        verify(budgetService).upsert(eq(ME), captor.capture());
        assertThat(captor.getValue().month()).isEqualTo("2026-09");
        assertThat(captor.getValue().monthlyLimit()).isEqualByComparingTo("800");
    }

    @Test
    void should_treatBlankMonthAsCurrent_when_listing() {
        tools.listBudgets("", context());

        verify(budgetService).list(ME, null);
    }

    private ToolContext context() {
        return new ToolContext(Map.of(ToolUser.USER_ID, ME));
    }
}
