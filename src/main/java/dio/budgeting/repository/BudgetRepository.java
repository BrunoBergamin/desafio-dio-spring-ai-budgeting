package dio.budgeting.repository;

import dio.budgeting.entity.Budget;
import dio.budgeting.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    Optional<Budget> findByUserIdAndCategoryAndReferenceMonth(UUID userId, Category category, LocalDate month);

    List<Budget> findAllByUserIdAndReferenceMonthOrderByCategory(UUID userId, LocalDate month);

    boolean existsByUserIdAndCategoryAndReferenceMonth(UUID userId, Category category, LocalDate month);
}
