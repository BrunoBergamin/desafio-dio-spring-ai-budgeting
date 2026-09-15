package dio.budgeting.repository;

import dio.budgeting.entity.Category;
import dio.budgeting.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByOrderByDateDescCreatedAtDesc();

    List<Transaction> findAllByCategoryOrderByDateDesc(Category category);

    List<Transaction> findAllByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);

    List<Transaction> findAllByCategoryAndDateBetweenOrderByDateDesc(Category category, LocalDate start, LocalDate end);

    List<Transaction> findTop5ByOrderByDateDescCreatedAtDesc();

    @Query("""
            select t.category as category, sum(t.amount) as total, count(t) as quantity
            from Transaction t
            where t.date between :start and :end
            group by t.category
            order by sum(t.amount) desc
            """)
    List<CategoryTotal> sumByCategoryBetween(LocalDate start, LocalDate end);
}
