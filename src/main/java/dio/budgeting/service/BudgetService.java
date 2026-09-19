package dio.budgeting.service;

import dio.budgeting.dto.request.BudgetRequest;
import dio.budgeting.dto.response.BudgetStatusResponse;
import dio.budgeting.entity.Budget;
import dio.budgeting.entity.BudgetStatus;
import dio.budgeting.entity.Category;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.repository.BudgetRepository;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Orcamento mensal por categoria: limites, situacao (OK / atencao / estourado) e alertas. */
@Slf4j
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final Validator validator;
    private final BigDecimal warningThreshold;
    private final Clock clock;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository,
                         Validator validator,
                         @Value("${app.budget.warning-threshold:80}") BigDecimal warningThreshold,
                         Clock clock) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.validator = validator;
        this.warningThreshold = warningThreshold;
        this.clock = clock;
    }

    /** Cria um orcamento novo; se ja existir um para a categoria no mes, recusa (REST). */
    @Transactional
    public BudgetStatusResponse create(UUID userId, BudgetRequest request) {
        validate(request);
        var month = resolveMonth(request.month());
        if (budgetRepository.existsByUserIdAndCategoryAndReferenceMonth(userId, request.category(), month)) {
            throw new BusinessException("já existe um orçamento de %s para %s"
                    .formatted(request.category().getLabel(), formatMonth(month)));
        }
        var saved = budgetRepository.save(new Budget(
                userRepository.getReferenceById(userId), request.category(), month, request.monthlyLimit()));
        log.info("Orçamento criado: id={} user={} categoria={} mês={} limite={}",
                saved.getId(), userId, saved.getCategory(), formatMonth(month), saved.getMonthlyLimit());
        return toStatus(userId, saved);
    }

    /** Cria ou atualiza: e o que soa natural pela voz ("meu limite de mercado e 800"). */
    @Transactional
    public BudgetStatusResponse upsert(UUID userId, BudgetRequest request) {
        validate(request);
        var month = resolveMonth(request.month());
        var budget = budgetRepository.findByUserIdAndCategoryAndReferenceMonth(userId, request.category(), month)
                .map(existing -> {
                    existing.setMonthlyLimit(request.monthlyLimit());
                    return existing;
                })
                .orElseGet(() -> new Budget(
                        userRepository.getReferenceById(userId), request.category(), month, request.monthlyLimit()));
        var saved = budgetRepository.save(budget);
        log.info("Orçamento definido: id={} user={} categoria={} mês={} limite={}",
                saved.getId(), userId, saved.getCategory(), formatMonth(month), saved.getMonthlyLimit());
        return toStatus(userId, saved);
    }

    @Transactional
    public BudgetStatusResponse update(UUID userId, UUID id, BigDecimal monthlyLimit) {
        var budget = getOrThrow(userId, id);
        validate(new BudgetRequest(budget.getCategory(), monthlyLimit, null));
        budget.setMonthlyLimit(monthlyLimit);
        return toStatus(userId, budgetRepository.save(budget));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        budgetRepository.delete(getOrThrow(userId, id));
        log.info("Orçamento removido: id={} user={}", id, userId);
    }

    @Transactional(readOnly = true)
    public List<BudgetStatusResponse> list(UUID userId, String month) {
        var reference = resolveMonth(month);
        return budgetRepository.findAllByUserIdAndReferenceMonthOrderByCategory(userId, reference)
                .stream().map(b -> toStatus(userId, b)).toList();
    }

    @Transactional(readOnly = true)
    public BudgetStatusResponse status(UUID userId, Category category, String month) {
        var reference = resolveMonth(month);
        return budgetRepository.findByUserIdAndCategoryAndReferenceMonth(userId, category, reference)
                .map(b -> toStatus(userId, b))
                .orElseThrow(() -> new ResourceNotFoundException("não há orçamento de %s para %s"
                        .formatted(category.getLabel(), formatMonth(reference))));
    }

    /** Somente as categorias em atencao ou estouradas. */
    @Transactional(readOnly = true)
    public List<BudgetStatusResponse> alerts(UUID userId, String month) {
        return list(userId, month).stream().filter(s -> s.status() != BudgetStatus.OK).toList();
    }

    /** Situacao do orcamento da categoria no mes do gasto, se houver um. Usado logo apos registrar. */
    @Transactional(readOnly = true)
    public Optional<BudgetStatusResponse> checkAfterExpense(UUID userId, Category category, LocalDate expenseDate) {
        return budgetRepository.findByUserIdAndCategoryAndReferenceMonth(userId, category, expenseDate.withDayOfMonth(1))
                .map(b -> toStatus(userId, b));
    }

    private BudgetStatusResponse toStatus(UUID userId, Budget budget) {
        var start = budget.getReferenceMonth();
        var end = start.plusMonths(1).minusDays(1);
        var spent = transactionRepository.sumAmountByCategory(userId, budget.getCategory(), start, end);
        var limit = budget.getMonthlyLimit();
        var remaining = limit.subtract(spent);
        var used = spent.multiply(BigDecimal.valueOf(100)).divide(limit, 1, RoundingMode.HALF_UP);
        // O status usa os valores exatos, nao o percentual arredondado: 399,99 de 500 ainda esta OK
        var status = statusFor(spent, limit);
        var label = budget.getCategory().getLabel();
        var message = switch (status) {
            case OK -> "você usou %s%% do limite de %s neste mês".formatted(used.toPlainString(), label);
            case WARNING -> "atenção: você já usou %s%% do limite de %s neste mês, restam %s reais"
                    .formatted(used.toPlainString(), label, remaining.toPlainString());
            case EXCEEDED -> "o limite de %s deste mês foi estourado: %s reais gastos de %s"
                    .formatted(label, spent.toPlainString(), limit.toPlainString());
        };
        return new BudgetStatusResponse(budget.getId(), budget.getCategory(), label, formatMonth(start),
                limit, spent, remaining, used, status, status.getLabel(), message);
    }

    BudgetStatus statusFor(BigDecimal spent, BigDecimal limit) {
        if (spent.compareTo(limit) >= 0) {
            return BudgetStatus.EXCEEDED;
        }
        // spent / limit >= threshold / 100  <=>  spent * 100 >= threshold * limit (sem divisao, sem arredondamento)
        if (spent.multiply(BigDecimal.valueOf(100)).compareTo(warningThreshold.multiply(limit)) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.OK;
    }

    /** "AAAA-MM" -> dia 1 do mes; vazio -> mes atual (no fuso do Clock). */
    LocalDate resolveMonth(String month) {
        if (month == null || month.isBlank()) {
            return LocalDate.now(clock).withDayOfMonth(1);
        }
        try {
            return YearMonth.parse(month.trim()).atDay(1);
        } catch (DateTimeParseException e) {
            throw new BusinessException("mês '%s' inválido, use o formato AAAA-MM".formatted(month));
        }
    }

    static String formatMonth(LocalDate month) {
        return YearMonth.from(month).toString();
    }

    private void validate(BudgetRequest request) {
        if (request == null) {
            throw new BusinessException("os dados do orçamento são obrigatórios");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new BusinessException(violations.stream()
                    .map(ConstraintViolation::getMessage).sorted().collect(Collectors.joining("; ")));
        }
    }

    private Budget getOrThrow(UUID userId, UUID id) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento %s não encontrado".formatted(id)));
    }
}
