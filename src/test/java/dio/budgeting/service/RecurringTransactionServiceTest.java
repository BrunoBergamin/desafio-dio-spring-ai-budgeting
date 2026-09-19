package dio.budgeting.service;

import dio.budgeting.dto.request.RecurringRequest;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.RecurringTransaction;
import dio.budgeting.entity.Transaction;
import dio.budgeting.entity.TransactionType;
import dio.budgeting.entity.User;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.repository.RecurringTransactionRepository;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Mock RecurringTransactionRepository recurringRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository userRepository;

    User owner = new User("Bruno", "bruno@email.com", "hash");

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    /** Relogio fixo no dia informado, meio-dia em Brasilia. */
    private RecurringTransactionService serviceAt(LocalDate today) {
        var clock = Clock.fixed(today.atTime(12, 0).atZone(SP).toInstant(), SP);
        return new RecurringTransactionService(recurringRepository, transactionRepository, userRepository,
                FACTORY.getValidator(), clock);
    }

    private RecurringTransaction rule(int dayOfMonth, LocalDate startMonth) {
        var r = new RecurringTransaction(owner, "Aluguel", new BigDecimal("1500.00"), Category.HOUSING,
                dayOfMonth, startMonth, null);
        r.setId(UUID.randomUUID());
        return r;
    }

    private List<Transaction> savedTransactions() {
        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    @BeforeEach
    void ignoreSaves() {
        lenient().when(recurringRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void should_useTheLastDay_when_theChosenDayDoesNotExistInTheMonth() {
        // Regra no dia 31, gerando fevereiro de 2027 (28 dias)
        var rule = rule(31, LocalDate.of(2027, 2, 1));
        var service = serviceAt(LocalDate.of(2027, 2, 28));
        when(recurringRepository.findAllByActiveTrue()).thenReturn(List.of(rule));

        service.generateDue();

        assertThat(savedTransactions()).singleElement()
                .extracting(Transaction::getDate).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    void should_useTheTwentyNinth_when_februaryIsInALeapYear() {
        var rule = rule(31, LocalDate.of(2028, 2, 1));
        var service = serviceAt(LocalDate.of(2028, 2, 29));
        when(recurringRepository.findAllByActiveTrue()).thenReturn(List.of(rule));

        service.generateDue();

        assertThat(savedTransactions()).singleElement()
                .extracting(Transaction::getDate).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void should_createOneForEachMissedMonth_when_theAppWasDownForMonths() {
        var rule = rule(10, LocalDate.of(2026, 7, 1));
        var service = serviceAt(LocalDate.of(2026, 9, 19));
        when(recurringRepository.findAllByActiveTrue()).thenReturn(List.of(rule));

        var created = service.generateDue();

        assertThat(created).isEqualTo(3);
        assertThat(savedTransactions()).extracting(Transaction::getDate).containsExactly(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 9, 10));
        assertThat(rule.getLastGeneratedMonth()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void should_createNothing_when_runningTwiceOnTheSameDay() {
        var rule = rule(10, LocalDate.of(2026, 9, 1));
        var service = serviceAt(LocalDate.of(2026, 9, 19));
        when(recurringRepository.findAllByActiveTrue()).thenReturn(List.of(rule));

        assertThat(service.generateDue()).isEqualTo(1);
        assertThat(service.generateDue()).isZero();
        assertThat(savedTransactions()).hasSize(1);
    }

    @Test
    void should_waitForTheDay_when_itHasNotArrivedYetThisMonth() {
        var rule = rule(25, LocalDate.of(2026, 9, 1));
        var service = serviceAt(LocalDate.of(2026, 9, 19));
        when(recurringRepository.findAllByActiveTrue()).thenReturn(List.of(rule));

        assertThat(service.generateDue()).isZero();
        assertThat(rule.getLastGeneratedMonth()).isNull();
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void should_stopGenerating_when_thereIsAnEndMonth() {
        var rule = new RecurringTransaction(owner, "Parcelas do curso", new BigDecimal("200.00"),
                Category.EDUCATION, 5, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1));
        rule.setId(UUID.randomUUID());
        var service = serviceAt(LocalDate.of(2026, 9, 19));
        when(recurringRepository.findAllByActiveTrue()).thenReturn(List.of(rule));

        assertThat(service.generateDue()).isEqualTo(2);
        assertThat(savedTransactions()).extracting(Transaction::getDate).containsExactly(
                LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 5));
    }

    @Test
    void should_keepGoing_when_oneRuleFails() {
        var quebrada = rule(10, LocalDate.of(2026, 9, 1));
        var boa = rule(10, LocalDate.of(2026, 9, 1));
        var service = serviceAt(LocalDate.of(2026, 9, 19));
        when(recurringRepository.findAllByActiveTrue()).thenReturn(List.of(quebrada, boa));
        when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new IllegalStateException("banco fora do ar"))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.generateDue()).isEqualTo(1);
        assertThat(boa.getLastGeneratedMonth()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void should_linkTheTransactionToTheRule_when_generating() {
        var rule = rule(10, LocalDate.of(2026, 9, 1));
        var service = serviceAt(LocalDate.of(2026, 9, 19));
        when(recurringRepository.findAllByActiveTrue()).thenReturn(List.of(rule));

        service.generateDue();

        assertThat(savedTransactions()).singleElement().satisfies(t -> {
            assertThat(t.getRecurring()).isSameAs(rule);
            assertThat(t.getUser()).isSameAs(owner);
            assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(t.getAmount()).isEqualByComparingTo("1500.00");
        });
    }

    @Test
    void should_createTheCurrentMonthRightAway_when_theDayHasAlreadyPassed() {
        var service = serviceAt(LocalDate.of(2026, 9, 19));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);

        var response = service.create(USER_ID,
                new RecurringRequest("Netflix", new BigDecimal("59.90"), Category.SUBSCRIPTIONS, 4, null, null, null));

        assertThat(savedTransactions()).singleElement()
                .extracting(Transaction::getDate).isEqualTo(LocalDate.of(2026, 9, 4));
        // A proxima ja aponta para outubro
        assertThat(response.nextOccurrence()).isEqualTo(LocalDate.of(2026, 10, 4));
        assertThat(response.message()).contains("Netflix").contains("todo dia 4");
    }

    @Test
    void should_ignorePausedRules_when_generating() {
        var rule = rule(10, LocalDate.of(2026, 9, 1));
        rule.setActive(false);
        var service = serviceAt(LocalDate.of(2026, 9, 19));
        // O repositorio so devolve as ativas, entao a pausada nem aparece
        when(recurringRepository.findAllByActiveTrue()).thenReturn(List.of());

        assertThat(service.generateDue()).isZero();
        assertThat(service.toResponse(rule).nextOccurrence()).isNull();
        assertThat(service.toResponse(rule).message()).contains("pausada");
    }

    @Test
    void should_notBackfillPausedMonths_when_theRuleIsResumed() {
        var rule = rule(10, LocalDate.of(2026, 1, 1));
        rule.setActive(false);
        rule.setLastGeneratedMonth(LocalDate.of(2026, 3, 1)); // parou em marco
        var service = serviceAt(LocalDate.of(2026, 9, 19));
        when(recurringRepository.findByIdAndUserId(rule.getId(), USER_ID)).thenReturn(java.util.Optional.of(rule));

        service.update(USER_ID, rule.getId(),
                new RecurringRequest("Aluguel", new BigDecimal("1500.00"), Category.HOUSING, 10, null, null, true));

        // Retomou em setembro: os meses de abril a agosto nao voltam
        assertThat(rule.getLastGeneratedMonth()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(rule.isActive()).isTrue();
    }

    @Test
    void should_rejectTheRule_when_endMonthIsBeforeStartMonth() {
        var service = serviceAt(LocalDate.of(2026, 9, 19));

        assertThatThrownBy(() -> service.create(USER_ID, new RecurringRequest(
                "Curso", new BigDecimal("100"), Category.EDUCATION, 5, "2026-09", "2026-07", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("igual ou posterior");
    }

    @Test
    void should_rejectTheRule_when_dayIsOutOfRange() {
        var service = serviceAt(LocalDate.of(2026, 9, 19));

        assertThatThrownBy(() -> service.create(USER_ID, new RecurringRequest(
                "Aluguel", new BigDecimal("1500"), Category.HOUSING, 32, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entre 1 e 31");
    }

    @Test
    void should_acceptIncome_when_theCategoryIsAnEarningOne() {
        var service = serviceAt(LocalDate.of(2026, 9, 19));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);

        var response = service.create(USER_ID,
                new RecurringRequest("Salário", new BigDecimal("5200"), Category.SALARY, 5, null, null, null));

        assertThat(response.type()).isEqualTo(TransactionType.INCOME);
        assertThat(savedTransactions()).singleElement()
                .extracting(Transaction::getType).isEqualTo(TransactionType.INCOME);
    }
}
