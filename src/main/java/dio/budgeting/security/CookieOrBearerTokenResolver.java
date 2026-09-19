package dio.budgeting.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/**
 * Onde procurar o token: primeiro no header {@code Authorization}, depois no cookie.
 * <p>
 * O site usa o cookie {@code HttpOnly} (fora do alcance do JavaScript, protegido contra XSS), enquanto
 * Swagger, {@code requests.http} e curl continuam mandando o Bearer no header. O header vem primeiro
 * para um teste no Swagger com outra conta nao ser atropelado pelo cookie do site aberto na outra aba.
 */
@Component
public class CookieOrBearerTokenResolver implements BearerTokenResolver {

    private final BearerTokenResolver header = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        var fromHeader = header.resolve(request);
        if (fromHeader != null) {
            return fromHeader;
        }
        var cookie = WebUtils.getCookie(request, AuthCookies.NAME);
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }
}
