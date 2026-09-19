package dio.budgeting.tool;

import dio.budgeting.dto.request.RecurringRequest;
import dio.budgeting.dto.response.RecurringResponse;
import dio.budgeting.entity.Category;
import dio.budgeting.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contas recorrentes para a Lumi. Sao so duas ferramentas: criar e listar. Pausar e remover ficam no site,
 * porque cada ferramenta a mais entra no prompt de toda conversa e custa tokens em todas as chamadas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTools {

    private final RecurringTransactionService recurringService;

    @Tool(name = "criar_recorrente",
            description = "Cadastra uma conta que se repete todo mês (aluguel, streaming, academia, salário). "
                    + "A partir daí o lançamento de cada mês é criado sozinho no dia informado, sem a pessoa pedir")
    public RecurringResponse createRecurring(
            @ToolParam(description = "Descrição da conta, ex.: 'Aluguel' ou 'Netflix'") String description,
            @ToolParam(description = "Valor em reais, ex.: 1500.00") BigDecimal amount,
            @ToolParam(description = "Categoria, que também diz se é gasto ou receita. Guia: " + Category.GUIDE) Category category,
            @ToolParam(description = "Dia do mês em que cai, de 1 a 31") Integer dayOfMonth,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] criar_recorrente user={} description='{}' amount={} category={} dia={}",
                userId, description, amount, category, dayOfMonth);
        return recurringService.create(userId,
                new RecurringRequest(description, amount, category, dayOfMonth, null, null, null));
    }

    @Tool(name = "listar_recorrentes",
            description = "Lista as contas que se repetem todo mês, com o dia de cada uma e a próxima data")
    public List<RecurringResponse> listRecurring(ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] listar_recorrentes user={}", userId);
        return recurringService.list(userId);
    }
}
