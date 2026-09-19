package dio.budgeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        @Schema(example = "Viagem")
        @NotBlank(message = "o nome da meta é obrigatório")
        @Size(max = 80, message = "o nome deve ter no máximo 80 caracteres")
        String name,

        @Schema(example = "6000.00")
        @NotNull(message = "o valor da meta é obrigatório")
        @DecimalMin(value = "0.01", message = "o valor da meta deve ser maior que zero")
        @DecimalMax(value = "1000000.00", message = "o valor da meta deve ser no máximo 1000000.00")
        @Digits(integer = 7, fraction = 2, message = "o valor deve ter no máximo 2 casas decimais")
        BigDecimal targetAmount,

        @Schema(example = "2027-01-31", description = "Prazo opcional. Sem ele a meta só acumula, não vence.")
        LocalDate deadline,

        @Schema(example = "1800.00", description = "Opcional: corrige quanto já está guardado.")
        @DecimalMin(value = "0.00", message = "o valor guardado não pode ser negativo")
        @Digits(integer = 7, fraction = 2, message = "o valor guardado deve ter no máximo 2 casas decimais")
        BigDecimal savedAmount
) {
}
