package dio.budgeting.service;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.Transaction;
import dio.budgeting.entity.User;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.mapper.TransactionMapper;
import dio.budgeting.repository.CategoryTotal;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final UUID USER_ID = UUID.randomUUID();

    /**
     * 01:30 UTC do dia 19 = 22:30 do dia 18 em Brasilia. Se algum ponto do codigo usar o fuso do
     * servidor (UTC no container) em vez do Clock, a data "de hoje" vira 19 e o teste acusa.
     */
    private static final Clock BRAZIL_LATE_NIGHT =
            Clock.fixed(Instant.parse("2026-09-19T01:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 18);

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    UserRepository userRepository;

    TransactionService service;
    User owner = new User("Bruno", "bruno@email.com", "hash");

    @BeforeEach
    void setUp() {
        Validator validator = FACTORY.getValidator();
        service = new TransactionService(transactionRepository, userRepository,
                new TransactionMapper(BRAZIL_LATE_NIGHT), validator, BRAZIL_LATE_NIGHT);
    }

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void should_saveTransactionForTheUser_when_requestIsValid() {
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(USER_ID, new TransactionRequest("Mercado", new BigDecimal("80.50"), Category.GROCERIES, null));

        assertThat(response.amount()).isEqualByComparingTo("80.50");
        assertThat(response.categoryLabel()).isEqualTo("Mercado");
        verify(transactionRepository).save(argThat(t -> t.getUser() == owner));
    }

    @Test
    void should_useBrazilDate_when_dateIsOmittedAndServerIsAlreadyOnTheNextDayInUtc() {
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(USER_ID, new TransactionRequest("Pizza", new BigDecimal("72"), Category.RESTAURANT, null));

        assertThat(response.date()).isEqualTo(TODAY);
    }

    @Test
    void should_rejectTransaction_when_amountIsNotPositive() {
        var request = new TransactionRequest("Mercado", new BigDecimal("-10"), Category.GROCERIES, null);

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maior que zero");
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void should_rejectTransaction_when_dateIsInTheFuture() {
        // @PastOrPresent do Bean Validation usa o relogio real da JVM, entao aqui o "amanha" e o de verdade
        var request = new TransactionRequest("Mercado", BigDecimal.TEN, Category.GROCERIES, LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void should_rejectTransaction_when_descriptionIsBlankAndCategoryMissing() {
        var request = new TransactionRequest(" ", BigDecimal.TEN, null, null);

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("descrição")
                .hasMessageContaining("categoria");
    }

    @Test
    void should_throwNotFound_when_transactionDoesNotExist() {
        var id = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(USER_ID, id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throwNotFound_when_transactionBelongsToAnotherUser() {
        var id = UUID.randomUUID();
        var anotherUser = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(id, anotherUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(anotherUser, id)).isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void should_clampPageSizeAndSortNewestFirst_when_listing() {
        var transaction = new Transaction(owner, "Mercado", BigDecimal.TEN, Category.GROCERIES, TODAY);
        when(transactionRepository.findAllByUserId(eq(USER_ID), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(transaction), inv.getArgument(1, Pageable.class), 1));

        var page = service.list(USER_ID, null, null, null, -3, 9_999);

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAllByUserId(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(TransactionService.MAX_PAGE_SIZE);
        assertThat(captor.getValue().getSort().getOrderFor("date").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(page.content()).extracting(r -> r.description()).containsExactly("Mercado");
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void should_useThePeriodQuery_when_listingWithDates() {
        when(transactionRepository.findAllByUserIdAndDateBetween(eq(USER_ID), eq(LocalDate.of(2026, 9, 1)), eq(TODAY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        var page = service.list(USER_ID, null, LocalDate.of(2026, 9, 1), null, 0, 50);

        assertThat(page.content()).isEmpty();
        assertThat(page.size()).isEqualTo(50);
    }

    @Test
    void should_rejectPeriod_when_startIsAfterEnd() {
        assertThatThrownBy(() -> service.summary(USER_ID, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_calculateTotalsAndPercentages_when_summaryIsRequested() {
        var start = LocalDate.of(2026, 9, 1);
        var end = LocalDate.of(2026, 9, 30);
        when(transactionRepository.sumByCategoryBetween(USER_ID, start, end)).thenReturn(List.of(
                total(Category.GROCERIES, "150.00", 2),
                total(Category.PHARMA, "50.00", 1)));

        var summary = service.summary(USER_ID, start, end);

        assertThat(summary.total()).isEqualByComparingTo("200.00");
        assertThat(summary.quantity()).isEqualTo(3);
        assertThat(summary.categories()).hasSize(2);
        assertThat(summary.categories().getFirst().percentage()).isEqualByComparingTo("75.0");
        assertThat(summary.categories().get(1).percentage()).isEqualByComparingTo("25.0");
    }

    @Test
    void should_useCurrentMonthInBrazilTime_when_summaryHasNoDates() {
        when(transactionRepository.sumByCategoryBetween(USER_ID, LocalDate.of(2026, 9, 1), TODAY)).thenReturn(List.of());

        var summary = service.summary(USER_ID, null, null);

        assertThat(summary.start()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(summary.end()).isEqualTo(TODAY);
        assertThat(summary.total()).isEqualByComparingTo("0");
    }

    private CategoryTotal total(Category category, String total, long quantity) {
        return new CategoryTotal() {
            public Category getCategory() { return category; }
            public BigDecimal getTotal() { return new BigDecimal(total); }
            public Long getQuantity() { return quantity; }
        };
    }
}
