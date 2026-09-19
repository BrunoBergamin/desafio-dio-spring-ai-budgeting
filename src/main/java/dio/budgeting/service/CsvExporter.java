package dio.budgeting.service;

import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.TransactionType;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Gera o CSV dos lancamentos sem biblioteca nenhuma. Os detalhes existem por causa do Excel em portugues:
 * BOM para ele reconhecer o UTF-8 (senao "Farmácia" vira "FarmÃ¡cia"), ponto e virgula como separador
 * (a virgula ja e o decimal no Brasil) e CRLF no fim das linhas.
 */
public final class CsvExporter {

    static final String HEADER = "Data;Tipo;Descrição;Categoria;Valor;Recorrente";
    private static final String BOM = "﻿";
    private static final String CRLF = "\r\n";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private CsvExporter() {
    }

    public static byte[] toCsv(List<TransactionResponse> transactions) {
        var csv = new StringBuilder(BOM).append(HEADER).append(CRLF);
        transactions.stream()
                // Planilha se le de cima para baixo no tempo, ao contrario da tela
                .sorted(Comparator.comparing(TransactionResponse::date))
                .forEach(t -> csv
                        .append(t.date().format(BR_DATE)).append(';')
                        .append(t.type() == TransactionType.INCOME ? "Receita" : "Gasto").append(';')
                        .append(escape(t.description())).append(';')
                        .append(escape(t.categoryLabel())).append(';')
                        .append(t.amount().toPlainString().replace('.', ',')).append(';')
                        .append(t.recurringId() != null ? "sim" : "não")
                        .append(CRLF));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Campo com separador, aspas ou quebra de linha vai entre aspas, e aspas de dentro viram duas. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(';') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
