package dio.budgeting.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Janela fixa de um minuto por chave, em memoria (Caffeine expira a entrada sozinho). Roda depois da
 * autenticacao do JWT, entao consegue contar por usuario nas rotas da Lumi; nas rotas de login, que nao
 * tem token, conta por IP. Acima do limite responde 429 em ProblemDetail com Retry-After.
 * <p>
 * Nao e um "balde" distribuido de proposito: uma instancia so, app pessoal. Se um dia houver varias
 * instancias, a chave muda para o Redis e o resto fica igual.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Set<String> AUTH_PATHS = Set.of("/api/auth/login", "/api/auth/register", "/api/auth/demo");

    private final RateLimitProperties properties;
    private final ProblemDetailResponses problemResponses;
    private final Cache<String, AtomicInteger> hits;

    public RateLimitFilter(RateLimitProperties properties, ProblemDetailResponses problemResponses) {
        this(properties, problemResponses, Ticker.systemTicker());
    }

    /** O Ticker e injetavel para o teste avancar o relogio sem esperar um minuto. */
    RateLimitFilter(RateLimitProperties properties, ProblemDetailResponses problemResponses, Ticker ticker) {
        this.properties = properties;
        this.problemResponses = problemResponses;
        this.hits = Caffeine.newBuilder()
                .expireAfterWrite(WINDOW)
                .maximumSize(50_000)
                .ticker(ticker)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var bucket = bucketFor(request);
        if (bucket == null) {
            chain.doFilter(request, response);
            return;
        }
        var count = hits.get(bucket.key(), key -> new AtomicInteger()).incrementAndGet();
        if (count > bucket.limit()) {
            log.warn("[rate-limit] {} passou de {} requisições por minuto em {}", bucket.key(), bucket.limit(), request.getRequestURI());
            response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            problemResponses.write(response, request, HttpStatus.TOO_MANY_REQUESTS, "Muitas requisições",
                    "limite de %d por minuto; tente de novo em instantes".formatted(bucket.limit()));
            return;
        }
        chain.doFilter(request, response);
    }

    /** Qual limite vale para a requisicao; nulo quando a rota nao e limitada. */
    Bucket bucketFor(HttpServletRequest request) {
        var path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod()) && AUTH_PATHS.contains(path)) {
            return new Bucket("auth:" + clientIp(request), properties.authPerMinute());
        }
        if (path.startsWith("/api/assistant/") || path.equals("/api/whatsapp/connect")) {
            return new Bucket("assistant:" + principalOrIp(request), properties.assistantPerMinute());
        }
        return null;
    }

    private static String principalOrIp(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        }
        return clientIp(request);
    }

    /** Atras de um proxy (deploy) o IP real vem no X-Forwarded-For; sem proxy e o remoto mesmo. */
    static String clientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    record Bucket(String key, int limit) {
    }
}
