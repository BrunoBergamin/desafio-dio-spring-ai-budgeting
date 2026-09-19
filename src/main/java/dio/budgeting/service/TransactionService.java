package dio.budgeting.service;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.dto.response.CategorySummaryResponse;
import dio.budgeting.dto.response.PageResponse;
import dio.budgeting.dto.response.SpendingSummaryResponse;
import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.Transaction;
import dio.budgeting.entity.TransactionType;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.mapper.TransactionMapper;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Regras de negocio das transacoes. O {@code userId} e sempre o primeiro parametro e nao tem
 * sobrecarga sem ele: esquecer o usuario nao compila.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    /** Teto do tamanho da pagina: acima disso a resposta fica pesada e o cliente deveria filtrar por periodo. */
    public static final int MAX_PAGE_SIZE = 500;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt"));

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;
    private final Validator validator;
    private final Clock clock;

    @Transactional
    public TransactionResponse create(UUID userId, TransactionRequest request) {
        validate(request);
        // O id vem de um JWT ja validado: a referencia evita um SELECT extra
        var owner = userRepository.getReferenceById(userId);
        var saved = transactionRepository.save(transactionMapper.toEntity(owner, request));
        log.info("Transação criada: id={} user={} valor={} categoria={}",
                saved.getId(), userId, saved.getAmount(), saved.getCategory());
        return transactionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID userId, UUID id) {
        return transactionMapper.toResponse(getOrThrow(userId, id));
    }

    /**
     * Lista paginada, dos mais recentes para os mais antigos. Filtros nulos nao filtram. Sem datas, traz
     * todo o historico (paginado); com uma das datas, aplica as mesmas regras de periodo do resumo.
     */
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(UUID userId, TransactionType type, Category category,
                                                  LocalDate start, LocalDate end, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
        LocalDate from = null;
        LocalDate to = null;
        if (start != null || end != null) {
            var period = resolvePeriod(start, end);
            from = period.start();
            to = period.end();
        }
        return PageResponse.from(
                transactionRepository.search(userId, type, category, from, to, pageable),
                transactionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> recent(UUID userId) {
        return transactionRepository.findTop5ByUserIdOrderByDateDescCreatedAtDesc(userId)
                .stream().map(transactionMapper::toResponse).toList();
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID id, TransactionRequest request) {
        validate(request);
        var transaction = getOrThrow(userId, id);
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.changeCategory(request.category());
        if (request.date() != null) {
            transaction.setDate(request.date());
        }
        log.info("Transação atualizada: id={} user={}", id, userId);
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        transactionRepository.delete(getOrThrow(userId, id));
        log.info("Transação removida: id={} user={}", id, userId);
    }

    /** Gastos por categoria, total recebido e saldo do periodo. */
    @Transactional(readOnly = true)
    public SpendingSummaryResponse summary(UUID userId, LocalDate start, LocalDate end) {
        var period = resolvePeriod(start, end);
        var totals = transactionRepository.sumByCategoryBetween(
                userId, TransactionType.EXPENSE, period.start(), period.end());

        var expenses = totals.stream().map(t -> t.getTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
        var quantity = totals.stream().mapToLong(t -> t.getQuantity()).sum();
        var income = transactionRepository.sumAmountByType(
                userId, TransactionType.INCOME, period.start(), period.end());

        var categories = totals.stream()
                .map(t -> new CategorySummaryResponse(
                        t.getCategory(),
                        t.getCategory().getLabel(),
                        t.getTotal(),
                        t.getQuantity(),
                        percentage(t.getTotal(), expenses)))
                .toList();

        return new SpendingSummaryResponse(period.start(), period.end(), expenses, income,
                income.subtract(expenses), quantity, categories);
    }

    /** Total gasto pelo usuario em uma categoria dentro do periodo (usado pelo orcamento). */
    @Transactional(readOnly = true)
    public BigDecimal totalByCategory(UUID userId, Category category, LocalDate start, LocalDate end) {
        return transactionRepository.sumAmountByCategory(userId, category, start, end);
    }

    /**
     * Aplica as mesmas regras do DTO também quando a chamada não passa pelo controller
     * (ex.: quando a IA chama a ferramenta diretamente via Tool Calling).
     */
    private void validate(TransactionRequest request) {
        if (request == null) {
            throw new BusinessException("os dados da transação são obrigatórios");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            var message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new BusinessException(message);
        }
    }

    /** Sem datas: mês atual. Só início: até hoje. Só fim: desde o primeiro dia do mês do fim. */
    private Period resolvePeriod(LocalDate start, LocalDate end) {
        var today = LocalDate.now(clock);
        var resolvedEnd = end != null ? end : today;
        var resolvedStart = start != null ? start : resolvedEnd.withDayOfMonth(1);
        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new BusinessException("a data inicial deve ser anterior ou igual à data final");
        }
        return new Period(resolvedStart, resolvedEnd);
    }

    private BigDecimal percentage(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
    }

    /** Transacao de outro usuario devolve 404, nao 403: nao revela que o id existe. */
    private Transaction getOrThrow(UUID userId, UUID id) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transação %s não encontrada".formatted(id)));
    }

    private record Period(LocalDate start, LocalDate end) {
    }
}
