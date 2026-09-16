package dio.budgeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "Bruno")
        @NotBlank(message = "o nome é obrigatório")
        @Size(max = 80, message = "o nome deve ter no máximo 80 caracteres")
        String name,

        @Schema(example = "bruno@email.com")
        @NotBlank(message = "o e-mail é obrigatório")
        @Email(message = "o e-mail é inválido")
        @Size(max = 180, message = "o e-mail deve ter no máximo 180 caracteres")
        String email,

        @Schema(example = "senha-forte-123")
        @NotBlank(message = "a senha é obrigatória")
        @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres")
        String password
) {
}
