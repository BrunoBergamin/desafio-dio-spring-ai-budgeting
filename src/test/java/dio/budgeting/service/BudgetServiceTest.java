package dio.budgeting.service;

import dio.budgeting.dto.request.BudgetRequest;
import dio.budgeting.entity.Budget;
import dio.budgeting.entity.BudgetStatus;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.User;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.repository.BudgetRepository;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate SEPTEMBER = LocalDate.of(2026, 9, 1);
    /** 30/09 as 23h em Brasilia: em UTC ja e 1/10. O "mes atual" tem que continuar setembro. */
    private static final Clock END_OF_SEPTEMBER =
            Clock.fixed(Instant.parse("2026-10-01T02:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    @Mock BudgetRepository budgetRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository userRepository;

    BudgetService service;
    User owner = new User("Bruno", "bruno@email.com", "hash");

    @BeforeEach
    void setUp() {
        service = new BudgetService(budgetRepository, transactionRepository, userRepository,
                FACTORY.getValidator(), new BigDecimal("80"), END_OF_SEPTEMBER);
    }

    @Test
    void should_useBrazilMonth_when_monthIsOmittedAndUtcIsAlreadyOnTheNextMonth() {
        when(budgetRepository.findAllByUserIdAndReferenceMonthOrderByCategory(USER_ID, SEPTEMBER)).thenReturn(java.util.List.of());

        assertThat(service.list(USER_ID, null)).isEmpty();

        verify(budgetRepository).findAllByUserIdAndReferenceMonthOrderByCategory(USER_ID, SEPTEMBER);
    }

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void should_rejectBudget_when_limitIsNotPositive() {
        assertThatThrownBy(() -> service.create(USER_ID, new BudgetRequest(Category.GROCERIES, BigDecimal.ZERO, "2026-09")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maior que zero");
        verifyNoInteractions(budgetRepository);
    }

    @Test
    void should_rejectBudget_when_categoryIsAnIncomeOne() {
        assertThatThrownBy(() -> service.create(USER_ID, new BudgetRequest(Category.SALARY, new BigDecimal("5000"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("categoria de receita");
        verifyNoInteractions(budgetRepository);
    }

    @Test
    void should_rejectBudget_when_monthFormatIsInvalid() {
        assertThatThrownBy(() -> service.create(USER_ID, new BudgetRequest(Category.GROCERIES, BigDecimal.TEN, "2026-13")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AAAA-MM");
    }

    @Test
    void should_rejectDuplicate_when_budgetExistsForCategoryAndMonth() {
        when(budgetRepository.existsByUserIdAndCategoryAndReferenceMonth(USER_ID, Category.GROCERIES, SEPTEMBER)).thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, new BudgetRequest(Category.GROCERIES, BigDecimal.TEN, "2026-09")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já existe");
    }

    @Test
    void should_updateLimit_when_upsertFindsExistingBudget() {
        var existing = new Budget(owner, Category.GROCERIES, SEPTEMBER, new BigDecimal("500"));
        when(budgetRepository.findByUserIdAndCategoryAndReferenceMonth(USER_ID, Category.GROCERIES, SEPTEMBER))
                .thenReturn(Optional.of(existing));
        when(budgetRepository.save(existing)).thenReturn(existing);
        when(transactionRepository.sumAmountByCategory(eq(USER_ID), eq(Category.GROCERIES), any(), any())).thenReturn(BigDecimal.ZERO);

        var status = service.upsert(USER_ID, new BudgetRequest(Category.GROCERIES, new BigDecimal("800"), "2026-09"));

        assertThat(status.monthlyLimit()).isEqualByComparingTo("800");
        assertThat(existing.getMonthlyLimit()).isEqualByComparingTo("800");
    }

    @ParameterizedTest
    @CsvSource({"399.99, OK", "400.00, WARNING", "499.99, WARNING", "500.00, EXCEEDED", "650.00, EXCEEDED"})
    void should_computeStatusAtTheBoundaries(String spent, BudgetStatus expected) {
        var budget = new Budget(owner, Category.GROCERIES, SEPTEMBER, new BigDecimal("500.00"));
        when(budgetRepository.findByUserIdAndCategoryAndReferenceMonth(USER_ID, Category.GROCERIES, SEPTEMBER))
                .thenReturn(Optional.of(budget));
        when(transactionRepository.sumAmountByCategory(USER_ID, Category.GROCERIES, SEPTEMBER, LocalDate.of(2026, 9, 30)))
                .thenReturn(new BigDecimal(spent));

        var status = service.checkAfterExpense(USER_ID, Category.GROCERIES, LocalDate.of(2026, 9, 15)).orElseThrow();

        assertThat(status.status()).isEqualTo(expected);
        assertThat(status.spent()).isEqualByComparingTo(spent);
        assertThat(status.remaining()).isEqualByComparingTo(new BigDecimal("500.00").subtract(new BigDecimal(spent)));
        assertThat(status.month()).isEqualTo("2026-09");
    }

    @Test
    void should_returnEmpty_when_thereIsNoBudgetForTheCategory() {
        when(budgetRepository.findByUserIdAndCategoryAndReferenceMonth(USER_ID, Category.AUTO, SEPTEMBER)).thenReturn(Optional.empty());

        assertThat(service.checkAfterExpense(USER_ID, Category.AUTO, LocalDate.of(2026, 9, 3))).isEmpty();
    }

    @Test
    void should_mentionRemainingAmount_when_statusIsWarning() {
        var budget = new Budget(owner, Category.PHARMA, SEPTEMBER, new BigDecimal("200.00"));
        when(budgetRepository.findByUserIdAndCategoryAndReferenceMonth(USER_ID, Category.PHARMA, SEPTEMBER)).thenReturn(Optional.of(budget));
        when(transactionRepository.sumAmountByCategory(any(), any(), any(), any())).thenReturn(new BigDecimal("170.00"));

        var status = service.checkAfterExpense(USER_ID, Category.PHARMA, LocalDate.of(2026, 9, 20)).orElseThrow();

        assertThat(status.usedPercentage()).isEqualByComparingTo("85.0");
        assertThat(status.message()).contains("85.0%").contains("30.00");
    }
}
