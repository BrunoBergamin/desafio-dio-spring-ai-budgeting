package dio.budgeting.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente da Evolution API v2 (API compativel com o WhatsApp Web, nao oficial).
 * Endpoints confirmados no codigo-fonte da Evolution: /instance/create, /instance/connect/{i},
 * /instance/connectionState/{i}, /message/sendText/{i}, /message/sendWhatsAppAudio/{i},
 * /chat/getBase64FromMediaMessage/{i}. Autenticacao pelo header "apikey".
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.whatsapp", name = "enabled", havingValue = "true")
public class EvolutionApiGateway implements WhatsAppGateway {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };

    private final RestClient client;
    private final WhatsAppProperties properties;

    public EvolutionApiGateway(RestClient.Builder builder, WhatsAppProperties properties) {
        this.properties = properties;
        this.client = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("apikey", properties.apiKey())
                .build();
    }

    @Override
    public ConnectionInfo connect() {
        ensureInstance();
        var body = client.get().uri("/instance/connect/{instance}", properties.instance())
                .retrieve().body(MAP);
        return toConnectionInfo(body);
    }

    @Override
    public ConnectionInfo status() {
        try {
            var body = client.get().uri("/instance/connectionState/{instance}", properties.instance())
                    .retrieve().body(MAP);
            var instance = asMap(body == null ? null : body.get("instance"));
            return new ConnectionInfo(str(instance.get("state")), null, null);
        } catch (HttpClientErrorException.NotFound e) {
            return new ConnectionInfo("not_created", null, null);
        }
    }

    @Override
    public void sendText(String phone, String text) {
        client.post().uri("/message/sendText/{instance}", properties.instance())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("number", phone, "text", text))
                .retrieve().toBodilessEntity();
        log.info("[whatsapp] texto enviado para {}", mask(phone));
    }

    @Override
    public void sendAudio(String phone, byte[] mp3) {
        client.post().uri("/message/sendWhatsAppAudio/{instance}", properties.instance())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("number", phone, "audio", Base64.getEncoder().encodeToString(mp3)))
                .retrieve().toBodilessEntity();
        log.info("[whatsapp] áudio enviado para {}", mask(phone));
    }

    @Override
    public Optional<Media> downloadMedia(Map<String, Object> messageKey) {
        var body = client.post().uri("/chat/getBase64FromMediaMessage/{instance}", properties.instance())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", Map.of("key", messageKey), "convertToMp4", false))
                .retrieve().body(MAP);
        if (body == null || body.get("base64") == null) {
            return Optional.empty();
        }
        return Optional.of(new Media(Base64.getDecoder().decode(str(body.get("base64"))), str(body.get("mimetype"))));
    }

    /** Cria a instancia (uma vez) ja com o webhook apontando para esta aplicacao. */
    private void ensureInstance() {
        if (!"not_created".equals(status().state())) {
            return;
        }
        var webhook = Map.of(
                "enabled", true,
                "url", properties.webhookUrl(),
                "events", List.of("MESSAGES_UPSERT"),
                "base64", true,
                "byEvents", false);
        client.post().uri("/instance/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "instanceName", properties.instance(),
                        "integration", "WHATSAPP-BAILEYS",
                        "qrcode", true,
                        "webhook", webhook))
                .retrieve().toBodilessEntity();
        log.info("[whatsapp] instância '{}' criada com webhook em {}", properties.instance(), properties.webhookUrl());
    }

    private ConnectionInfo toConnectionInfo(Map<String, Object> body) {
        if (body == null) {
            return new ConnectionInfo("unknown", null, null);
        }
        // /instance/connect devolve o QR ({base64, code, pairingCode, count}) enquanto conecta,
        // e {instance:{state}} quando ja esta conectado
        if (body.containsKey("base64") || body.containsKey("code")) {
            return new ConnectionInfo("connecting", str(body.get("base64")), str(body.get("pairingCode")));
        }
        var instance = asMap(body.get("instance"));
        return new ConnectionInfo(str(instance.getOrDefault("state", "unknown")), null, null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    static String mask(String phone) {
        return phone == null || phone.length() < 4 ? "***" : "***" + phone.substring(phone.length() - 4);
    }
}
