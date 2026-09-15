package dio.budgeting.tool;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.dto.response.SpendingSummaryResponse;
import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.Category;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Esta classe é só um adaptador: traduz os parâmetros vindos da IA e delega para o {@link TransactionService},
 * onde ficam as regras de negócio. Assim a IA nunca acessa o repositório diretamente.
 * Se o service lançar exceção, o Spring AI devolve a mensagem de erro para o modelo, que explica o problema ao usuário.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionTools {

    private final TransactionService transactionService;

    @Tool(name = "registrar_transacao", description = "Registra um novo gasto financeiro")
    public TransactionResponse registerTransaction(
            @ToolParam(description = "Descrição curta do gasto, ex.: 'Compras no mercado'") String description,
            @ToolParam(description = "Valor do gasto em reais, ex.: 80.50") BigDecimal amount,
            @ToolParam(description = "Categoria do gasto") Category category,
            @ToolParam(description = "Data do gasto no formato AAAA-MM-DD. Omita se for hoje", required = false) String date) {
        log.info("[tool] registrar_transacao description='{}' amount={} category={} date={}", description, amount, category, date);
        return transactionService.create(new TransactionRequest(description, amount, category, parseDate(date)));
    }

    @Tool(name = "listar_transacoes", description = "Lista gastos, com filtros opcionais de categoria e período")
    public List<TransactionResponse> listTransactions(
            @ToolParam(description = "Categoria para filtrar", required = false) Category category,
            @ToolParam(description = "Data inicial AAAA-MM-DD", required = false) String start,
            @ToolParam(description = "Data final AAAA-MM-DD", required = false) String end) {
        log.info("[tool] listar_transacoes category={} start={} end={}", category, start, end);
        return transactionService.list(category, parseDate(start), parseDate(end));
    }

    @Tool(name = "ultimas_transacoes", description = "Retorna as 5 transações mais recentes")
    public List<TransactionResponse> recentTransactions() {
        log.info("[tool] ultimas_transacoes");
        return transactionService.recent();
    }

    @Tool(name = "resumo_de_gastos",
            description = "Calcula o total gasto em um período, agrupado por categoria, com percentual de cada uma. "
                    + "Sem datas, considera o mês atual")
    public SpendingSummaryResponse spendingSummary(
            @ToolParam(description = "Data inicial AAAA-MM-DD", required = false) String start,
            @ToolParam(description = "Data final AAAA-MM-DD", required = false) String end) {
        log.info("[tool] resumo_de_gastos start={} end={}", start, end);
        return transactionService.summary(parseDate(start), parseDate(end));
    }

    private LocalDate parseDate(String value) {
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
