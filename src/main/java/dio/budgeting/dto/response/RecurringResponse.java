package dio.budgeting.dto.response;

import dio.budgeting.entity.Category;
import dio.budgeting.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Conta recorrente, com a proxima data em que ela vai virar lancamento.
 *
 * @param nextOccurrence nulo quando a conta esta pausada ou ja passou do mes final
 * @param message frase pronta para a Lumi repetir
 */
public record RecurringResponse(UUID id,
                                String description,
                                BigDecimal amount,
                                Category category,
                                String categoryLabel,
                                TransactionType type,
                                int dayOfMonth,
                                boolean active,
                                String startMonth,
                                String endMonth,
                                @Schema(example = "2026-10-10") LocalDate nextOccurrence,
                                String message) {
}
