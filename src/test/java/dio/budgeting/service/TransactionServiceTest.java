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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    UserRepository userRepository;

    TransactionService service;
    User owner = new User("Bruno", "bruno@email.com", "hash");

    @BeforeEach
    void setUp() {
        Validator validator = FACTORY.getValidator();
        service = new TransactionService(transactionRepository, userRepository, new TransactionMapper(), validator);
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
        assertThat(response.date()).isEqualTo(LocalDate.now());
        verify(transactionRepository).save(argThat(t -> t.getUser() == owner));
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
    void should_useCurrentMonth_when_summaryHasNoDates() {
        var today = LocalDate.now();
        when(transactionRepository.sumByCategoryBetween(USER_ID, today.withDayOfMonth(1), today)).thenReturn(List.of());

        var summary = service.summary(USER_ID, null, null);

        assertThat(summary.start()).isEqualTo(today.withDayOfMonth(1));
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
