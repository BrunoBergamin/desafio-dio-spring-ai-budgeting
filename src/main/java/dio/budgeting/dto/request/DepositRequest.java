package dio.budgeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
        @Schema(example = "300.00")
        @NotNull(message = "o valor é obrigatório")
        @DecimalMin(value = "0.01", message = "o valor deve ser maior que zero")
        @Digits(integer = 7, fraction = 2, message = "o valor deve ter no máximo 2 casas decimais")
        BigDecimal amount
) {
}
