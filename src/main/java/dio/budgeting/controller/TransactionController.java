package dio.budgeting.controller;

import dio.budgeting.dto.request.TransactionRequest;
import dio.budgeting.dto.response.PageResponse;
import dio.budgeting.dto.response.SpendingSummaryResponse;
import dio.budgeting.dto.response.TransactionResponse;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.TransactionType;
import dio.budgeting.security.CurrentUserProvider;
import dio.budgeting.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Transações", description = "CRUD e consultas de gastos do usuário autenticado")
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentUserProvider currentUser;

    @Operation(summary = "Registra um lançamento (gasto ou receita, conforme a categoria)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lançamento registrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        var created = transactionService.create(currentUser.requireUserId(), request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Lista lançamentos, paginados, com filtros opcionais de tipo, categoria e período")
    @GetMapping
    public PageResponse<TransactionResponse> list(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return transactionService.list(currentUser.requireUserId(), type, category, start, end, page, size);
    }

    @Operation(summary = "Resumo de gastos por categoria no período (padrão: mês atual)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo calculado"),
            @ApiResponse(responseCode = "422", description = "Data inicial posterior à final", content = @Content)
    })
    @GetMapping("/summary")
    public SpendingSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return transactionService.summary(currentUser.requireUserId(), start, end);
    }

    @Operation(summary = "Busca um gasto pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gasto encontrado"),
            @ApiResponse(responseCode = "404", description = "Gasto não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public TransactionResponse findById(@PathVariable UUID id) {
        return transactionService.findById(currentUser.requireUserId(), id);
    }

    @Operation(summary = "Atualiza um gasto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gasto atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Gasto não encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable UUID id, @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(currentUser.requireUserId(), id, request);
    }

    @Operation(summary = "Remove um gasto")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Gasto removido"),
            @ApiResponse(responseCode = "404", description = "Gasto não encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        transactionService.delete(currentUser.requireUserId(), id);
    }
}
