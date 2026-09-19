package dio.budgeting.service;

import dio.budgeting.dto.request.GoalRequest;
import dio.budgeting.entity.GoalStatus;
import dio.budgeting.entity.SavingsGoal;
import dio.budgeting.entity.User;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.repository.SavingsGoalRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    /** 19 de setembro de 2026, meio-dia em Brasilia. */
    private static final Clock TODAY = Clock.fixed(Instant.parse("2026-09-19T15:00:00Z"), SP);

    @Mock SavingsGoalRepository goalRepository;
    @Mock UserRepository userRepository;

    SavingsGoalService service;
    User owner = new User("Bruno", "bruno@email.com", "hash");

    @BeforeEach
    void setUp() {
        service = new SavingsGoalService(goalRepository, userRepository, FACTORY.getValidator(), TODAY);
        lenient().when(goalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    private SavingsGoal goal(String name, String target, String saved, LocalDate deadline) {
        var g = new SavingsGoal(owner, name, new BigDecimal(target), deadline);
        g.changeSaved(new BigDecimal(saved));
        g.setId(UUID.randomUUID());
        return g;
    }

    @ParameterizedTest
    @CsvSource({"0, 0.0", "1500, 25.0", "5999.99, 100.0", "6000, 100.0", "7000, 116.7"})
    void should_computePercentage_atTheBoundaries(String saved, String expected) {
        var progress = service.toProgress(goal("Viagem", "6000.00", saved, null));

        assertThat(progress.percentage()).isEqualByComparingTo(expected);
    }

    @Test
    void should_markAsCompleted_when_theDepositReachesTheTarget() {
        var g = goal("Viagem", "6000.00", "5800.00", null);
        when(goalRepository.findByIdAndUserId(g.getId(), USER_ID)).thenReturn(Optional.of(g));

        var progress = service.deposit(USER_ID, g.getId(), new BigDecimal("200.00"));

        assertThat(progress.status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(progress.remaining()).isEqualByComparingTo("0");
        assertThat(g.getCompletedAt()).isNotNull();
        assertThat(progress.message()).contains("concluída");
    }

    @Test
    void should_stayCompletedWithoutNegativeRemaining_when_savingMoreThanTheTarget() {
        var g = goal("Viagem", "6000.00", "6500.00", null);

        var progress = service.toProgress(g);

        assertThat(progress.status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(progress.remaining()).isEqualByComparingTo("0");
        assertThat(progress.percentage()).isEqualByComparingTo("108.3");
    }

    @Test
    void should_undoCompletion_when_theSavedAmountGoesBackDown() {
        var g = goal("Viagem", "6000.00", "6000.00", null);
        assertThat(g.isCompleted()).isTrue();

        g.changeSaved(new BigDecimal("100.00"));

        assertThat(g.isCompleted()).isFalse();
        assertThat(service.toProgress(g).status()).isEqualTo(GoalStatus.IN_PROGRESS);
    }

    @Test
    void should_beOverdue_when_theDeadlineHasPassedWithoutReachingTheTarget() {
        var progress = service.toProgress(goal("Viagem", "6000.00", "1800.00", LocalDate.of(2026, 8, 31)));

        assertThat(progress.status()).isEqualTo(GoalStatus.OVERDUE);
        assertThat(progress.monthsLeft()).isNull();
        assertThat(progress.message()).contains("venceu");
    }

    @Test
    void should_suggestHowMuchPerMonth_when_thereIsADeadline() {
        // Faltam 4200 e restam 4 meses (setembro, outubro, novembro e dezembro)
        var progress = service.toProgress(goal("Viagem", "6000.00", "1800.00", LocalDate.of(2026, 12, 31)));

        assertThat(progress.monthsLeft()).isEqualTo(4);
        assertThat(progress.suggestedMonthly()).isEqualByComparingTo("1050.00");
        assertThat(progress.message()).contains("1050.00 reais por mês");
    }

    @Test
    void should_countTheCurrentMonth_when_theDeadlineIsThisMonth() {
        var progress = service.toProgress(goal("Viagem", "1000.00", "0", LocalDate.of(2026, 9, 30)));

        assertThat(progress.monthsLeft()).isEqualTo(1);
        assertThat(progress.suggestedMonthly()).isEqualByComparingTo("1000.00");
    }

    @Test
    void should_rejectTheGoal_when_theNameIsAlreadyUsed() {
        when(goalRepository.existsByUserIdAndNameIgnoreCase(USER_ID, "Viagem")).thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, new GoalRequest("Viagem", new BigDecimal("6000"), null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já tem uma meta chamada");
        verify(goalRepository, never()).save(any());
    }

    @Test
    void should_rejectTheGoal_when_theDeadlineIsInThePast() {
        assertThatThrownBy(() -> service.create(USER_ID,
                new GoalRequest("Viagem", new BigDecimal("6000"), LocalDate.of(2026, 1, 1), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passado");
    }

    @Test
    void should_findTheGoalByName_when_theLumiRegistersADeposit() {
        var g = goal("Reserva de emergência", "10000.00", "4200.00", null);
        when(goalRepository.findByUserIdAndNameIgnoreCase(USER_ID, "reserva de emergência")).thenReturn(Optional.of(g));

        var progress = service.depositByName(USER_ID, "  reserva de emergência  ", new BigDecimal("300.00"));

        assertThat(progress.savedAmount()).isEqualByComparingTo("4500.00");
    }

    @Test
    void should_explainWhatToDo_when_theGoalNameDoesNotExist() {
        when(goalRepository.findByUserIdAndNameIgnoreCase(USER_ID, "Carro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.depositByName(USER_ID, "Carro", new BigDecimal("100")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("não encontrei uma meta chamada Carro");
    }

    @Test
    void should_rejectTheDeposit_when_theAmountIsNotPositive() {
        var g = goal("Viagem", "6000.00", "0", null);
        when(goalRepository.findByIdAndUserId(g.getId(), USER_ID)).thenReturn(Optional.of(g));

        assertThatThrownBy(() -> service.deposit(USER_ID, g.getId(), BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void should_throwNotFound_when_theGoalBelongsToSomeoneElse() {
        var id = UUID.randomUUID();
        when(goalRepository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, id)).isInstanceOf(ResourceNotFoundException.class);
    }
}
