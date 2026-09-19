package dio.budgeting.repository;

import dio.budgeting.entity.Category;
import dio.budgeting.entity.Transaction;
import dio.budgeting.entity.TransactionType;
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
 * A listagem e paginada e a ordem vem no {@link Pageable}.
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Busca com filtros opcionais: nulo em tipo, categoria ou data significa "nao filtra por isso".
     * Um metodo so, em vez de uma combinacao de {@code findAllBy...} para cada mistura de filtros.
     */
    @Query("""
            select t from Transaction t
            where t.user.id = :userId
              and (:type is null or t.type = :type)
              and (:category is null or t.category = :category)
              and (:start is null or t.date >= :start)
              and (:end is null or t.date <= :end)
            """)
    Page<Transaction> search(UUID userId, TransactionType type, Category category,
                             LocalDate start, LocalDate end, Pageable pageable);

    List<Transaction> findTop5ByUserIdOrderByDateDescCreatedAtDesc(UUID userId);

    /** Gastos agrupados por categoria. Recebe o tipo para a pizza do painel nao misturar salario com mercado. */
    @Query("""
            select t.category as category, sum(t.amount) as total, count(t) as quantity
            from Transaction t
            where t.user.id = :userId and t.type = :type and t.date between :start and :end
            group by t.category
            order by sum(t.amount) desc
            """)
    List<CategoryTotal> sumByCategoryBetween(UUID userId, TransactionType type, LocalDate start, LocalDate end);

    /** Total de gastos ou de receitas no periodo, para o saldo. */
    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.user.id = :userId and t.type = :type and t.date between :start and :end
            """)
    BigDecimal sumAmountByType(UUID userId, TransactionType type, LocalDate start, LocalDate end);

    /**
     * Total de uma categoria no periodo (usado pelo orcamento). Nao precisa filtrar por tipo:
     * so existe orcamento de categoria de gasto, e a categoria ja define o tipo.
     */
    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.user.id = :userId and t.category = :category and t.date between :start and :end
            """)
    BigDecimal sumAmountByCategory(UUID userId, Category category, LocalDate start, LocalDate end);
}
