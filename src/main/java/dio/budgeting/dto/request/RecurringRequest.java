package dio.budgeting.dto.request;

import dio.budgeting.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record RecurringRequest(
        @Schema(example = "Aluguel")
        @NotBlank(message = "a descrição é obrigatória")
        @Size(max = 120, message = "a descrição deve ter no máximo 120 caracteres")
        String description,

        @Schema(example = "1500.00")
        @NotNull(message = "o valor é obrigatório")
        @DecimalMin(value = "0.01", message = "o valor deve ser maior que zero")
        @DecimalMax(value = "100000.00", message = "o valor deve ser no máximo 100000.00")
        @Digits(integer = 6, fraction = 2, message = "o valor deve ter no máximo 2 casas decimais")
        BigDecimal amount,

        @Schema(example = "HOUSING", description = "A categoria define se a conta é gasto ou receita.")
        @NotNull(message = "a categoria é obrigatória")
        Category category,

        @Schema(example = "10", description = "Dia do mês. Em mês curto, cai no último dia: 31 vira 28 em fevereiro.")
        @NotNull(message = "o dia do mês é obrigatório")
        @Min(value = 1, message = "o dia deve estar entre 1 e 31")
        @Max(value = 31, message = "o dia deve estar entre 1 e 31")
        Integer dayOfMonth,

        @Schema(example = "2026-09", description = "Primeiro mês. Opcional: padrão é o mês atual.")
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "o mês inicial deve estar no formato AAAA-MM")
        String startMonth,

        @Schema(example = "2027-12", description = "Último mês. Opcional: sem isso, a conta não tem prazo para acabar.")
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "o mês final deve estar no formato AAAA-MM")
        String endMonth,

        @Schema(example = "true", description = "Opcional. Pausa a conta sem apagar o histórico.")
        Boolean active
) {
}
