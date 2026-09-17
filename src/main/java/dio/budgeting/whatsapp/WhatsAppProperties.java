package dio.budgeting.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Integracao com o WhatsApp via Evolution API (perfil "whatsapp").
 *
 * @param enabled       liga o modulo
 * @param baseUrl       URL da Evolution (dentro do compose: http://evolution:8080)
 * @param apiKey        AUTHENTICATION_API_KEY da Evolution (header "apikey")
 * @param instance      nome da instancia (uma por numero conectado)
 * @param webhookSecret segredo que protege o webhook, vai no caminho da URL
 * @param webhookUrl    URL que a Evolution chama (dentro do compose: http://app:8080/api/whatsapp/webhook/{segredo})
 * @param replyUnknown  responder a numeros nao vinculados com um convite. Padrao false: o WhatsApp pareado recebe
 *                      mensagens de TODO MUNDO, e responder a estranhos vira spam (so ligue com um chip dedicado a Lumi)
 */
@ConfigurationProperties("app.whatsapp")
public record WhatsAppProperties(boolean enabled,
                                 String baseUrl,
                                 String apiKey,
                                 String instance,
                                 String webhookSecret,
                                 String webhookUrl,
                                 boolean replyUnknown) {

    public WhatsAppProperties {
        if (enabled) {
            if (webhookSecret == null || webhookSecret.length() < 16) {
                throw new IllegalStateException("app.whatsapp.webhook-secret precisa ter ao menos 16 caracteres");
            }
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("app.whatsapp.api-key (EVOLUTION_API_KEY) é obrigatória");
            }
        }
        if (instance == null || instance.isBlank()) {
            instance = "lumi";
        }
    }
}
