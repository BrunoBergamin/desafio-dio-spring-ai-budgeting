package dio.budgeting.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SpendingSummaryResponse(LocalDate start,
                                      LocalDate end,
                                      BigDecimal total,
                                      long quantity,
                                      List<CategorySummaryResponse> categories) {
}
