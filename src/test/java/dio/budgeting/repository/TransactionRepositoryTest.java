package dio.budgeting.repository;

import dio.budgeting.entity.Category;
import dio.budgeting.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    TransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository.save(new Transaction("Mercado", new BigDecimal("100.00"), Category.GROCERIES, LocalDate.of(2026, 9, 2)));
        repository.save(new Transaction("Padaria", new BigDecimal("20.00"), Category.GROCERIES, LocalDate.of(2026, 9, 5)));
        repository.save(new Transaction("Remédio", new BigDecimal("30.00"), Category.PHARMA, LocalDate.of(2026, 9, 6)));
        repository.save(new Transaction("Gasolina", new BigDecimal("200.00"), Category.AUTO, LocalDate.of(2026, 8, 20)));
    }

    @Test
    void should_sumByCategory_when_periodIsGiven() {
        var totals = repository.sumByCategoryBetween(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(totals).hasSize(2);
        assertThat(totals.getFirst().getCategory()).isEqualTo(Category.GROCERIES);
        assertThat(totals.getFirst().getTotal()).isEqualByComparingTo("120.00");
        assertThat(totals.getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    void should_filterByCategoryAndPeriod() {
        var result = repository.findAllByCategoryAndDateBetweenOrderByDateDesc(
                Category.GROCERIES, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 30));

        assertThat(result).extracting(Transaction::getDescription).containsExactly("Padaria");
    }

    @Test
    void should_returnMostRecentFirst() {
        var result = repository.findTop5ByOrderByDateDescCreatedAtDesc();

        assertThat(result).extracting(Transaction::getDescription)
                .containsExactly("Remédio", "Padaria", "Mercado", "Gasolina");
    }
}
