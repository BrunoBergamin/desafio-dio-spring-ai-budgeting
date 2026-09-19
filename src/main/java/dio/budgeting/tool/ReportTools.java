package dio.budgeting.tool;

import dio.budgeting.dto.response.MonthlyReportResponse;
import dio.budgeting.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** O fechamento do mes numa chamada so, para a Lumi narrar em vez de somar na mao. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportTools {

    private final ReportService reportService;

    @Tool(name = "relatorio_mensal",
            description = "Fechamento do mês numa chamada só: total recebido, total gasto, saldo, variação "
                    + "contra o mês anterior, maiores categorias e gastos, orçamentos e metas. "
                    + "Use quando pedirem um resumo do mês, 'como foi meu mês' ou um balanço geral")
    public MonthlyReportResponse monthlyReport(
            @ToolParam(description = "Mês no formato AAAA-MM. Omita para o mês atual", required = false) String month,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] relatorio_mensal user={} month={}", userId, month);
        return reportService.monthly(userId, month);
    }
}
