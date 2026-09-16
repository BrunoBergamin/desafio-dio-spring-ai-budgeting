package dio.budgeting.tool;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.dto.response.SpendingSummaryResponse;
import dio.budgeting.dto.response.TransactionRegisteredResponse;
import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.Category;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.service.ExpenseService;
import dio.budgeting.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Ferramentas expostas ao modelo via Tool Calling.
 * <p>
 * Esta classe é só um adaptador: traduz os parâmetros vindos da IA e delega para os services,
 * onde ficam as regras de negócio. O usuário dono dos dados vem do {@link ToolContext}, nunca do modelo.
 * Se o service lançar exceção, o Spring AI devolve a mensagem de erro para o modelo, que explica o problema.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionTools {

    private final TransactionService transactionService;
    private final ExpenseService expenseService;

    @Tool(name = "registrar_transacao",
            description = "Registra um novo gasto financeiro. Devolve a transação e, se houver orçamento para a "
                    + "categoria, um alerta de quanto do limite do mês já foi usado")
    public TransactionRegisteredResponse registerTransaction(
            @ToolParam(description = "Descrição curta do gasto, ex.: 'Compras no mercado'") String description,
            @ToolParam(description = "Valor do gasto em reais, ex.: 80.50") BigDecimal amount,
            @ToolParam(description = "Categoria do gasto") Category category,
            @ToolParam(description = "Data do gasto no formato AAAA-MM-DD. Omita se for hoje", required = false) String date,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] registrar_transacao user={} description='{}' amount={} category={} date={}",
                userId, description, amount, category, date);
        return expenseService.register(userId, new TransactionRequest(description, amount, category, parseDate(date)));
    }

    @Tool(name = "listar_transacoes", description = "Lista gastos, com filtros opcionais de categoria e período")
    public List<TransactionResponse> listTransactions(
            @ToolParam(description = "Categoria para filtrar", required = false) Category category,
            @ToolParam(description = "Data inicial AAAA-MM-DD", required = false) String start,
            @ToolParam(description = "Data final AAAA-MM-DD", required = false) String end,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] listar_transacoes user={} category={} start={} end={}", userId, category, start, end);
        return transactionService.list(userId, category, parseDate(start), parseDate(end));
    }

    @Tool(name = "ultimas_transacoes", description = "Retorna as 5 transações mais recentes")
    public List<TransactionResponse> recentTransactions(ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] ultimas_transacoes user={}", userId);
        return transactionService.recent(userId);
    }

    @Tool(name = "resumo_de_gastos",
            description = "Calcula o total gasto em um período, agrupado por categoria, com percentual de cada uma. "
                    + "Sem datas, considera o mês atual")
    public SpendingSummaryResponse spendingSummary(
            @ToolParam(description = "Data inicial AAAA-MM-DD", required = false) String start,
            @ToolParam(description = "Data final AAAA-MM-DD", required = false) String end,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] resumo_de_gastos user={} start={} end={}", userId, start, end);
        return transactionService.summary(userId, parseDate(start), parseDate(end));
    }

    static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("data '%s' inválida, use o formato AAAA-MM-DD".formatted(value));
        }
    }
}
