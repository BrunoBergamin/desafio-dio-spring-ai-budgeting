package dio.budgeting.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    GROCERIES("Mercado"),
    PHARMA("Farmácia e saúde"),
    AUTO("Carro e combustível"),
    RESTAURANT("Restaurantes e delivery"),
    TRANSPORT("Transporte"),
    HOUSING("Moradia e contas"),
    LEISURE("Lazer"),
    EDUCATION("Educação"),
    OTHER("Outros");

    private final String label;
}
