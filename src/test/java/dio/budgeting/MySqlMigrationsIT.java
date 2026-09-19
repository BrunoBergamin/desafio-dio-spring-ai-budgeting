package dio.budgeting;

import dio.budgeting.entity.Budget;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.Transaction;
import dio.budgeting.entity.User;
import dio.budgeting.repository.BudgetRepository;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sobe um MySQL de verdade (a mesma imagem do compose.yml) e roda as migrations nele.
 * Os testes de repositorio usam H2, que aceita SQL que o MySQL rejeita e vice-versa; aqui a
 * diferenca aparece no build, nao no docker compose up. Sem Docker na maquina o teste e pulado.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles({"groq", "mysql"})
class MySqlMigrationsIT {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6");

    @Autowired Flyway flyway;
    @Autowired UserRepository userRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired BudgetRepository budgetRepository;

    @BeforeEach
    void cleanUp() {
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void should_applyEveryMigration_when_databaseIsMySql() {
        var info = flyway.info();

        assertThat(info.pending()).isEmpty();
        assertThat(info.applied()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(info.current().getVersion().getVersion()).isEqualTo("4");
    }

    @Test
    void should_aggregateByCategory_when_runningTheJpqlOnMySql() {
        var user = userRepository.save(new User("Bruno", "bruno@email.com", "hash"));
        transactionRepository.save(new Transaction(user, "Mercado", new BigDecimal("80.50"), Category.GROCERIES, LocalDate.of(2026, 9, 10)));
        transactionRepository.save(new Transaction(user, "Padaria", new BigDecimal("19.50"), Category.GROCERIES, LocalDate.of(2026, 9, 12)));
        transactionRepository.save(new Transaction(user, "Remedio", new BigDecimal("42.90"), Category.PHARMA, LocalDate.of(2026, 9, 12)));

        var totals = transactionRepository.sumByCategoryBetween(user.getId(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        var groceries = transactionRepository.sumAmountByCategory(user.getId(), Category.GROCERIES, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(totals).hasSize(2);
        assertThat(totals.getFirst().getCategory()).isEqualTo(Category.GROCERIES);
        assertThat(totals.getFirst().getTotal()).isEqualByComparingTo("100.00");
        assertThat(totals.getFirst().getQuantity()).isEqualTo(2);
        assertThat(groceries).isEqualByComparingTo("100.00");
    }

    @Test
    void should_enforceUniqueConstraints_when_databaseIsMySql() {
        var user = userRepository.saveAndFlush(new User("Bruno", "bruno@email.com", "hash"));
        budgetRepository.saveAndFlush(new Budget(user, Category.GROCERIES, LocalDate.of(2026, 9, 1), new BigDecimal("500")));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("Clone", "bruno@email.com", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> budgetRepository.saveAndFlush(
                new Budget(user, Category.GROCERIES, LocalDate.of(2026, 9, 1), new BigDecimal("600"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
