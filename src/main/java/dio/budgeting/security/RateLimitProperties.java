package dio.budgeting.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limite de requisicoes por minuto. "auth" vale para login, cadastro e demo (por IP: freia forca bruta);
 * "assistant" vale para as rotas da Lumi e o pareamento do WhatsApp (por usuario: cada chamada gasta
 * cota da IA). Desligue com app.rate-limit.enabled=false (os testes fazem isso).
 */
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(boolean enabled, int authPerMinute, int assistantPerMinute) {

    public RateLimitProperties {
        if (authPerMinute <= 0) {
            authPerMinute = 10;
        }
        if (assistantPerMinute <= 0) {
            assistantPerMinute = 20;
        }
    }
}
