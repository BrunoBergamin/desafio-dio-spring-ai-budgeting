package dio.budgeting.repository;

import dio.budgeting.entity.SavingsGoal;
import dio.budgeting.entity.User;
import dio.budgeting.support.JpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaTest
class SavingsGoalRepositoryTest {

    @Autowired SavingsGoalRepository repository;
    @Autowired UserRepository userRepository;

    User bruno;
    User outra;

    @BeforeEach
    void setUp() {
        bruno = userRepository.save(new User("Bruno", "bruno@email.com", "hash"));
        outra = userRepository.save(new User("Outra", "outra@email.com", "hash"));
    }

    @Test
    void should_rejectTwoGoalsWithTheSameName_forTheSameUser() {
        repository.saveAndFlush(new SavingsGoal(bruno, "Viagem", new BigDecimal("6000"), null));

        assertThatThrownBy(() -> repository.saveAndFlush(new SavingsGoal(bruno, "Viagem", new BigDecimal("9000"), null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_allowTheSameGoalName_forDifferentPeople() {
        repository.saveAndFlush(new SavingsGoal(bruno, "Viagem", new BigDecimal("6000"), null));
        repository.saveAndFlush(new SavingsGoal(outra, "Viagem", new BigDecimal("3000"), null));

        assertThat(repository.countByUserId(bruno.getId())).isEqualTo(1);
        assertThat(repository.countByUserId(outra.getId())).isEqualTo(1);
    }

    @Test
    void should_findByNameIgnoringCase_andNeverAcrossUsers() {
        repository.saveAndFlush(new SavingsGoal(bruno, "Reserva de emergência", new BigDecimal("10000"), null));

        assertThat(repository.findByUserIdAndNameIgnoreCase(bruno.getId(), "reserva de EMERGÊNCIA")).isPresent();
        assertThat(repository.findByUserIdAndNameIgnoreCase(outra.getId(), "Reserva de emergência")).isEmpty();
    }

    @Test
    void should_listInProgressFirst_thenByDeadline() {
        var concluida = new SavingsGoal(bruno, "Notebook", new BigDecimal("4000"), LocalDate.of(2026, 10, 31));
        concluida.changeSaved(new BigDecimal("4000"));
        repository.save(concluida);
        repository.save(new SavingsGoal(bruno, "Viagem", new BigDecimal("6000"), LocalDate.of(2027, 1, 31)));
        repository.save(new SavingsGoal(bruno, "Reserva", new BigDecimal("10000"), LocalDate.of(2026, 12, 31)));

        assertThat(repository.findAllByUserIdOrderByCompletedAtAscDeadlineAscCreatedAtAsc(bruno.getId()))
                .extracting(SavingsGoal::getName)
                .containsExactly("Reserva", "Viagem", "Notebook");
    }
}
