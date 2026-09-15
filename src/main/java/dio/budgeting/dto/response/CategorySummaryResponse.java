package dio.budgeting.dto.response;

import dio.budgeting.entity.Category;

import java.math.BigDecimal;

public record CategorySummaryResponse(Category category,
                                      String categoryLabel,
                                      BigDecimal total,
                                      long quantity,
                                      BigDecimal percentage) {
}
