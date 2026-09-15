package dio.budgeting.repository;

import dio.budgeting.entity.Category;

import java.math.BigDecimal;

/** Projeção usada na consulta agregada de gastos por categoria. */
public interface CategoryTotal {
    Category getCategory();

    BigDecimal getTotal();

    Long getQuantity();
}
