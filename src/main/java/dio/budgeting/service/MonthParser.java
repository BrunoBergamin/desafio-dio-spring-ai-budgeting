package dio.budgeting.service;

import dio.budgeting.exception.BusinessException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Converte "AAAA-MM" em data (dia 1) e de volta. Usado pelo orcamento e pelas contas recorrentes. */
public final class MonthParser {

    private MonthParser() {
    }

    /** "AAAA-MM" -> dia 1 do mes; vazio -> mes atual no fuso do relogio. */
    public static LocalDate parse(String month, Clock clock) {
        if (month == null || month.isBlank()) {
            return LocalDate.now(clock).withDayOfMonth(1);
        }
        try {
            return YearMonth.parse(month.trim()).atDay(1);
        } catch (DateTimeParseException e) {
            throw new BusinessException("mês '%s' inválido, use o formato AAAA-MM".formatted(month));
        }
    }

    public static String format(LocalDate month) {
        return YearMonth.from(month).toString();
    }
}
