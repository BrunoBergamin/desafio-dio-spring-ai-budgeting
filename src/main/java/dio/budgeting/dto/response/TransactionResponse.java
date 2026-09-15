package dio.budgeting.dto.response;

import dio.budgeting.entity.Category;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(UUID id,
                                  String description,
                                  BigDecimal amount,
                                  Category category,
                                  String categoryLabel,
                                  LocalDate date) {
}
