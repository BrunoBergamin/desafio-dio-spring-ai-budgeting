package dio.budgeting.whatsapp;

import java.util.Map;
import java.util.Optional;

/**
 * O que a aplicacao precisa de um provedor de WhatsApp. Hoje e a Evolution API; trocar pela
 * API oficial da Meta significa escrever outra implementacao desta interface, e mais nada.
 */
public interface WhatsAppGateway {

    /** Estado da conexao (ex.: "open", "connecting", "close") e, quando ainda nao pareado, o QR code. */
    ConnectionInfo connect();

    ConnectionInfo status();

    /** Envia e devolve o id da mensagem (para reconhecer o proprio eco no webhook). Nulo se o provedor nao informar. */
    String sendText(String phone, String text);

    String sendAudio(String phone, byte[] mp3);

    /** Baixa a midia de uma mensagem recebida quando ela nao veio em base64 no webhook. */
    Optional<Media> downloadMedia(Map<String, Object> messageKey);

    record ConnectionInfo(String state, String qrCodeBase64, String pairingCode) {
        @com.fasterxml.jackson.annotation.JsonProperty("connected")
        public boolean connected() {
            return "open".equals(state);
        }
    }

    record Media(byte[] bytes, String mimetype) {
    }
}
