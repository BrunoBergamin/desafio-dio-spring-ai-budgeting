package dio.budgeting.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Cookie do token. Utilitario estatico de proposito: virar bean obrigaria a acrescenta-lo no
 * {@code @Import} de todos os testes de controller.
 * <p>
 * {@code HttpOnly} tira o token do alcance do JavaScript, entao um XSS nao consegue le-lo.
 * {@code SameSite=Strict} faz o navegador nao mandar o cookie em nada que venha de outro site,
 * inclusive formulario HTML, que e o vetor de CSRF que o CORS nao cobre.
 */
public final class AuthCookies {

    public static final String NAME = "lumi_token";
    private static final String PATH = "/api";

    private AuthCookies() {
    }

    public static ResponseCookie issue(String token, long maxAgeSeconds, HttpServletRequest request) {
        return base(request)
                .value(token)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    /** Mesmo cookie com validade zero: e assim que o navegador apaga o que ja tem. */
    public static ResponseCookie clear(HttpServletRequest request) {
        return base(request).value("").maxAge(Duration.ZERO).build();
    }

    private static ResponseCookie.ResponseCookieBuilder base(HttpServletRequest request) {
        return ResponseCookie.from(NAME)
                .httpOnly(true)
                .sameSite("Strict")
                .path(PATH)
                .secure(isHttps(request));
    }

    /** Em http local o cookie nao pode ser Secure, senao o navegador descarta; atras de proxy vale o header. */
    private static boolean isHttps(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }
}
