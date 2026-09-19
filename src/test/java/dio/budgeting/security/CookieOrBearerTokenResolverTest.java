package dio.budgeting.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CookieOrBearerTokenResolverTest {

    final CookieOrBearerTokenResolver resolver = new CookieOrBearerTokenResolver();

    @Test
    void should_readTheTokenFromTheCookie_when_thereIsNoHeader() {
        var request = new MockHttpServletRequest("GET", "/api/transactions");
        request.setCookies(new Cookie(AuthCookies.NAME, "token-do-cookie"));

        assertThat(resolver.resolve(request)).isEqualTo("token-do-cookie");
    }

    @Test
    void should_preferTheHeader_when_bothArePresent() {
        // Swagger aberto numa aba com outra conta nao pode ser atropelado pelo cookie do site na outra
        var request = new MockHttpServletRequest("GET", "/api/transactions");
        request.addHeader("Authorization", "Bearer token-do-header");
        request.setCookies(new Cookie(AuthCookies.NAME, "token-do-cookie"));

        assertThat(resolver.resolve(request)).isEqualTo("token-do-header");
    }

    @Test
    void should_returnNothing_when_thereIsNeitherHeaderNorCookie() {
        assertThat(resolver.resolve(new MockHttpServletRequest("GET", "/api/transactions"))).isNull();
    }

    @Test
    void should_ignoreAnEmptyCookie_so_theRequestIsTreatedAsAnonymous() {
        var request = new MockHttpServletRequest("GET", "/api/transactions");
        request.setCookies(new Cookie(AuthCookies.NAME, ""));

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void should_notBeSecure_onPlainHttp_butBeSecureBehindAnHttpsProxy() {
        var local = new MockHttpServletRequest("POST", "/api/auth/login");
        var behindProxy = new MockHttpServletRequest("POST", "/api/auth/login");
        behindProxy.addHeader("X-Forwarded-Proto", "https");

        // Secure no localhost em http faria o navegador descartar o cookie
        assertThat(AuthCookies.issue("t", 60, local).toString()).doesNotContain("Secure");
        assertThat(AuthCookies.issue("t", 60, behindProxy).toString()).contains("Secure");
    }

    @Test
    void should_buildACookieThatJavascriptCannotRead() {
        var cookie = AuthCookies.issue("token", 28800, new MockHttpServletRequest()).toString();

        assertThat(cookie)
                .contains("lumi_token=token")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api")
                .contains("Max-Age=28800");
    }

    @Test
    void should_clearTheCookie_when_loggingOut() {
        assertThat(AuthCookies.clear(new MockHttpServletRequest()).toString())
                .contains("lumi_token=")
                .contains("Max-Age=0");
    }
}
