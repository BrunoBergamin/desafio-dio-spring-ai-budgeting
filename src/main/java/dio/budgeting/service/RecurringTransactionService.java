package dio.budgeting.service;

import dio.budgeting.dto.request.RecurringRequest;
import dio.budgeting.dto.response.RecurringResponse;
import dio.budgeting.entity.RecurringTransaction;
import dio.budgeting.entity.Transaction;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.repository.RecurringTransactionRepository;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Contas que se repetem todo mes. A regra fica guardada uma vez; o lancamento de cada mes e criado
 * pelo {@link RecurringTransactionScheduler} (ou logo na criacao da regra, se o dia do mes ja passou).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final Validator validator;
    private final Clock clock;

    @Transactional
    public RecurringResponse create(UUID userId, RecurringRequest request) {
        validate(request);
        var start = MonthParser.parse(request.startMonth(), clock);
        var end = request.endMonth() == null ? null : MonthParser.parse(request.endMonth(), clock);
        if (end != null && end.isBefore(start)) {
            throw new BusinessException("o mês final deve ser igual ou posterior ao inicial");
        }
        var rule = new RecurringTransaction(userRepository.getReferenceById(userId), request.description().trim(),
                request.amount(), request.category(), request.dayOfMonth(), start, end);
        if (Boolean.FALSE.equals(request.active())) {
            rule.setActive(false);
        }
        var saved = recurringRepository.save(rule);
        log.info("Conta recorrente criada: id={} user={} dia={} categoria={}",
                saved.getId(), userId, saved.getDayOfMonth(), saved.getCategory());
        // Se o dia deste mes ja passou, o lancamento aparece na hora, sem esperar o mes que vem
        materializeUpTo(saved, LocalDate.now(clock));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RecurringResponse> list(UUID userId) {
        return recurringRepository.findAllByUserIdOrderByDayOfMonth(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public RecurringResponse update(UUID userId, UUID id, RecurringRequest request) {
        validate(request);
        var rule = getOrThrow(userId, id);
        var wasActive = rule.isActive();

        rule.setDescription(request.description().trim());
        rule.setAmount(request.amount());
        rule.changeCategory(request.category());
        rule.setDayOfMonth(request.dayOfMonth());
        rule.setEndMonth(request.endMonth() == null ? null : MonthParser.parse(request.endMonth(), clock));
        if (request.startMonth() != null) {
            rule.setStartMonth(MonthParser.parse(request.startMonth(), clock));
        }
        if (request.active() != null) {
            rule.setActive(request.active());
            if (!wasActive && request.active()) {
                // Retomar nao gera os meses em que esteve pausada: recomeca do mes atual
                rule.setLastGeneratedMonth(YearMonth.from(LocalDate.now(clock)).minusMonths(1).atDay(1));
            }
        }
        var saved = recurringRepository.save(rule);
        log.info("Conta recorrente atualizada: id={} user={} ativa={}", id, userId, saved.isActive());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        var rule = getOrThrow(userId, id);
        // Os lançamentos já criados continuam no histórico, só deixam de apontar para a regra
        var unlinked = transactionRepository.unlinkRecurring(id);
        recurringRepository.delete(rule);
        log.info("Conta recorrente removida: id={} user={} ({} lançamentos mantidos no histórico)", id, userId, unlinked);
    }

    /**
     * Cria os lancamentos que ja venceram, de todas as regras ativas. Roda uma vez por dia e na subida da
     * aplicacao; e seguro chamar quantas vezes quiser, porque cada regra so avanca o mes uma vez.
     */
    @Transactional
    public int generateDue() {
        var today = LocalDate.now(clock);
        var created = 0;
        for (var rule : recurringRepository.findAllByActiveTrue()) {
            try {
                created += materializeUpTo(rule, today);
            } catch (RuntimeException e) {
                // Uma regra com problema nao pode impedir as outras de rodar
                log.error("[recorrente] falha ao gerar a conta {}", rule.getId(), e);
            }
        }
        if (created > 0) {
            log.info("[recorrente] {} lançamentos criados", created);
        }
        return created;
    }

    /**
     * Gera os meses que faltam entre o ultimo gerado e hoje. Volta o quanto criou.
     * <p>
     * Cobre os tres casos num laco so: primeira vez (nunca gerou), dia ainda nao chegou (para no mes atual
     * e nao grava nada) e aplicacao parada por dias ou meses (gera cada mes que passou, na data correta).
     */
    private int materializeUpTo(RecurringTransaction rule, LocalDate today) {
        var currentMonth = YearMonth.from(today);
        var month = rule.getLastGeneratedMonth() == null
                ? YearMonth.from(rule.getStartMonth())
                : YearMonth.from(rule.getLastGeneratedMonth()).plusMonths(1);
        var lastAllowed = rule.getEndMonth() == null ? currentMonth : YearMonth.from(rule.getEndMonth());

        var created = 0;
        while (!month.isAfter(currentMonth) && !month.isAfter(lastAllowed)) {
            var date = rule.occurrenceIn(month);
            if (date.isAfter(today)) {
                break; // o dia deste mes ainda nao chegou; fica para a proxima rodada
            }
            var transaction = new Transaction(rule.getUser(), rule.getDescription(), rule.getAmount(),
                    rule.getCategory(), date);
            transaction.setRecurring(rule);
            transactionRepository.save(transaction);
            rule.setLastGeneratedMonth(month.atDay(1));
            created++;
            month = month.plusMonths(1);
        }
        if (created > 0) {
            recurringRepository.save(rule);
            log.info("[recorrente] {} lançamento(s) de '{}' criados até {}", created, rule.getDescription(), today);
        }
        return created;
    }

    /** Proxima data em que a conta vira lancamento; nulo quando esta pausada ou ja acabou. */
    LocalDate nextOccurrence(RecurringTransaction rule) {
        if (!rule.isActive()) {
            return null;
        }
        var today = LocalDate.now(clock);
        var next = rule.getLastGeneratedMonth() == null
                ? YearMonth.from(rule.getStartMonth())
                : YearMonth.from(rule.getLastGeneratedMonth()).plusMonths(1);
        if (next.isBefore(YearMonth.from(today))) {
            next = YearMonth.from(today);
        }
        if (rule.occurrenceIn(next).isBefore(today)) {
            next = next.plusMonths(1);
        }
        if (rule.getEndMonth() != null && next.isAfter(YearMonth.from(rule.getEndMonth()))) {
            return null;
        }
        return rule.occurrenceIn(next);
    }

    RecurringResponse toResponse(RecurringTransaction rule) {
        var next = nextOccurrence(rule);
        var label = rule.getCategory().getLabel();
        var message = !rule.isActive()
                ? "a conta %s está pausada".formatted(rule.getDescription())
                : next == null
                ? "a conta %s já passou do mês final".formatted(rule.getDescription())
                : "%s de %s reais todo dia %d, em %s; a próxima é em %s".formatted(
                        rule.getDescription(), rule.getAmount().toPlainString(), rule.getDayOfMonth(), label, next);
        return new RecurringResponse(rule.getId(), rule.getDescription(), rule.getAmount(), rule.getCategory(),
                label, rule.getType(), rule.getDayOfMonth(), rule.isActive(),
                MonthParser.format(rule.getStartMonth()),
                rule.getEndMonth() == null ? null : MonthParser.format(rule.getEndMonth()),
                next, message);
    }

    private void validate(RecurringRequest request) {
        if (request == null) {
            throw new BusinessException("os dados da conta recorrente são obrigatórios");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new BusinessException(violations.stream()
                    .map(ConstraintViolation::getMessage).sorted().collect(Collectors.joining("; ")));
        }
    }

    private RecurringTransaction getOrThrow(UUID userId, UUID id) {
        return recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta recorrente %s não encontrada".formatted(id)));
    }
}
