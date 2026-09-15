package dio.budgeting.dto.request;

import dio.budgeting.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @Schema(example = "Compras no mercado")
        @NotBlank(message = "a descrição é obrigatória")
        @Size(max = 120, message = "a descrição deve ter no máximo 120 caracteres")
        String description,

        @Schema(example = "80.50")
        @NotNull(message = "o valor é obrigatório")
        @DecimalMin(value = "0.01", message = "o valor deve ser maior que zero")
        @DecimalMax(value = "100000.00", message = "o valor deve ser no máximo 100000.00")
        @Digits(integer = 6, fraction = 2, message = "o valor deve ter no máximo 2 casas decimais")
        BigDecimal amount,

        @Schema(example = "GROCERIES")
        @NotNull(message = "a categoria é obrigatória")
        Category category,

        @Schema(example = "2026-09-15", description = "Opcional. Se omitida, usa a data de hoje.")
        @PastOrPresent(message = "a data não pode estar no futuro")
        LocalDate date
) {
}
