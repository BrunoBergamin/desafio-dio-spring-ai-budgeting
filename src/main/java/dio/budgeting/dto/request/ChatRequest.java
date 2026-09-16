package dio.budgeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @Schema(example = "Gastei 80 reais no mercado hoje")
        @NotBlank(message = "a mensagem é obrigatória")
        @Size(max = 500, message = "a mensagem deve ter no máximo 500 caracteres")
        String message,

        @Schema(example = "principal", description = "Opcional. Identifica a conversa para a Lumi lembrar do contexto.")
        @Pattern(regexp = "[A-Za-z0-9_-]{1,40}", message = "o identificador de conversa deve ter até 40 letras, números, hífen ou underline")
        String conversationId
) {
}
