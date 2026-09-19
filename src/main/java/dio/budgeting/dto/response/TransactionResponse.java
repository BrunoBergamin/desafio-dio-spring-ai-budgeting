package dio.budgeting.dto.response;

import dio.budgeting.entity.Category;
import dio.budgeting.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record TransactionResponse(
        UUID id,
        String description,
        BigDecimal amount,
        Category category,
        String categoryLabel,
        TransactionType type,

        @Schema(description = "Id da conta recorrente que gerou este lançamento; nulo quando foi lançado à mão")
        UUID recurringId,

        LocalDate date
) {
}
