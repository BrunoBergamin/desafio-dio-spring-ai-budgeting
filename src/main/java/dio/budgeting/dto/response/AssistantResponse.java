package dio.budgeting.dto.response;

/**
 * Resposta textual da Lumi.
 *
 * @param transcription  o que foi entendido do áudio (nulo quando a entrada já era texto)
 * @param answer         resposta final gerada pela IA
 * @param conversationId identificador da conversa usado na memória (reenvie para manter o contexto)
 */
public record AssistantResponse(String transcription, String answer, String conversationId) {
}
