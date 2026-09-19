package dio.budgeting.service;

import dio.budgeting.dto.request.GoalRequest;
import dio.budgeting.dto.response.SavingsGoalResponse;
import dio.budgeting.entity.GoalStatus;
import dio.budgeting.entity.SavingsGoal;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.repository.SavingsGoalRepository;
import dio.budgeting.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Metas de economia. Guardar dinheiro numa meta nao cria lancamento: separar nao e gastar, entao o
 * resumo, o orcamento e o saldo do mes continuam falando so de dinheiro que entrou e saiu de verdade.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final Validator validator;
    private final Clock clock;

    @Transactional
    public SavingsGoalResponse create(UUID userId, GoalRequest request) {
        validate(request);
        var name = request.name().trim();
        // Checa aqui para devolver 422 com mensagem clara, em vez do 409 generico da constraint
        if (goalRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new BusinessException("você já tem uma meta chamada %s".formatted(name));
        }
        if (request.deadline() != null && request.deadline().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("o prazo da meta não pode estar no passado");
        }
        var goal = new SavingsGoal(userRepository.getReferenceById(userId), name,
                request.targetAmount(), request.deadline());
        if (request.savedAmount() != null) {
            goal.changeSaved(request.savedAmount());
        }
        var saved = goalRepository.save(goal);
        log.info("Meta criada: id={} user={} alvo={} prazo={}", saved.getId(), userId,
                saved.getTargetAmount(), saved.getDeadline());
        return toProgress(saved);
    }

    /** Em andamento primeiro, depois por prazo mais perto; concluidas no fim. */
    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> list(UUID userId) {
        return goalRepository.findAllByUserIdOrderByCompletedAtAscDeadlineAscCreatedAtAsc(userId)
                .stream().map(this::toProgress).toList();
    }

    @Transactional
    public SavingsGoalResponse update(UUID userId, UUID id, GoalRequest request) {
        validate(request);
        var goal = getOrThrow(userId, id);
        var name = request.name().trim();
        goalRepository.findByUserIdAndNameIgnoreCase(userId, name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessException("você já tem uma meta chamada %s".formatted(name));
                });
        goal.setName(name);
        goal.setTargetAmount(request.targetAmount());
        goal.setDeadline(request.deadline());
        if (request.savedAmount() != null) {
            goal.changeSaved(request.savedAmount());
        } else {
            // Mudar o alvo pode concluir (ou desfazer a conclusao) sem ninguem depositar nada
            goal.changeSaved(goal.getSavedAmount());
        }
        return toProgress(goalRepository.save(goal));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        goalRepository.delete(getOrThrow(userId, id));
        log.info("Meta removida: id={} user={}", id, userId);
    }

    @Transactional
    public SavingsGoalResponse deposit(UUID userId, UUID id, BigDecimal amount) {
        return depositOn(getOrThrow(userId, id), amount, userId);
    }

    /** Usado pela Lumi: a pessoa fala o nome da meta, nao um id. */
    @Transactional
    public SavingsGoalResponse depositByName(UUID userId, String name, BigDecimal amount) {
        var goal = goalRepository.findByUserIdAndNameIgnoreCase(userId, name == null ? "" : name.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "não encontrei uma meta chamada %s; crie a meta primeiro".formatted(name)));
        return depositOn(goal, amount, userId);
    }

    private SavingsGoalResponse depositOn(SavingsGoal goal, BigDecimal amount, UUID userId) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("o valor guardado deve ser maior que zero");
        }
        goal.deposit(amount);
        log.info("Depósito na meta: id={} user={} valor={} total={}", goal.getId(), userId, amount, goal.getSavedAmount());
        return toProgress(goalRepository.save(goal));
    }

    SavingsGoalResponse toProgress(SavingsGoal goal) {
        var today = LocalDate.now(clock);
        var target = goal.getTargetAmount();
        var saved = goal.getSavedAmount();
        var remaining = target.subtract(saved).max(BigDecimal.ZERO);
        var percentage = saved.multiply(BigDecimal.valueOf(100)).divide(target, 1, RoundingMode.HALF_UP);

        var status = goal.isCompleted() ? GoalStatus.COMPLETED
                : goal.getDeadline() != null && goal.getDeadline().isBefore(today) ? GoalStatus.OVERDUE
                : GoalStatus.IN_PROGRESS;

        Long monthsLeft = null;
        BigDecimal suggestedMonthly = null;
        if (goal.getDeadline() != null && status == GoalStatus.IN_PROGRESS) {
            // Conta os meses que faltam pelo calendario; o mes corrente conta como um
            monthsLeft = Math.max(1, YearMonth.from(today).until(YearMonth.from(goal.getDeadline()),
                    java.time.temporal.ChronoUnit.MONTHS) + 1);
            suggestedMonthly = remaining.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.UP);
        }

        var message = switch (status) {
            case COMPLETED -> "meta %s concluída: %s reais guardados".formatted(goal.getName(), saved.toPlainString());
            case OVERDUE -> "a meta %s venceu em %s com %s%% guardado, faltavam %s reais"
                    .formatted(goal.getName(), goal.getDeadline(), percentage.toPlainString(), remaining.toPlainString());
            case IN_PROGRESS -> suggestedMonthly == null
                    ? "você já guardou %s%% da meta %s, faltam %s reais"
                            .formatted(percentage.toPlainString(), goal.getName(), remaining.toPlainString())
                    : "você já guardou %s%% da meta %s, faltam %s reais, cerca de %s reais por mês até %s"
                            .formatted(percentage.toPlainString(), goal.getName(), remaining.toPlainString(),
                                    suggestedMonthly.toPlainString(), goal.getDeadline());
        };

        return new SavingsGoalResponse(goal.getId(), goal.getName(), target, saved, remaining, percentage,
                goal.getDeadline(), monthsLeft, suggestedMonthly, status, status.getLabel(), message);
    }

    private void validate(GoalRequest request) {
        if (request == null) {
            throw new BusinessException("os dados da meta são obrigatórios");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new BusinessException(violations.stream()
                    .map(ConstraintViolation::getMessage).sorted().collect(Collectors.joining("; ")));
        }
    }

    private SavingsGoal getOrThrow(UUID userId, UUID id) {
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta %s não encontrada".formatted(id)));
    }
}
