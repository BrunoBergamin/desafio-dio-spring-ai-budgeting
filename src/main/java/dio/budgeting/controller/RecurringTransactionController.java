package dio.budgeting.controller;

import dio.budgeting.dto.request.RecurringRequest;
import dio.budgeting.dto.response.RecurringResponse;
import dio.budgeting.security.CurrentUserProvider;
import dio.budgeting.service.RecurringTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Contas recorrentes", description = "Aluguel, streaming, salário: lançados sozinhos todo mês")
@RestController
@RequestMapping("/recurring")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;
    private final CurrentUserProvider currentUser;

    @Operation(summary = "Lista as contas recorrentes, com a próxima data de cada uma")
    @GetMapping
    public List<RecurringResponse> list() {
        return recurringService.list(currentUser.requireUserId());
    }

    @Operation(summary = "Cria uma conta recorrente (o lançamento do mês atual sai na hora, se o dia já passou)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "422", description = "Mês final antes do inicial", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringResponse create(@Valid @RequestBody RecurringRequest request) {
        return recurringService.create(currentUser.requireUserId(), request);
    }

    @Operation(summary = "Altera a conta, inclusive pausar e retomar (active)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta atualizada"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada", content = @Content)
    })
    @PutMapping("/{id}")
    public RecurringResponse update(@PathVariable UUID id, @Valid @RequestBody RecurringRequest request) {
        return recurringService.update(currentUser.requireUserId(), id, request);
    }

    @Operation(summary = "Remove a conta recorrente; os lançamentos já criados ficam no histórico")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta removida"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        recurringService.delete(currentUser.requireUserId(), id);
    }
}
