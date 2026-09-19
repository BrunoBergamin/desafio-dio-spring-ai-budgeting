package dio.budgeting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumo do periodo. {@code total} continua sendo o total de GASTOS (nome mantido para nao quebrar
 * quem ja consome a API); as receitas vem em {@code income} e a sobra em {@code balance}.
 */
public record SpendingSummaryResponse(
        LocalDate start,
        LocalDate end,

        @Schema(description = "Total gasto no período", example = "1250.40")
        BigDecimal total,

        @Schema(description = "Total recebido no período", example = "5200.00")
        BigDecimal income,

        @Schema(description = "Receitas menos gastos; negativo quando gastou mais do que entrou", example = "3949.60")
        BigDecimal balance,

        @Schema(description = "Quantidade de gastos no período")
        long quantity,

        @Schema(description = "Gastos agrupados por categoria, do maior para o menor")
        List<CategorySummaryResponse> categories
) {
}
