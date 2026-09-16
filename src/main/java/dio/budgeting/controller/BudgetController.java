package dio.budgeting.controller;

import dio.budgeting.dto.request.BudgetRequest;
import dio.budgeting.dto.response.BudgetStatusResponse;
import dio.budgeting.security.CurrentUserProvider;
import dio.budgeting.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Orçamentos", description = "Limite mensal por categoria e alertas")
@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final CurrentUserProvider currentUser;

    public record LimitRequest(
            @NotNull(message = "o limite é obrigatório")
            @DecimalMin(value = "0.01", message = "o limite deve ser maior que zero")
            BigDecimal monthlyLimit) {
    }

    @Operation(summary = "Define o limite mensal de uma categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orçamento criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "422", description = "Já existe orçamento para a categoria no mês", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetStatusResponse create(@Valid @RequestBody BudgetRequest request) {
        return budgetService.create(currentUser.requireUserId(), request);
    }

    @Operation(summary = "Lista os orçamentos do mês com quanto já foi gasto (padrão: mês atual)")
    @GetMapping
    public List<BudgetStatusResponse> list(@RequestParam(required = false) String month) {
        return budgetService.list(currentUser.requireUserId(), month);
    }

    @Operation(summary = "Só as categorias em atenção ou estouradas")
    @GetMapping("/alerts")
    public List<BudgetStatusResponse> alerts(@RequestParam(required = false) String month) {
        return budgetService.alerts(currentUser.requireUserId(), month);
    }

    @Operation(summary = "Altera o limite de um orçamento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Limite atualizado"),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public BudgetStatusResponse update(@PathVariable UUID id, @Valid @RequestBody LimitRequest request) {
        return budgetService.update(currentUser.requireUserId(), id, request.monthlyLimit());
    }

    @Operation(summary = "Remove um orçamento")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Orçamento removido"),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        budgetService.delete(currentUser.requireUserId(), id);
    }
}
