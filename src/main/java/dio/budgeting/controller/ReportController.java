package dio.budgeting.controller;

import dio.budgeting.dto.response.MonthlyReportResponse;
import dio.budgeting.security.CurrentUserProvider;
import dio.budgeting.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Relatórios", description = "Fechamento do mês: entrou, saiu, sobrou e onde foi parar")
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserProvider currentUser;

    @Operation(summary = "Relatório do mês: receitas, gastos, saldo, maiores gastos, orçamentos e metas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório do mês"),
            @ApiResponse(responseCode = "422", description = "Mês fora do formato AAAA-MM", content = @Content)
    })
    @GetMapping("/monthly")
    public MonthlyReportResponse monthly(@RequestParam(required = false) String month) {
        return reportService.monthly(currentUser.requireUserId(), month);
    }
}
