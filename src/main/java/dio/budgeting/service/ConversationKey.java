package dio.budgeting.service;

import dio.budgeting.exception.BusinessException;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Chave da memoria de conversa. Sempre comeca pelo id do usuario autenticado, entao um usuario
 * nunca consegue ler o historico de outro, mesmo mandando o mesmo conversationId.
 */
public final class ConversationKey {

    public static final String DEFAULT = "default";
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,40}");

    private ConversationKey() {
    }

    public static String of(UUID userId, String clientConversationId) {
        return userId + ":" + slug(clientConversationId);
    }

    /** Normaliza o id vindo do cliente (o que volta na resposta, sem o userId). */
    public static String slug(String clientConversationId) {
        if (clientConversationId == null || clientConversationId.isBlank()) {
            return DEFAULT;
        }
        var trimmed = clientConversationId.trim();
        if (!VALID.matcher(trimmed).matches()) {
            throw new BusinessException(
                    "identificador de conversa inválido: use até 40 letras, números, hífen ou underline");
        }
        return trimmed;
    }
}
