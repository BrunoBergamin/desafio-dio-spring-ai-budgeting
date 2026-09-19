package dio.budgeting.controller;

import dio.budgeting.dto.request.DepositRequest;
import dio.budgeting.dto.request.GoalRequest;
import dio.budgeting.dto.response.SavingsGoalResponse;
import dio.budgeting.security.CurrentUserProvider;
import dio.budgeting.service.SavingsGoalService;
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

@Tag(name = "Metas de economia", description = "Guardar dinheiro com alvo e prazo")
@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class SavingsGoalController {

    private final SavingsGoalService goalService;
    private final CurrentUserProvider currentUser;

    @Operation(summary = "Lista as metas com o progresso de cada uma")
    @GetMapping
    public List<SavingsGoalResponse> list() {
        return goalService.list(currentUser.requireUserId());
    }

    @Operation(summary = "Cria uma meta de economia")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Meta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "422", description = "Nome repetido ou prazo no passado", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsGoalResponse create(@Valid @RequestBody GoalRequest request) {
        return goalService.create(currentUser.requireUserId(), request);
    }

    @Operation(summary = "Guarda dinheiro na meta (não é um gasto, não entra no resumo)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meta atualizada"),
            @ApiResponse(responseCode = "400", description = "Valor inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Meta não encontrada", content = @Content)
    })
    @PostMapping("/{id}/deposits")
    public SavingsGoalResponse deposit(@PathVariable UUID id, @Valid @RequestBody DepositRequest request) {
        return goalService.deposit(currentUser.requireUserId(), id, request.amount());
    }

    @Operation(summary = "Altera nome, alvo, prazo ou o valor já guardado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meta atualizada"),
            @ApiResponse(responseCode = "404", description = "Meta não encontrada", content = @Content)
    })
    @PutMapping("/{id}")
    public SavingsGoalResponse update(@PathVariable UUID id, @Valid @RequestBody GoalRequest request) {
        return goalService.update(currentUser.requireUserId(), id, request);
    }

    @Operation(summary = "Remove a meta")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Meta removida"),
            @ApiResponse(responseCode = "404", description = "Meta não encontrada", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        goalService.delete(currentUser.requireUserId(), id);
    }
}
