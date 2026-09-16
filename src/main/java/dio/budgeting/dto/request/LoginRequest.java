package dio.budgeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "bruno@email.com")
        @NotBlank(message = "o e-mail é obrigatório")
        String email,

        @Schema(example = "senha-forte-123")
        @NotBlank(message = "a senha é obrigatória")
        String password
) {
}
