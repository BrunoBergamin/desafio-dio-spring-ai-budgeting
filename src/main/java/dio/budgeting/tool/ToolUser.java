package dio.budgeting.tool;

import org.springframework.ai.chat.model.ToolContext;

import java.util.UUID;

/**
 * O usuario autenticado chega as ferramentas pelo {@link ToolContext}, que o Spring AI injeta
 * na chamada e NAO expoe no schema enviado ao modelo. Assim a IA nunca escolhe de quem sao os dados.
 */
public final class ToolUser {

    public static final String USER_ID = "userId";

    private ToolUser() {
    }

    public static UUID require(ToolContext context) {
        var value = context == null ? null : context.getContext().get(USER_ID);
        if (value instanceof UUID id) {
            return id;
        }
        throw new IllegalStateException("ferramenta executada sem usuário autenticado no contexto");
    }
}
