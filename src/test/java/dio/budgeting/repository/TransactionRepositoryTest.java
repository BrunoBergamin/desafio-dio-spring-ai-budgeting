package dio.budgeting.repository;

import dio.budgeting.entity.Category;
import dio.budgeting.entity.Transaction;
import dio.budgeting.entity.User;
import dio.budgeting.support.JpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@JpaTest
class TransactionRepositoryTest {

    @Autowired
    TransactionRepository repository;

    @Autowired
    UserRepository userRepository;

    User bruno;
    User outra;

    @BeforeEach
    void setUp() {
        bruno = userRepository.save(new User("Bruno", "bruno@email.com", "hash"));
        outra = userRepository.save(new User("Outra", "outra@email.com", "hash"));

        repository.save(new Transaction(bruno, "Mercado", new BigDecimal("100.00"), Category.GROCERIES, LocalDate.of(2026, 9, 2)));
        repository.save(new Transaction(bruno, "Padaria", new BigDecimal("20.00"), Category.GROCERIES, LocalDate.of(2026, 9, 5)));
        repository.save(new Transaction(bruno, "Remédio", new BigDecimal("30.00"), Category.PHARMA, LocalDate.of(2026, 9, 6)));
        repository.save(new Transaction(bruno, "Gasolina", new BigDecimal("200.00"), Category.AUTO, LocalDate.of(2026, 8, 20)));
        repository.save(new Transaction(outra, "Mercado da outra", new BigDecimal("999.00"), Category.GROCERIES, LocalDate.of(2026, 9, 3)));
    }

    @Test
    void should_sumByCategory_onlyForTheUser_when_periodIsGiven() {
        var totals = repository.sumByCategoryBetween(bruno.getId(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(totals).hasSize(2);
        assertThat(totals.getFirst().getCategory()).isEqualTo(Category.GROCERIES);
        assertThat(totals.getFirst().getTotal()).isEqualByComparingTo("120.00");
        assertThat(totals.getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    void should_filterByCategoryAndPeriod() {
        var result = repository.findAllByUserIdAndCategoryAndDateBetweenOrderByDateDesc(
                bruno.getId(), Category.GROCERIES, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 30));

        assertThat(result).extracting(Transaction::getDescription).containsExactly("Padaria");
    }

    @Test
    void should_returnMostRecentFirst() {
        var result = repository.findTop5ByUserIdOrderByDateDescCreatedAtDesc(bruno.getId());

        assertThat(result).extracting(Transaction::getDescription)
                .containsExactly("Remédio", "Padaria", "Mercado", "Gasolina");
    }

    @Test
    void should_notReturnTransactionsOfAnotherUser() {
        var all = repository.findAllByUserIdOrderByDateDescCreatedAtDesc(outra.getId());
        var brunoTransaction = repository.findAllByUserIdOrderByDateDescCreatedAtDesc(bruno.getId()).getFirst();

        assertThat(all).extracting(Transaction::getDescription).containsExactly("Mercado da outra");
        assertThat(repository.findByIdAndUserId(brunoTransaction.getId(), outra.getId())).isEmpty();
    }

    @Test
    void should_sumAmountByCategory_ignoringOtherUsersMonthsAndCategories() {
        var total = repository.sumAmountByCategory(bruno.getId(), Category.GROCERIES, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        var nothing = repository.sumAmountByCategory(bruno.getId(), Category.LEISURE, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(total).isEqualByComparingTo("120.00");
        assertThat(nothing).isEqualByComparingTo("0");
    }
}
