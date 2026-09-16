package dio.budgeting.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetStatus {
    OK("Dentro do limite"),
    WARNING("Atenção: perto do limite"),
    EXCEEDED("Limite estourado");

    private final String label;
}
