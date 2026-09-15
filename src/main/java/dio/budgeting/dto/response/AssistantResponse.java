package dio.budgeting.dto.response;

/**
 * Resposta textual do assistente.
 *
 * @param transcription o que foi entendido do áudio (nulo quando a entrada já era texto)
 * @param answer        resposta final gerada pela IA
 */
public record AssistantResponse(String transcription, String answer) {
}
