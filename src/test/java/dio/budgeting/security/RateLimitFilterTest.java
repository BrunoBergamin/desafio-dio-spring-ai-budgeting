package dio.budgeting.security;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    /** Relogio manual: o teste "espera um minuto" sem esperar de verdade. */
    static class FakeTicker implements Ticker {
        final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }

    final FakeTicker ticker = new FakeTicker();
    RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(new RateLimitProperties(true, 3, 2), new ProblemDetailResponses(new ObjectMapper()), ticker);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_return429WithProblemDetail_when_ipPassesTheLoginLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
        }

        var blocked = call("POST", "/api/auth/login", "10.0.0.1");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentType()).startsWith("application/problem+json");
        assertThat(blocked.getContentAsString()).contains("\"title\":\"Muitas requisições\"").contains("\"status\":429");
    }

    @Test
    void should_countEachIpSeparately_when_limitingLogin() throws Exception {
        for (int i = 0; i < 3; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }

        assertThat(call("POST", "/api/auth/login", "10.0.0.2").getStatus()).isEqualTo(200);
        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(429);
    }

    @Test
    void should_allowAgain_when_theMinuteWindowPasses() throws Exception {
        for (int i = 0; i < 4; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }
        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(429);

        ticker.advance(Duration.ofSeconds(61));

        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void should_countByAuthenticatedUser_when_callingTheAssistant() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user-a", null, List.of()));
        assertThat(call("POST", "/api/assistant/chat", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(call("POST", "/api/assistant/chat", "10.0.0.9").getStatus()).isEqualTo(200);
        assertThat(call("POST", "/api/assistant/chat", "10.0.0.1").getStatus()).isEqualTo(429);

        // Outro usuario, mesmo IP: contador proprio
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user-b", null, List.of()));
        assertThat(call("POST", "/api/assistant/chat", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void should_notLimit_when_routeIsNotSensitive() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertThat(call("GET", "/api/transactions", "10.0.0.1").getStatus()).isEqualTo(200);
        }
        // GET no login (nao existe) tambem nao conta: so o POST e limitado
        for (int i = 0; i < 20; i++) {
            assertThat(call("GET", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void should_useTheFirstForwardedIp_when_behindAProxy() {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("172.18.0.2");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 172.18.0.1");

        assertThat(RateLimitFilter.clientIp(request)).isEqualTo("203.0.113.7");
        assertThat(filter.bucketFor(request)).isEqualTo(new RateLimitFilter.Bucket("auth:203.0.113.7", 3));
    }

    private MockHttpServletResponse call(String method, String path, String ip) throws Exception {
        var request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        request.setRemoteAddr(ip);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
