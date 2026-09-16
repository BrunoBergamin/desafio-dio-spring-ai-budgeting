package dio.budgeting.dto.request;

import dio.budgeting.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BudgetRequest(
        @Schema(example = "GROCERIES")
        @NotNull(message = "a categoria é obrigatória")
        Category category,

        @Schema(example = "800.00")
        @NotNull(message = "o limite é obrigatório")
        @DecimalMin(value = "0.01", message = "o limite deve ser maior que zero")
        @DecimalMax(value = "1000000.00", message = "o limite deve ser no máximo 1000000.00")
        @Digits(integer = 7, fraction = 2, message = "o limite deve ter no máximo 2 casas decimais")
        BigDecimal monthlyLimit,

        @Schema(example = "2026-09", description = "Mês no formato AAAA-MM. Opcional: padrão é o mês atual.")
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "o mês deve estar no formato AAAA-MM")
        String month
) {
}
