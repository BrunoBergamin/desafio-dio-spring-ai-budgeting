package dio.budgeting.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailResponsesTest {

    final ObjectMapper objectMapper = new ObjectMapper();
    final ProblemDetailResponses responses = new ProblemDetailResponses(objectMapper);

    @Test
    void should_writeProblemDetail_when_requestIsNotAuthenticated() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/transactions");
        var response = new MockHttpServletResponse();

        responses.commence(request, response, new InsufficientAuthenticationException("sem token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        var body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("title").asString()).isEqualTo("Não autenticado");
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("instance").asString()).isEqualTo("/api/transactions");
    }

    @Test
    void should_keepTheJsonValid_when_theUriHasCharactersThatWouldBreakIt() throws Exception {
        // Montado na mao com String.formatted, um caminho com aspas quebraria o JSON; o Jackson escapa
        var request = new MockHttpServletRequest("GET", "/api/x");
        request.setRequestURI("/api/\"aspas\"/\\barra");
        var response = new MockHttpServletResponse();

        responses.handle(request, response, new org.springframework.security.access.AccessDeniedException("nao pode"));

        assertThat(response.getStatus()).isEqualTo(403);
        var body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("instance").asString()).isEqualTo("/api/\"aspas\"/\\barra");
        assertThat(body.get("title").asString()).isEqualTo("Acesso negado");
    }

    @Test
    void should_writeAnyStatus_when_calledByTheRateLimitFilter() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        var response = new MockHttpServletResponse();

        responses.write(response, request, HttpStatus.TOO_MANY_REQUESTS, "Muitas requisições", "limite de 10 por minuto");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(objectMapper.readTree(response.getContentAsString()).get("detail").asString())
                .isEqualTo("limite de 10 por minuto");
    }
}
