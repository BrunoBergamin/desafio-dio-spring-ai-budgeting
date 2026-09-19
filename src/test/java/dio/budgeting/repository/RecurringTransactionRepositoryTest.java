package dio.budgeting.repository;

import dio.budgeting.entity.Category;
import dio.budgeting.entity.RecurringTransaction;
import dio.budgeting.entity.Transaction;
import dio.budgeting.entity.User;
import dio.budgeting.support.JpaTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@JpaTest
class RecurringTransactionRepositoryTest {

    @Autowired RecurringTransactionRepository repository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager entityManager;

    User bruno;
    User outra;

    @BeforeEach
    void setUp() {
        bruno = userRepository.save(new User("Bruno", "bruno@email.com", "hash"));
        outra = userRepository.save(new User("Outra", "outra@email.com", "hash"));
    }

    private RecurringTransaction rule(User user, String description, int day, boolean active) {
        var r = new RecurringTransaction(user, description, new BigDecimal("100.00"), Category.SUBSCRIPTIONS,
                day, LocalDate.of(2026, 9, 1), null);
        r.setActive(active);
        // saveAndFlush: o lançamento que aponta para a regra precisa dela já gravada
        return repository.saveAndFlush(r);
    }

    @Test
    void should_listOnlyTheOwnRules_orderedByDay() {
        rule(bruno, "Netflix", 4, true);
        rule(bruno, "Aluguel", 10, true);
        rule(outra, "Academia da outra", 2, true);

        assertThat(repository.findAllByUserIdOrderByDayOfMonth(bruno.getId()))
                .extracting(RecurringTransaction::getDescription).containsExactly("Netflix", "Aluguel");
        assertThat(repository.countByUserId(outra.getId())).isEqualTo(1);
    }

    @Test
    void should_returnOnlyActiveRules_when_theGeneratorAsksForThem() {
        rule(bruno, "Netflix", 4, true);
        rule(bruno, "Academia pausada", 8, false);
        rule(outra, "Aluguel da outra", 10, true);

        // O gerador roda para todo mundo: as duas ativas aparecem, a pausada nao
        assertThat(repository.findAllByActiveTrue())
                .extracting(RecurringTransaction::getDescription)
                .containsExactlyInAnyOrder("Netflix", "Aluguel da outra");
    }

    @Test
    void should_keepTheTransaction_when_theRuleIsDeleted() {
        var rule = rule(bruno, "Netflix", 4, true);
        var transaction = new Transaction(bruno, "Netflix", new BigDecimal("59.90"),
                Category.SUBSCRIPTIONS, LocalDate.of(2026, 9, 4));
        transaction.setRecurring(rule);
        var transactionId = transactionRepository.saveAndFlush(transaction).getId();

        // Limpa o contexto antes de apagar: na aplicacao o delete roda em outra transacao, sem o
        // lancamento carregado em memoria. Assim quem resolve o vinculo e o ON DELETE SET NULL do banco.
        entityManager.clear();
        repository.deleteById(rule.getId());
        repository.flush();
        entityManager.clear();

        // O historico fica: o lancamento continua la, so perde o vinculo com a regra apagada
        var stillThere = transactionRepository.findByIdAndUserId(transactionId, bruno.getId()).orElseThrow();
        assertThat(stillThere.getDescription()).isEqualTo("Netflix");
        assertThat(stillThere.getRecurring()).isNull();
    }
}
