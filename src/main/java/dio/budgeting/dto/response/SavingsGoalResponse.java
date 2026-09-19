package dio.budgeting.dto.response;

import dio.budgeting.entity.GoalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Progresso de uma meta de economia.
 *
 * @param monthsLeft nulo quando a meta nao tem prazo
 * @param suggestedMonthly quanto guardar por mes para chegar no prazo; nulo sem prazo ou ja concluida
 * @param message frase pronta para a Lumi repetir
 */
public record SavingsGoalResponse(UUID id,
                                  String name,
                                  BigDecimal targetAmount,
                                  BigDecimal savedAmount,
                                  BigDecimal remaining,
                                  @Schema(example = "30.0") BigDecimal percentage,
                                  LocalDate deadline,
                                  Long monthsLeft,
                                  BigDecimal suggestedMonthly,
                                  GoalStatus status,
                                  String statusLabel,
                                  String message) {
}
