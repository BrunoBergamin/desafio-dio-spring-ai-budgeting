package dio.budgeting.repository;

import dio.budgeting.entity.Category;
import dio.budgeting.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Todas as consultas recebem o usuario: nao existe metodo que devolva dados de todo mundo.
 * As listagens sao paginadas: a ordem vem no {@link Pageable}, e nao no nome do metodo.
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    Page<Transaction> findAllByUserId(UUID userId, Pageable pageable);

    Page<Transaction> findAllByUserIdAndCategory(UUID userId, Category category, Pageable pageable);

    Page<Transaction> findAllByUserIdAndDateBetween(UUID userId, LocalDate start, LocalDate end, Pageable pageable);

    Page<Transaction> findAllByUserIdAndCategoryAndDateBetween(
            UUID userId, Category category, LocalDate start, LocalDate end, Pageable pageable);

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
