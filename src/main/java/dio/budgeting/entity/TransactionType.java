package dio.budgeting.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Dinheiro que sai (gasto) ou que entra (receita). Toda transacao e um dos dois. */
@Getter
@RequiredArgsConstructor
public enum TransactionType {
    EXPENSE("Gasto"),
    INCOME("Receita");

    private final String label;
}
