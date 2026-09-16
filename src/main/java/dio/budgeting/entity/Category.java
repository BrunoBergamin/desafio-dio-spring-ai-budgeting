package dio.budgeting.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Categorias de gasto. Os nomes ficam gravados como texto (VARCHAR(20)), entao adicionar uma
 * categoria nova nao exige migration: basta o enum, o rotulo e a cor no frontend.
 */
@Getter
@RequiredArgsConstructor
public enum Category {
    GROCERIES("Mercado"),
    RESTAURANT("Restaurantes e delivery"),
    PHARMA("Farmácia e saúde"),
    HOUSING("Moradia e contas de casa"),
    TRANSPORT("Transporte"),
    AUTO("Carro e combustível"),
    SUBSCRIPTIONS("Assinaturas e streaming"),
    CLOTHING("Roupas e acessórios"),
    PERSONAL_CARE("Beleza e cuidados pessoais"),
    LEISURE("Lazer"),
    EDUCATION("Educação"),
    PETS("Pets"),
    TRAVEL("Viagem"),
    GIFTS("Presentes e doações"),
    TAXES("Impostos e taxas"),
    OTHER("Outros");

    private final String label;

    /** Guia curto para o modelo escolher a categoria certa (vai na descricao das ferramentas). */
    public static final String GUIDE = "GROCERIES=mercado/supermercado/padaria, RESTAURANT=restaurante/lanche/delivery, "
            + "PHARMA=farmácia/remédio/médico/plano de saúde, HOUSING=aluguel/luz/água/internet/condomínio, "
            + "TRANSPORT=ônibus/metrô/uber/táxi, AUTO=combustível/estacionamento/manutenção do carro, "
            + "SUBSCRIPTIONS=streaming/academia/apps/planos mensais, CLOTHING=roupas/calçados/acessórios, "
            + "PERSONAL_CARE=cabeleireiro/cosméticos/barbearia, LEISURE=cinema/bar/jogos/passeios, "
            + "EDUCATION=cursos/livros/escola, PETS=ração/veterinário/pet shop, TRAVEL=passagens/hotel/viagem, "
            + "GIFTS=presentes/doações, TAXES=impostos/taxas/multas, OTHER=quando nenhuma se encaixa";
}
