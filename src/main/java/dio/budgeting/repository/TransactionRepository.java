package dio.budgeting.repository;

import dio.budgeting.entity.Category;
import dio.budgeting.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Todas as consultas recebem o usuario: nao existe metodo que devolva dados de todo mundo. */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    List<Transaction> findAllByUserIdOrderByDateDescCreatedAtDesc(UUID userId);

    List<Transaction> findAllByUserIdAndCategoryOrderByDateDesc(UUID userId, Category category);

    List<Transaction> findAllByUserIdAndDateBetweenOrderByDateDesc(UUID userId, LocalDate start, LocalDate end);

    List<Transaction> findAllByUserIdAndCategoryAndDateBetweenOrderByDateDesc(
            UUID userId, Category category, LocalDate start, LocalDate end);

    List<Transaction> findTop5ByUserIdOrderByDateDescCreatedAtDesc(UUID userId);

    @Query("""
            select t.category as category, sum(t.amount) as total, count(t) as quantity
            from Transaction t
            where t.user.id = :userId and t.date between :start and :end
            group by t.category
            order by sum(t.amount) desc
            """)
    List<CategoryTotal> sumByCategoryBetween(UUID userId, LocalDate start, LocalDate end);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.user.id = :userId and t.category = :category and t.date between :start and :end
            """)
    BigDecimal sumAmountByCategory(UUID userId, Category category, LocalDate start, LocalDate end);
}
