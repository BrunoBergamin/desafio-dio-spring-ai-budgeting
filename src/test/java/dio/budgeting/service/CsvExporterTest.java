package dio.budgeting.service;

import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    private TransactionResponse transaction(String description, String amount, Category category,
                                            TransactionType type, LocalDate date, UUID recurringId) {
        return new TransactionResponse(UUID.randomUUID(), description, new BigDecimal(amount),
                category, category.getLabel(), type, recurringId, date);
    }

    private String asText(byte[] csv) {
        return new String(csv, StandardCharsets.UTF_8);
    }

    @Test
    void should_startWithTheUtf8Bom_so_excelDoesNotBreakAccents() {
        var csv = CsvExporter.toCsv(List.of());

        // EF BB BF: sem isto o Excel em portugues mostra "FarmÃ¡cia" no lugar de "Farmácia"
        assertThat(csv).startsWith(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        assertThat(asText(csv)).contains(CsvExporter.HEADER).endsWith("\r\n");
    }

    @Test
    void should_useBrazilianFormat_when_writingDateAndAmount() {
        var csv = asText(CsvExporter.toCsv(List.of(
                transaction("Mercado", "80.50", Category.GROCERIES, TransactionType.EXPENSE, LocalDate.of(2026, 9, 15), null))));

        assertThat(csv).contains("15/09/2026;Gasto;Mercado;Mercado;80,50;não");
    }

    @Test
    void should_markIncomeAndRecurring_when_theyApply() {
        var csv = asText(CsvExporter.toCsv(List.of(
                transaction("Salário", "5200.00", Category.SALARY, TransactionType.INCOME, LocalDate.of(2026, 9, 5), UUID.randomUUID()))));

        assertThat(csv).contains(";Receita;").contains(";sim");
    }

    @Test
    void should_quoteTheField_when_itContainsTheSeparatorOrQuotes() {
        var csv = asText(CsvExporter.toCsv(List.of(
                transaction("Pão; integral", "9.90", Category.GROCERIES, TransactionType.EXPENSE, LocalDate.of(2026, 9, 2), null),
                transaction("Livro \"Clean Code\"", "89.00", Category.EDUCATION, TransactionType.EXPENSE, LocalDate.of(2026, 9, 3), null))));

        assertThat(csv).contains("\"Pão; integral\"");
        assertThat(csv).contains("\"Livro \"\"Clean Code\"\"\"");
    }

    @Test
    void should_sortFromOldestToNewest_because_aSpreadsheetIsReadDownwards() {
        var csv = asText(CsvExporter.toCsv(List.of(
                transaction("Mais novo", "10.00", Category.OTHER, TransactionType.EXPENSE, LocalDate.of(2026, 9, 20), null),
                transaction("Mais antigo", "10.00", Category.OTHER, TransactionType.EXPENSE, LocalDate.of(2026, 9, 1), null))));

        assertThat(csv.indexOf("Mais antigo")).isLessThan(csv.indexOf("Mais novo"));
    }
}
