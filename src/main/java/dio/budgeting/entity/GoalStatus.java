package dio.budgeting.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Situacao de uma meta de economia. */
@Getter
@RequiredArgsConstructor
public enum GoalStatus {
    IN_PROGRESS("Em andamento"),
    COMPLETED("Concluída"),
    OVERDUE("Prazo vencido");

    private final String label;
}
