package dio.budgeting.repository;

import dio.budgeting.entity.Budget;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.User;
import dio.budgeting.support.JpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaTest
class UserAndBudgetRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired BudgetRepository budgetRepository;

    @Test
    void should_findByEmail_when_userExists() {
        userRepository.save(new User("Bruno", "bruno@email.com", "hash"));

        assertThat(userRepository.findByEmail("bruno@email.com")).isPresent();
        assertThat(userRepository.existsByEmail("ninguem@email.com")).isFalse();
    }

    @Test
    void should_rejectDuplicatedEmail_when_savingTwice() {
        userRepository.saveAndFlush(new User("Bruno", "bruno@email.com", "hash"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("Clone", "bruno@email.com", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_rejectDuplicatedBudget_when_sameUserCategoryAndMonth() {
        var user = userRepository.save(new User("Bruno", "bruno@email.com", "hash"));
        budgetRepository.saveAndFlush(new Budget(user, Category.GROCERIES, LocalDate.of(2026, 9, 15), new BigDecimal("500")));

        assertThatThrownBy(() -> budgetRepository.saveAndFlush(
                new Budget(user, Category.GROCERIES, LocalDate.of(2026, 9, 1), new BigDecimal("600"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_notReturnBudgetOfAnotherUser() {
        var bruno = userRepository.save(new User("Bruno", "bruno@email.com", "hash"));
        var outra = userRepository.save(new User("Outra", "outra@email.com", "hash"));
        var budget = budgetRepository.save(new Budget(bruno, Category.PHARMA, LocalDate.of(2026, 9, 1), new BigDecimal("200")));

        assertThat(budgetRepository.findByIdAndUserId(budget.getId(), outra.getId())).isEmpty();
        assertThat(budgetRepository.findAllByUserIdAndReferenceMonthOrderByCategory(bruno.getId(), LocalDate.of(2026, 9, 1))).hasSize(1);
        assertThat(budget.getReferenceMonth()).isEqualTo(LocalDate.of(2026, 9, 1));
    }
}
