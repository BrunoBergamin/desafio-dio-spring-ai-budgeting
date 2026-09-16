package dio.budgeting.tool;

import dio.budgeting.dto.request.BudgetRequest;
import dio.budgeting.dto.response.BudgetStatusResponse;
import dio.budgeting.entity.Category;
import dio.budgeting.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Ferramentas de orcamento. Remover orcamento fica so no REST: cada tool a mais custa tokens em toda chamada. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetTools {

    private final BudgetService budgetService;

    @Tool(name = "definir_orcamento", description = "Define ou atualiza o limite mensal de gastos de uma categoria")
    public BudgetStatusResponse defineBudget(
            @ToolParam(description = "Categoria do orçamento") Category category,
            @ToolParam(description = "Limite mensal em reais, ex.: 800.00") BigDecimal monthlyLimit,
            @ToolParam(description = "Mês no formato AAAA-MM. Omita para o mês atual", required = false) String month,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] definir_orcamento user={} category={} limit={} month={}", userId, category, monthlyLimit, month);
        return budgetService.upsert(userId, new BudgetRequest(category, monthlyLimit, blankToNull(month)));
    }

    @Tool(name = "consultar_orcamentos", description = "Lista os orçamentos do mês com quanto já foi gasto e quanto resta")
    public List<BudgetStatusResponse> listBudgets(
            @ToolParam(description = "Mês no formato AAAA-MM. Omita para o mês atual", required = false) String month,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] consultar_orcamentos user={} month={}", userId, month);
        return budgetService.list(userId, blankToNull(month));
    }

    @Tool(name = "status_do_orcamento", description = "Mostra quanto já foi usado do limite de uma categoria")
    public BudgetStatusResponse budgetStatus(
            @ToolParam(description = "Categoria do orçamento") Category category,
            @ToolParam(description = "Mês no formato AAAA-MM. Omita para o mês atual", required = false) String month,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] status_do_orcamento user={} category={} month={}", userId, category, month);
        return budgetService.status(userId, category, blankToNull(month));
    }

    @Tool(name = "alertas_de_orcamento", description = "Lista as categorias que já passaram de 80% do limite no mês")
    public List<BudgetStatusResponse> budgetAlerts(
            @ToolParam(description = "Mês no formato AAAA-MM. Omita para o mês atual", required = false) String month,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] alertas_de_orcamento user={} month={}", userId, month);
        return budgetService.alerts(userId, blankToNull(month));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
