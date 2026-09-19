package dio.budgeting.service;

import dio.budgeting.dto.response.BudgetStatusResponse;
import dio.budgeting.dto.response.CategorySummaryResponse;
import dio.budgeting.dto.response.SpendingSummaryResponse;
import dio.budgeting.entity.BudgetStatus;
import dio.budgeting.entity.Category;
import dio.budgeting.mapper.TransactionMapper;
import dio.budgeting.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final Clock SEPTEMBER = Clock.fixed(Instant.parse("2026-09-19T15:00:00Z"), SP);
    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 30);

    @Mock TransactionService transactionService;
    @Mock BudgetService budgetService;
    @Mock SavingsGoalService goalService;
    @Mock TransactionRepository transactionRepository;

    ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(transactionService, budgetService, goalService,
                transactionRepository, new TransactionMapper(SEPTEMBER), SEPTEMBER);
        lenient().when(transactionRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage());
        lenient().when(budgetService.list(any(), any())).thenReturn(List.of());
        lenient().when(goalService.list(any())).thenReturn(List.of());
    }

    private Page<dio.budgeting.entity.Transaction> emptyPage() {
        return new PageImpl<>(List.of());
    }

    private SpendingSummaryResponse summary(String expenses, String income, CategorySummaryResponse... categories) {
        return new SpendingSummaryResponse(START, END, new BigDecimal(expenses), new BigDecimal(income),
                new BigDecimal(income).subtract(new BigDecimal(expenses)), categories.length, List.of(categories));
    }

    private CategorySummaryResponse category(Category category, String total) {
        return new CategorySummaryResponse(category, category.getLabel(), new BigDecimal(total), 1, new BigDecimal("10.0"));
    }

    private void stubMonths(SpendingSummaryResponse current, SpendingSummaryResponse previous) {
        when(transactionService.summary(USER_ID, START, END)).thenReturn(current);
        when(transactionService.summary(USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))).thenReturn(previous);
    }

    @Test
    void should_useTheCurrentMonth_when_noMonthIsGiven() {
        stubMonths(summary("1000.00", "5000.00"), summary("800.00", "5000.00"));

        var report = service.monthly(USER_ID, null);

        assertThat(report.month()).isEqualTo("2026-09");
        assertThat(report.start()).isEqualTo(START);
        assertThat(report.end()).isEqualTo(END);
        assertThat(report.balance()).isEqualByComparingTo("4000.00");
    }

    @Test
    void should_showHowMuchSpendingGrew_against_thePreviousMonth() {
        stubMonths(summary("1000.00", "5000.00"), summary("800.00", "5000.00"));

        assertThat(service.monthly(USER_ID, "2026-09").expensesDeltaPercentage()).isEqualByComparingTo("25.0");
    }

    @Test
    void should_showANegativeDelta_when_spendingWentDown() {
        stubMonths(summary("600.00", "5000.00"), summary("800.00", "5000.00"));

        assertThat(service.monthly(USER_ID, "2026-09").expensesDeltaPercentage()).isEqualByComparingTo("-25.0");
    }

    @Test
    void should_leaveTheDeltaEmpty_when_thereWasNothingToCompare() {
        stubMonths(summary("1000.00", "5000.00"), summary("0", "0"));

        var report = service.monthly(USER_ID, "2026-09");

        assertThat(report.expensesDeltaPercentage()).isNull();
        assertThat(report.previousExpenses()).isEqualByComparingTo("0");
    }

    @Test
    void should_keepOnlyTheTopThreeCategories() {
        stubMonths(summary("1000.00", "0",
                category(Category.GROCERIES, "400"), category(Category.HOUSING, "300"),
                category(Category.RESTAURANT, "200"), category(Category.LEISURE, "100")), summary("0", "0"));

        assertThat(service.monthly(USER_ID, "2026-09").topCategories())
                .extracting(CategorySummaryResponse::category)
                .containsExactly(Category.GROCERIES, Category.HOUSING, Category.RESTAURANT);
    }

    @Test
    void should_countHowManyBudgetsNeedAttention() {
        stubMonths(summary("1000.00", "0"), summary("0", "0"));
        when(budgetService.list(USER_ID, "2026-09")).thenReturn(List.of(
                budget(BudgetStatus.OK), budget(BudgetStatus.WARNING), budget(BudgetStatus.EXCEEDED)));

        var report = service.monthly(USER_ID, "2026-09");

        assertThat(report.budgets()).hasSize(3);
        assertThat(report.budgetsInAlert()).isEqualTo(2);
    }

    private BudgetStatusResponse budget(BudgetStatus status) {
        return new BudgetStatusResponse(UUID.randomUUID(), Category.GROCERIES, "Mercado", "2026-09",
                new BigDecimal("500"), new BigDecimal("100"), new BigDecimal("400"), new BigDecimal("20.0"),
                status, status.getLabel(), "mensagem");
    }
}
