package dio.budgeting.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/**
 * 401 e 403 acontecem na cadeia de filtros, antes do DispatcherServlet, entao o
 * GlobalExceptionHandler nao os alcanca. Estes handlers escrevem o mesmo formato ProblemDetail
 * (RFC 9457), serializado pelo Jackson para nenhum caractere da URL quebrar o JSON.
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailResponses implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(response, request, HttpStatus.UNAUTHORIZED, "Não autenticado",
                "entre na sua conta; a API aceita o token no header Authorization: Bearer <token> ou no cookie de sessão");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(response, request, HttpStatus.FORBIDDEN, "Acesso negado",
                "você não tem permissão para acessar este recurso");
    }

    /** Escreve um ProblemDetail direto na resposta; tambem usado pelo filtro de limite de requisicoes. */
    public void write(HttpServletResponse response, HttpServletRequest request,
                      HttpStatus status, String title, String detail) throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("type", "about:blank");
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
