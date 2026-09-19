package dio.budgeting.entity;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Categorias de gasto e de receita, no mesmo enum: assim existe um unico parametro "categoria" no REST,
 * nas ferramentas e no frontend, e cada categoria ja sabe se e entrada ou saida de dinheiro.
 * Os nomes ficam gravados como texto (VARCHAR(20)), entao adicionar uma categoria nova nao exige
 * migration: basta o enum, o rotulo e a cor no frontend.
 */
@Getter
public enum Category {
    // ---------- Gastos ----------
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
    OTHER("Outros"),

    // ---------- Receitas ----------
    SALARY("Salário", TransactionType.INCOME),
    FREELANCE("Freelance e bicos", TransactionType.INCOME),
    INVESTMENTS("Rendimentos e investimentos", TransactionType.INCOME),
    OTHER_INCOME("Outras receitas", TransactionType.INCOME);

    private final String label;
    private final TransactionType type;

    Category(String label) {
        this(label, TransactionType.EXPENSE);
    }

    Category(String label, TransactionType type) {
        this.label = label;
        this.type = type;
    }

    public static List<Category> of(TransactionType type) {
        return Arrays.stream(values()).filter(c -> c.type == type).toList();
    }

    /**
     * Guia curto para o modelo escolher a categoria certa (vai na descricao das ferramentas).
     * Precisa ser constante de compilacao: montado so com "+", nada de String.join nem stream.
     */
    public static final String GUIDE = "gastos: GROCERIES=mercado/supermercado/padaria, "
            + "RESTAURANT=restaurante/lanche/delivery, PHARMA=farmácia/remédio/médico/plano de saúde, "
            + "HOUSING=aluguel/luz/água/internet/condomínio, TRANSPORT=ônibus/metrô/uber/táxi, "
            + "AUTO=combustível/estacionamento/manutenção do carro, SUBSCRIPTIONS=streaming/academia/apps/planos mensais, "
            + "CLOTHING=roupas/calçados/acessórios, PERSONAL_CARE=cabeleireiro/cosméticos/barbearia, "
            + "LEISURE=cinema/bar/jogos/passeios, EDUCATION=cursos/livros/escola, PETS=ração/veterinário/pet shop, "
            + "TRAVEL=passagens/hotel/viagem, GIFTS=presentes/doações, TAXES=impostos/taxas/multas, "
            + "OTHER=quando nenhuma se encaixa. "
            + "Receitas (dinheiro que entra): SALARY=salário, FREELANCE=freela/bico/serviço prestado, "
            + "INVESTMENTS=rendimento/dividendo/juros, OTHER_INCOME=presente recebido/reembolso/venda/outras entradas";
}
