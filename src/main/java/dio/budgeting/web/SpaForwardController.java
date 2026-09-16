package dio.budgeting.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entrega o index.html do React nas rotas da SPA, para o F5 em /transacoes nao dar 404.
 * Fica fora do pacote controller de proposito: nao deve receber o prefixo /api.
 * Lista explicita (e nao regex) para nao engolir 404 de API nem de asset.
 */
@Controller
class SpaForwardController {

    @GetMapping({"/", "/login", "/cadastro", "/conversa", "/transacoes", "/orcamentos"})
    String forward() {
        return "forward:/index.html";
    }
}
