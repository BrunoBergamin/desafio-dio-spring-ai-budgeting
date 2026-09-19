package dio.budgeting.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entrega o index.html do React nas rotas da SPA, para o F5 em /transacoes nao dar 404.
 * Fica fora do pacote controller de proposito: nao deve receber o prefixo /api.
 * A lista de paginas mora em {@link SpaRoutes}, junto com a liberacao na seguranca.
 */
@Controller
class SpaForwardController {

    @GetMapping({"/", SpaRoutes.PATTERN})
    String forward() {
        return "forward:/index.html";
    }
}
