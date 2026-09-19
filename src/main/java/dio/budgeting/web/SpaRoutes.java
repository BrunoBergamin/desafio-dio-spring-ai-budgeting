package dio.budgeting.web;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Unica lista das rotas do React. O {@link SpaForwardController} entrega o index.html nelas e o
 * SecurityConfig as libera sem token. Pagina nova no frontend: acrescente o nome em {@link #PAGES}.
 */
public final class SpaRoutes {

    /** Nomes das paginas, separados por "|" para servir de regex no {@code @GetMapping}. */
    public static final String PAGES = "login|cadastro|painel|conversa|transacoes|recorrentes|metas|orcamentos|whatsapp";

    /** Padrao de rota do Spring MVC: "/{page:login|cadastro|...}". Constante de compilacao, cabe em anotacao. */
    public static final String PATTERN = "/{page:" + PAGES + "}";

    /** Caminhos concretos, para o permitAll da seguranca. */
    public static final String[] PATHS = Stream.concat(
                    Stream.of("/", "/index.html"),
                    Arrays.stream(PAGES.split("\\|")).map(page -> "/" + page))
            .toArray(String[]::new);

    private SpaRoutes() {
    }
}
