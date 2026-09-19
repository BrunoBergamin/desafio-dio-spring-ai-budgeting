package dio.budgeting.tool;

import dio.budgeting.dto.request.GoalRequest;
import dio.budgeting.dto.response.SavingsGoalResponse;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.service.SavingsGoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Metas de economia para a Lumi: criar, guardar dinheiro e conferir o progresso. */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoalTools {

    private final SavingsGoalService goalService;

    @Tool(name = "criar_meta",
            description = "Cria uma meta de economia: um valor para juntar, com prazo opcional. "
                    + "Use quando a pessoa disser que quer juntar dinheiro para alguma coisa")
    public SavingsGoalResponse createGoal(
            @ToolParam(description = "Nome curto da meta, ex.: 'Viagem' ou 'Reserva de emergência'") String name,
            @ToolParam(description = "Valor a juntar, em reais, ex.: 6000.00") BigDecimal targetAmount,
            @ToolParam(description = "Prazo no formato AAAA-MM-DD. Omita se não houver", required = false) String deadline,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] criar_meta user={} name='{}' target={} deadline={}", userId, name, targetAmount, deadline);
        return goalService.create(userId, new GoalRequest(name, targetAmount, parseDate(deadline), null));
    }

    @Tool(name = "guardar_na_meta",
            description = "Registra dinheiro guardado em uma meta, pelo nome dela. Guardar não é gasto: "
                    + "não entra no total de gastos nem consome orçamento")
    public SavingsGoalResponse saveIntoGoal(
            @ToolParam(description = "Nome da meta, como a pessoa falou") String name,
            @ToolParam(description = "Valor guardado agora, em reais") BigDecimal amount,
            ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] guardar_na_meta user={} name='{}' amount={}", userId, name, amount);
        return goalService.depositByName(userId, name, amount);
    }

    @Tool(name = "consultar_metas",
            description = "Lista as metas de economia com quanto já foi guardado, quanto falta e "
                    + "quanto por mês seria preciso para chegar no prazo")
    public List<SavingsGoalResponse> listGoals(ToolContext toolContext) {
        var userId = ToolUser.require(toolContext);
        log.info("[tool] consultar_metas user={}", userId);
        return goalService.list(userId);
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("data '%s' inválida, use o formato AAAA-MM-DD".formatted(value));
        }
    }
}
